package com.preston.codex.domain.tasks.service;

import com.preston.codex.domain.TransactionRunner;
import com.preston.codex.domain.tasks.model.MissedPolicy;
import com.preston.codex.domain.tasks.model.OccurrenceStatus;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.model.TaskStatus;
import com.preston.codex.domain.tasks.recurrence.RecurrenceService;
import com.preston.codex.domain.tasks.repository.TaskRepository;
import com.preston.codex.domain.time.LocalDateProvider;
import com.preston.codex.integration.KeyValueStore;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class TaskService {

    public static final String PREF_DEFAULT_MISSED_POLICY = "pref_default_missed_policy";

    private final TaskRepository repository;
    private final RecurrenceService recurrenceService;
    private final LocalDateProvider dateProvider;
    private final KeyValueStore keyValueStore;
    private final TransactionRunner transactionRunner;

    public TaskService(
            TaskRepository repository,
            RecurrenceService recurrenceService,
            LocalDateProvider dateProvider,
            KeyValueStore keyValueStore,
            TransactionRunner transactionRunner
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.recurrenceService = Objects.requireNonNull(recurrenceService, "recurrenceService must not be null");
        this.dateProvider = Objects.requireNonNull(dateProvider, "dateProvider must not be null");
        this.keyValueStore = Objects.requireNonNull(keyValueStore, "keyValueStore must not be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
    }

    public Task createOneOffTask(CreateOneOffTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        return transactionRunner.runInTransaction(() -> {
            Instant now = dateProvider.getInstant();
            Set<String> lowerTags = normalizeTags(command.tags());

            Task task = new Task(
                    0L,
                    TaskKind.ONE_OFF,
                    command.title(),
                    command.description(),
                    TaskStatus.OPEN,
                    null,
                    command.knowledgeEntryId(),
                    command.reflection(),
                    command.isFlexible(),
                    command.priority(),
                    command.deadline(),
                    command.estimatedDurationMinutes(),
                    command.scheduledDate(),
                    command.scheduledTime(),
                    0,
                    null,
                    null,
                    now,
                    now,
                    lowerTags
            );

            long id = repository.insertTask(task);
            Task created = repository.findTaskById(id);

            recordEvent(id, null, "CREATED", "One-off task created", now);
            return created;
        });
    }

    public Task createSeriesTask(CreateSeriesTaskCommand command, RecurrenceRuleParams ruleParams) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(ruleParams, "ruleParams must not be null");
        return transactionRunner.runInTransaction(() -> {
            Instant now = dateProvider.getInstant();
            LocalDate today = dateProvider.getToday();
            Set<String> lowerTags = normalizeTags(command.tags());

            Task task = new Task(
                    0L,
                    TaskKind.SERIES,
                    command.title(),
                    command.description(),
                    TaskStatus.OPEN,
                    null,
                    command.knowledgeEntryId(),
                    command.reflection(),
                    command.isFlexible(),
                    command.priority(),
                    null, // Occurrences have no deadline of their own
                    null,
                    null,
                    null,
                    0,
                    null,
                    null,
                    now,
                    now,
                    lowerTags
            );

            long taskId = repository.insertTask(task);

            // Default missed policy copied into rule at creation (Amendment 9)
            MissedPolicy missedPolicy = ruleParams.missedPolicy();
            if (missedPolicy == null) {
                String prefVal = keyValueStore.getString(PREF_DEFAULT_MISSED_POLICY, "KEEP_OVERDUE");
                try {
                    missedPolicy = MissedPolicy.valueOf(prefVal);
                } catch (Exception e) {
                    missedPolicy = MissedPolicy.KEEP_OVERDUE;
                }
            }

            RecurrenceRule rule = new RecurrenceRule(
                    0L,
                    taskId,
                    ruleParams.type(),
                    ruleParams.interval(),
                    ruleParams.weekdays(),
                    ruleParams.startDate(),
                    ruleParams.endDate(),
                    ruleParams.occurrenceCount(),
                    ruleParams.anchorDayOfMonth(),
                    missedPolicy,
                    now,
                    now
            );

            long ruleId = repository.insertRecurrenceRule(rule);
            RecurrenceRule savedRule = repository.findRuleById(ruleId);

            recordEvent(taskId, null, "CREATED", "Series created with rule " + ruleId, now);

            // Initial materialization
            recurrenceService.materializeRule(savedRule, today, Collections.emptySet(), now);

            return repository.findTaskById(taskId);
        });
    }

    public void completeTask(long taskId) {
        transactionRunner.runInTransaction(() -> {
            Task task = repository.findTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("Task not found: " + taskId);
            }
            if (task.taskKind() == TaskKind.SERIES) {
                throw new IllegalStateException("A SERIES row stays OPEN until cancelled and cannot be completed.");
            }
            Instant now = dateProvider.getInstant();
            Task updated = new Task(
                    task.id(),
                    task.taskKind(),
                    task.title(),
                    task.description(),
                    TaskStatus.COMPLETED,
                    task.previousTaskId(),
                    task.knowledgeEntryId(),
                    task.reflection(),
                    task.isFlexible(),
                    task.priority(),
                    task.deadline(),
                    task.estimatedDurationMinutes(),
                    task.scheduledDate(),
                    task.scheduledTime(),
                    task.autoRescheduleCount(),
                    now,
                    null,
                    task.createdAt(),
                    now,
                    task.tags()
            );
            repository.updateTask(updated);
            recordEvent(taskId, null, "COMPLETED", "Task completed", now);
        });
    }

    public void completeOccurrence(long occurrenceId) {
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
                    OccurrenceStatus.COMPLETED,
                    occ.titleOverride(),
                    occ.timeOverride(),
                    occ.noteOverride(),
                    occ.reflection(),
                    occ.autoRescheduleCount(),
                    now,
                    null,
                    occ.createdAt(),
                    now
            );
            repository.updateOccurrence(updated);
            recordEvent(occ.taskId(), occ.id(), "COMPLETED", "Occurrence completed", now);
        });
    }

    public void reopenTask(long taskId) {
        transactionRunner.runInTransaction(() -> {
            Task task = repository.findTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("Task not found: " + taskId);
            }
            Instant now = dateProvider.getInstant();
            Task updated = new Task(
                    task.id(),
                    task.taskKind(),
                    task.title(),
                    task.description(),
                    TaskStatus.OPEN,
                    task.previousTaskId(),
                    task.knowledgeEntryId(),
                    task.reflection(),
                    task.isFlexible(),
                    task.priority(),
                    task.deadline(),
                    task.estimatedDurationMinutes(),
                    task.scheduledDate(),
                    task.scheduledTime(),
                    task.autoRescheduleCount(),
                    null,
                    null,
                    task.createdAt(),
                    now,
                    task.tags()
            );
            repository.updateTask(updated);
            recordEvent(taskId, null, "REOPENED", "Task reopened to OPEN", now);
        });
    }

    public void reopenOccurrence(long occurrenceId) {
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
                    OccurrenceStatus.OPEN,
                    occ.titleOverride(),
                    occ.timeOverride(),
                    occ.noteOverride(),
                    occ.reflection(),
                    occ.autoRescheduleCount(),
                    null,
                    null,
                    occ.createdAt(),
                    now
            );
            repository.updateOccurrence(updated);
            recordEvent(occ.taskId(), occ.id(), "REOPENED", "Occurrence reopened to OPEN", now);
        });
    }

    public void cancelTask(long taskId) {
        transactionRunner.runInTransaction(() -> {
            Task task = repository.findTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("Task not found: " + taskId);
            }
            Instant now = dateProvider.getInstant();
            Task updated = new Task(
                    task.id(),
                    task.taskKind(),
                    task.title(),
                    task.description(),
                    TaskStatus.CANCELLED,
                    task.previousTaskId(),
                    task.knowledgeEntryId(),
                    task.reflection(),
                    task.isFlexible(),
                    task.priority(),
                    task.deadline(),
                    task.estimatedDurationMinutes(),
                    task.scheduledDate(),
                    task.scheduledTime(),
                    task.autoRescheduleCount(),
                    null,
                    now,
                    task.createdAt(),
                    now,
                    task.tags()
            );
            repository.updateTask(updated);
            recordEvent(taskId, null, "CANCELLED", "Task cancelled", now);
        });
    }

    public void cancelOccurrence(long occurrenceId) {
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
                    OccurrenceStatus.CANCELLED,
                    occ.titleOverride(),
                    occ.timeOverride(),
                    occ.noteOverride(),
                    occ.reflection(),
                    occ.autoRescheduleCount(),
                    null,
                    now,
                    occ.createdAt(),
                    now
            );
            repository.updateOccurrence(updated);
            recordEvent(occ.taskId(), occ.id(), "CANCELLED", "Occurrence cancelled", now);
        });
    }

    public void markTaskInProgress(long taskId) {
        transactionRunner.runInTransaction(() -> {
            Task task = repository.findTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("Task not found: " + taskId);
            }
            Instant now = dateProvider.getInstant();
            Task updated = new Task(
                    task.id(),
                    task.taskKind(),
                    task.title(),
                    task.description(),
                    TaskStatus.IN_PROGRESS,
                    task.previousTaskId(),
                    task.knowledgeEntryId(),
                    task.reflection(),
                    task.isFlexible(),
                    task.priority(),
                    task.deadline(),
                    task.estimatedDurationMinutes(),
                    task.scheduledDate(),
                    task.scheduledTime(),
                    task.autoRescheduleCount(),
                    null,
                    null,
                    task.createdAt(),
                    now,
                    task.tags()
            );
            repository.updateTask(updated);
            recordEvent(taskId, null, "IN_PROGRESS", "Task marked IN_PROGRESS", now);
        });
    }

    public void markOccurrenceInProgress(long occurrenceId) {
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
                    OccurrenceStatus.IN_PROGRESS,
                    occ.titleOverride(),
                    occ.timeOverride(),
                    occ.noteOverride(),
                    occ.reflection(),
                    occ.autoRescheduleCount(),
                    null,
                    null,
                    occ.createdAt(),
                    now
            );
            repository.updateOccurrence(updated);
            recordEvent(occ.taskId(), occ.id(), "IN_PROGRESS", "Occurrence marked IN_PROGRESS", now);
        });
    }

    public void setTaskReflection(long taskId, String reflection) {
        transactionRunner.runInTransaction(() -> {
            Task task = repository.findTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("Task not found: " + taskId);
            }
            Instant now = dateProvider.getInstant();
            Task updated = new Task(
                    task.id(),
                    task.taskKind(),
                    task.title(),
                    task.description(),
                    task.status(),
                    task.previousTaskId(),
                    task.knowledgeEntryId(),
                    reflection,
                    task.isFlexible(),
                    task.priority(),
                    task.deadline(),
                    task.estimatedDurationMinutes(),
                    task.scheduledDate(),
                    task.scheduledTime(),
                    task.autoRescheduleCount(),
                    task.completedAt(),
                    task.cancelledAt(),
                    task.createdAt(),
                    now,
                    task.tags()
            );
            repository.updateTask(updated);
        });
    }

    public void setOccurrenceReflection(long occurrenceId, String reflection) {
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
                    occ.titleOverride(),
                    occ.timeOverride(),
                    occ.noteOverride(),
                    reflection,
                    occ.autoRescheduleCount(),
                    occ.completedAt(),
                    occ.cancelledAt(),
                    occ.createdAt(),
                    now
            );
            repository.updateOccurrence(updated);
        });
    }

    public void linkKnowledge(long taskId, Long knowledgeEntryId) {
        transactionRunner.runInTransaction(() -> {
            Task task = repository.findTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("Task not found: " + taskId);
            }
            Instant now = dateProvider.getInstant();
            Task updated = new Task(
                    task.id(),
                    task.taskKind(),
                    task.title(),
                    task.description(),
                    task.status(),
                    task.previousTaskId(),
                    knowledgeEntryId,
                    task.reflection(),
                    task.isFlexible(),
                    task.priority(),
                    task.deadline(),
                    task.estimatedDurationMinutes(),
                    task.scheduledDate(),
                    task.scheduledTime(),
                    task.autoRescheduleCount(),
                    task.completedAt(),
                    task.cancelledAt(),
                    task.createdAt(),
                    now,
                    task.tags()
            );
            repository.updateTask(updated);
        });
    }

    public void unlinkKnowledge(long taskId) {
        linkKnowledge(taskId, null);
    }

    public void setTags(long taskId, Set<String> tags) {
        transactionRunner.runInTransaction(() -> {
            Set<String> lowerTags = normalizeTags(tags);
            repository.setTaskTags(taskId, lowerTags);
        });
    }

    public void deleteTask(long taskId) {
        transactionRunner.runInTransaction(() -> {
            repository.deleteTask(taskId);
        });
    }

    public void cancelSeries(long seriesTaskId) {
        // Amendment 7: cancelSeries: series row status becomes CANCELLED; rule end_date = yesterday;
        // cancel OPEN occurrences with effective_date >= today that have no events or history; keep everything else.
        transactionRunner.runInTransaction(() -> {
            Task task = repository.findTaskById(seriesTaskId);
            if (task == null) {
                throw new IllegalArgumentException("Task not found: " + seriesTaskId);
            }
            if (task.taskKind() != TaskKind.SERIES) {
                throw new IllegalArgumentException("Task " + seriesTaskId + " is not a SERIES");
            }
            Instant now = dateProvider.getInstant();
            LocalDate today = dateProvider.getToday();
            LocalDate yesterday = today.minusDays(1);

            // Update series task row to CANCELLED
            Task updated = new Task(
                    task.id(),
                    task.taskKind(),
                    task.title(),
                    task.description(),
                    TaskStatus.CANCELLED,
                    task.previousTaskId(),
                    task.knowledgeEntryId(),
                    task.reflection(),
                    task.isFlexible(),
                    task.priority(),
                    task.deadline(),
                    task.estimatedDurationMinutes(),
                    task.scheduledDate(),
                    task.scheduledTime(),
                    task.autoRescheduleCount(),
                    task.completedAt(),
                    now,
                    task.createdAt(),
                    now,
                    task.tags()
            );
            repository.updateTask(updated);

            // Update rule end_date to yesterday
            RecurrenceRule rule = repository.findRuleByTaskId(seriesTaskId);
            if (rule != null) {
                RecurrenceRule updatedRule = new RecurrenceRule(
                        rule.id(),
                        rule.taskId(),
                        rule.type(),
                        rule.interval(),
                        rule.weekdays(),
                        rule.startDate(),
                        yesterday,
                        rule.occurrenceCount(),
                        rule.anchorDayOfMonth(),
                        rule.missedPolicy(),
                        rule.createdAt(),
                        now
                );
                repository.updateRecurrenceRule(updatedRule);
            }

            // Cancel OPEN occurrences with effective_date >= today that have no events or history; keep everything else
            List<TaskOccurrence> occurrences = repository.findOccurrencesByTaskId(seriesTaskId);
            for (TaskOccurrence occ : occurrences) {
                if (occ.status() == OccurrenceStatus.OPEN && !occ.effectiveDate().isBefore(today)) {
                    List<TaskEvent> events = repository.findTaskEventsByOccurrenceId(occ.id());
                    List<com.preston.codex.domain.tasks.model.RescheduleRecord> histories = repository.findRescheduleRecordsByOccurrenceId(occ.id());
                    if (events.isEmpty() && histories.isEmpty()) {
                        TaskOccurrence cancelledOcc = new TaskOccurrence(
                                occ.id(),
                                occ.taskId(),
                                occ.ruleId(),
                                occ.occurrenceDate(),
                                occ.effectiveDate(),
                                OccurrenceStatus.CANCELLED,
                                occ.titleOverride(),
                                occ.timeOverride(),
                                occ.noteOverride(),
                                occ.reflection(),
                                occ.autoRescheduleCount(),
                                occ.completedAt(),
                                now,
                                occ.createdAt(),
                                now
                        );
                        repository.updateOccurrence(cancelledOcc);
                        recordEvent(seriesTaskId, occ.id(), "CANCELLED", "Occurrence cancelled due to series cancellation", now);
                    }
                }
            }

            recordEvent(seriesTaskId, null, "CANCELLED", "Series cancelled", now);
        });
    }

    private void recordEvent(long taskId, Long occurrenceId, String type, String details, Instant timestamp) {
        TaskEvent event = new TaskEvent(0L, taskId, occurrenceId, type, details, timestamp);
        repository.insertTaskEvent(event);
    }

    private Set<String> normalizeTags(Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> result = new HashSet<>(tags.size());
        for (String tag : tags) {
            if (tag != null && !tag.trim().isEmpty()) {
                result.add(tag.trim().toLowerCase());
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
