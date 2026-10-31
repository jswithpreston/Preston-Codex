package com.preston.codex.domain.tasks.postpone;

import com.preston.codex.domain.TransactionRunner;
import com.preston.codex.domain.tasks.model.RescheduleRecord;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.repository.TaskRepository;
import com.preston.codex.domain.time.LocalDateProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public final class PostponementService {

    private final TaskRepository repository;
    private final LocalDateProvider dateProvider;
    private final TransactionRunner transactionRunner;

    public PostponementService(
            TaskRepository repository,
            LocalDateProvider dateProvider,
            TransactionRunner transactionRunner
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.dateProvider = Objects.requireNonNull(dateProvider, "dateProvider must not be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
    }

    public void postponeOneOffTask(long taskId, LocalDate newDate, String reason) {
        Objects.requireNonNull(newDate, "newDate must not be null");
        Objects.requireNonNull(reason, "reason must not be null");

        transactionRunner.runInTransaction(() -> {
            Task task = repository.findTaskById(taskId);
            if (task == null) {
                throw new IllegalArgumentException("Task not found: " + taskId);
            }

            Instant now = dateProvider.getInstant();
            LocalDate oldDate = task.scheduledDate();

            boolean pastDeadline = task.deadline() != null && newDate.isAfter(task.deadline());
            String eventNote = pastDeadline
                    ? "Postponed to " + newDate + " (WARNING: past deadline " + task.deadline() + "): " + reason
                    : "Postponed to " + newDate + ": " + reason;

            Task updated = new Task(
                    task.id(),
                    task.taskKind(),
                    task.title(),
                    task.description(),
                    task.status(),
                    task.previousTaskId(),
                    task.knowledgeEntryId(),
                    task.reflection(),
                    task.isFlexible(),
                    task.priority(),
                    task.deadline(),
                    task.estimatedDurationMinutes(),
                    newDate,
                    task.scheduledTime(),
                    task.autoRescheduleCount(),
                    task.completedAt(),
                    task.cancelledAt(),
                    task.createdAt(),
                    now,
                    task.tags()
            );
            repository.updateTask(updated);

            repository.insertRescheduleRecord(new RescheduleRecord(
                    0L,
                    taskId,
                    null,
                    oldDate,
                    newDate,
                    reason,
                    now
            ));

            repository.insertTaskEvent(new TaskEvent(
                    0L,
                    taskId,
                    null,
                    "POSTPONED",
                    eventNote,
                    now
            ));
        });
    }

    public void postponeOccurrence(long occurrenceId, LocalDate newDate, String reason) {
        Objects.requireNonNull(newDate, "newDate must not be null");
        Objects.requireNonNull(reason, "reason must not be null");

        transactionRunner.runInTransaction(() -> {
            TaskOccurrence occ = repository.findOccurrenceById(occurrenceId);
            if (occ == null) {
                throw new IllegalArgumentException("Occurrence not found: " + occurrenceId);
            }

            Instant now = dateProvider.getInstant();
            LocalDate oldDate = occ.effectiveDate();

            TaskOccurrence updated = new TaskOccurrence(
                    occ.id(),
                    occ.taskId(),
                    occ.ruleId(),
                    occ.occurrenceDate(), // occurrence_date never changes
                    newDate,             // only effective_date changes
                    occ.status(),
                    occ.titleOverride(),
                    occ.timeOverride(),
                    occ.noteOverride(),
                    occ.reflection(),
                    occ.autoRescheduleCount(),
                    occ.completedAt(),
                    occ.cancelledAt(),
                    occ.createdAt(),
                    now
            );
            repository.updateOccurrence(updated);

            repository.insertRescheduleRecord(new RescheduleRecord(
                    0L,
                    occ.taskId(),
                    occ.id(),
                    oldDate,
                    newDate,
                    reason,
                    now
            ));

            repository.insertTaskEvent(new TaskEvent(
                    0L,
                    occ.taskId(),
                    occ.id(),
                    "POSTPONED",
                    "Occurrence postponed from " + oldDate + " to " + newDate + ": " + reason,
                    now
            ));
        });
    }
}
