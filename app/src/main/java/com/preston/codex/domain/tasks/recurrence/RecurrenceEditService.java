package com.preston.codex.domain.tasks.recurrence;

import com.preston.codex.domain.TransactionRunner;
import com.preston.codex.domain.tasks.model.OccurrenceStatus;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.RescheduleRecord;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.model.TaskStatus;
import com.preston.codex.domain.tasks.repository.TaskRepository;
import com.preston.codex.domain.tasks.service.RecurrenceRuleParams;
import com.preston.codex.domain.time.LocalDateProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RecurrenceEditService {

    private final TaskRepository repository;
    private final RecurrenceService recurrenceService;
    private final LocalDateProvider dateProvider;
    private final TransactionRunner transactionRunner;

    public RecurrenceEditService(
            TaskRepository repository,
            RecurrenceService recurrenceService,
            LocalDateProvider dateProvider,
            TransactionRunner transactionRunner
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.recurrenceService = Objects.requireNonNull(recurrenceService, "recurrenceService must not be null");
        this.dateProvider = Objects.requireNonNull(dateProvider, "dateProvider must not be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
    }

    public void editThisOccurrence(
            long occurrenceId,
            String titleOverride,
            LocalTime timeOverride,
            String noteOverride,
            String reflection
    ) {
        transactionRunner.runInTransaction(() -> {
            TaskOccurrence occ = repository.findOccurrenceById(occurrenceId);
            if (occ == null) {
                throw new IllegalArgumentException("Occurrence not found: " + occurrenceId);
            }
            Instant now = dateProvider.getInstant();
            TaskOccurrence updated = new TaskOccurrence(
                    occ.id(),
                    occ.taskId(),
                    occ.ruleId(),
                    occ.occurrenceDate(),
                    occ.effectiveDate(),
                    occ.status(),
                    titleOverride,
                    timeOverride,
                    noteOverride,
                    reflection,
                    occ.autoRescheduleCount(),
                    occ.completedAt(),
                    occ.cancelledAt(),
                    occ.createdAt(),
                    now
            );
            repository.updateOccurrence(updated);
            repository.insertTaskEvent(new TaskEvent(0L, occ.taskId(), occ.id(), "EDITED", "This occurrence edited", now));
        });
    }

    public Task editThisAndFuture(
            long occurrenceId,
            RecurrenceRuleParams newRuleParams
    ) {
        Objects.requireNonNull(newRuleParams, "newRuleParams must not be null");
        return transactionRunner.runInTransaction(() -> {
            TaskOccurrence occ = repository.findOccurrenceById(occurrenceId);
            if (occ == null) {
                throw new IllegalArgumentException("Occurrence not found: " + occurrenceId);
            }
            Task oldTask = repository.findTaskById(occ.taskId());
            if (oldTask == null) {
                throw new IllegalArgumentException("Task not found: " + occ.taskId());
            }
            RecurrenceRule oldRule = repository.findRuleById(occ.ruleId());
            if (oldRule == null) {
                throw new IllegalArgumentException("Rule not found: " + occ.ruleId());
            }

            Instant now = dateProvider.getInstant();
            LocalDate today = dateProvider.getToday();
            LocalDate splitDate = occ.occurrenceDate();

            // 1. Create new task row with previous_task_id = oldTask.id()
            // Copies knowledge_entry_id, tags, is_flexible. Does NOT copy reflection text!
            Task newTask = new Task(
                    0L,
                    TaskKind.SERIES,
                    oldTask.title(),
                    oldTask.description(),
                    TaskStatus.OPEN,
                    oldTask.id(),
                    oldTask.knowledgeEntryId(),
                    null, // Do NOT copy reflection
                    oldTask.isFlexible(),
                    oldTask.priority(),
                    null,
                    null,
                    null,
                    null,
                    0,
                    null,
                    null,
                    now,
                    now,
                    oldTask.tags()
            );
            long newTaskId = repository.insertTask(newTask);

            // 2. End the old rule the day before splitDate
            LocalDate oldRuleEndDate = splitDate.minusDays(1);
            RecurrenceRule truncatedOldRule = new RecurrenceRule(
                    oldRule.id(),
                    oldRule.taskId(),
                    oldRule.type(),
                    oldRule.interval(),
                    oldRule.weekdays(),
                    oldRule.startDate(),
                    oldRuleEndDate,
                    oldRule.occurrenceCount(),
                    oldRule.anchorDayOfMonth(),
                    oldRule.missedPolicy(),
                    oldRule.createdAt(),
                    now
            );
            repository.updateRecurrenceRule(truncatedOldRule);

            // 3. Delete untouched future occurrences of old rule and collect retained dates
            List<TaskOccurrence> oldOccurrences = repository.findOccurrencesByRuleId(oldRule.id());
            Set<LocalDate> retainedDates = new HashSet<>();
            int keptOccurrencesCount = 0;

            for (TaskOccurrence existingOcc : oldOccurrences) {
                if (existingOcc.occurrenceDate().isBefore(splitDate)) {
                    keptOccurrencesCount++;
                } else {
                    List<TaskEvent> events = repository.findTaskEventsByOccurrenceId(existingOcc.id());
                    List<RescheduleRecord> history = repository.findRescheduleRecordsByOccurrenceId(existingOcc.id());
                    boolean isUntouched = (existingOcc.status() == OccurrenceStatus.OPEN) && events.isEmpty() && history.isEmpty();

                    if (!isUntouched) {
                        retainedDates.add(existingOcc.occurrenceDate());
                        keptOccurrencesCount++;
                    }
                }
            }
            repository.deleteUntouchedFutureOccurrences(oldRule.id(), splitDate);

            // 4. Calculate adjusted occurrence_count for new rule if original rule was count-limited
            Integer adjustedCount = newRuleParams.occurrenceCount();
            if (adjustedCount == null && oldRule.occurrenceCount() != null) {
                adjustedCount = Math.max(0, oldRule.occurrenceCount() - keptOccurrencesCount);
            }

            // 5. Create new recurrence rule
            RecurrenceRule newRule = new RecurrenceRule(
                    0L,
                    newTaskId,
                    newRuleParams.type(),
                    newRuleParams.interval(),
                    newRuleParams.weekdays(),
                    splitDate,
                    newRuleParams.endDate(),
                    adjustedCount,
                    newRuleParams.anchorDayOfMonth(),
                    newRuleParams.missedPolicy() != null ? newRuleParams.missedPolicy() : oldRule.missedPolicy(),
                    now,
                    now
            );
            long newRuleId = repository.insertRecurrenceRule(newRule);
            RecurrenceRule savedNewRule = repository.findRuleById(newRuleId);

            // 6. Materialize new series skipping dates retained by old series
            recurrenceService.materializeRule(savedNewRule, today, retainedDates, now);

            repository.insertTaskEvent(new TaskEvent(0L, oldTask.id(), occ.id(), "SPLIT", "Series split to new task " + newTaskId, now));
            repository.insertTaskEvent(new TaskEvent(0L, newTaskId, null, "CREATED", "Created from series split of task " + oldTask.id(), now));

            return repository.findTaskById(newTaskId);
        });
    }
}
