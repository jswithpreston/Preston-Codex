package com.preston.codex.domain.tasks.backlog;

import com.preston.codex.domain.TransactionRunner;
import com.preston.codex.domain.tasks.model.OccurrenceStatus;
import com.preston.codex.domain.tasks.model.RescheduleRecord;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.model.TaskPriority;
import com.preston.codex.domain.tasks.model.TaskStatus;
import com.preston.codex.domain.tasks.repository.TaskRepository;
import com.preston.codex.domain.time.LocalDateProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class BacklogReviewService {

    private final TaskRepository repository;
    private final LocalDateProvider dateProvider;
    private final TransactionRunner transactionRunner;

    public BacklogReviewService(
            TaskRepository repository,
            LocalDateProvider dateProvider,
            TransactionRunner transactionRunner
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.dateProvider = Objects.requireNonNull(dateProvider, "dateProvider must not be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
    }

    public BacklogReviewPreview preview() {
        LocalDate today = dateProvider.getToday();
        List<BacklogCandidate> candidates = new ArrayList<>();

        // 1. One-off overdue tasks
        List<Task> overdueOneOffs = repository.findOverdueOneOffTasks(today);
        for (Task task : overdueOneOffs) {
            int duration = task.estimatedDurationMinutes() != null ? task.estimatedDurationMinutes() : TaskPlanner.DEFAULT_ESTIMATE_MINUTES;
            candidates.add(new BacklogCandidate(
                    task.id(),
                    TaskKind.ONE_OFF,
                    task.title(),
                    task.priority(),
                    task.scheduledDate(),
                    task.deadline(),
                    duration,
                    task.autoRescheduleCount(),
                    task.isFlexible()
            ));
        }

        // 2. Missed occurrences
        List<TaskOccurrence> missedOccs = repository.findMissedOccurrences(today);
        for (TaskOccurrence occ : missedOccs) {
            Task parent = repository.findTaskById(occ.taskId());
            if (parent == null) {
                continue;
            }
            int duration = parent.estimatedDurationMinutes() != null ? parent.estimatedDurationMinutes() : TaskPlanner.DEFAULT_ESTIMATE_MINUTES;
            candidates.add(new BacklogCandidate(
                    occ.id(),
                    TaskKind.SERIES,
                    occ.titleOverride() != null ? occ.titleOverride() : parent.title(),
                    parent.priority(),
                    occ.effectiveDate(),
                    null, // occurrences have no deadline of their own
                    duration,
                    occ.autoRescheduleCount(),
                    parent.isFlexible()
            ));
        }

        // 3. Compute initial day loads for the 14-day horizon
        Map<LocalDate, TaskPlanner.DayLoad> initialLoads = computeLoadsForHorizon(today);

        return TaskPlanner.plan(candidates, today, initialLoads);
    }

    public BacklogReviewReport apply(List<BacklogMoveProposal> approvedMoves) {
        Objects.requireNonNull(approvedMoves, "approvedMoves must not be null");

        return transactionRunner.runInTransaction(() -> {
            LocalDate today = dateProvider.getToday();
            Instant now = dateProvider.getInstant();
            Map<LocalDate, TaskPlanner.DayLoad> currentLoads = computeLoadsForHorizon(today);

            List<BacklogMoveProposal> applied = new ArrayList<>();
            List<SkippedMove> skipped = new ArrayList<>();

            for (BacklogMoveProposal move : approvedMoves) {
                // Re-validation step inside transaction
                if (move.kind() == TaskKind.ONE_OFF) {
                    Task task = repository.findTaskById(move.id());
                    if (task == null || (task.status() != TaskStatus.OPEN && task.status() != TaskStatus.IN_PROGRESS)) {
                        skipped.add(new SkippedMove(move, "ALREADY_COMPLETED"));
                        continue;
                    }
                    if (task.deadline() != null && move.toDate().isAfter(task.deadline())) {
                        skipped.add(new SkippedMove(move, "PAST_DEADLINE"));
                        continue;
                    }

                    // Check caps on target day
                    TaskPlanner.DayLoad load = currentLoads.computeIfAbsent(move.toDate(), d -> new TaskPlanner.DayLoad(0, 0));
                    int duration = move.durationMinutes() > 0 ? move.durationMinutes() : TaskPlanner.DEFAULT_ESTIMATE_MINUTES;
                    if (load.taskCount + 1 > TaskPlanner.MAX_TASKS_PER_DAY || load.totalMinutes + duration > TaskPlanner.MAX_MINUTES_PER_DAY) {
                        skipped.add(new SkippedMove(move, "CAP_EXCEEDED"));
                        continue;
                    }

                    // Apply move
                    load.taskCount += 1;
                    load.totalMinutes += duration;

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
                            move.toDate(),
                            task.scheduledTime(),
                            task.autoRescheduleCount() + 1,
                            task.completedAt(),
                            task.cancelledAt(),
                            task.createdAt(),
                            now,
                            task.tags()
                    );
                    repository.updateTask(updated);

                    repository.insertRescheduleRecord(new RescheduleRecord(
                            0L,
                            task.id(),
                            null,
                            task.scheduledDate(),
                            move.toDate(),
                            "BACKLOG_AUTO_RESCHEDULE",
                            now
                    ));

                    repository.insertTaskEvent(new TaskEvent(
                            0L,
                            task.id(),
                            null,
                            "AUTO_RESCHEDULED",
                            "Auto-rescheduled from " + task.scheduledDate() + " to " + move.toDate(),
                            now
                    ));

                    applied.add(move);

                } else if (move.kind() == TaskKind.SERIES) {
                    TaskOccurrence occ = repository.findOccurrenceById(move.id());
                    if (occ == null || (occ.status() != OccurrenceStatus.OPEN && occ.status() != OccurrenceStatus.IN_PROGRESS)) {
                        skipped.add(new SkippedMove(move, "ALREADY_COMPLETED"));
                        continue;
                    }

                    TaskPlanner.DayLoad load = currentLoads.computeIfAbsent(move.toDate(), d -> new TaskPlanner.DayLoad(0, 0));
                    int duration = move.durationMinutes() > 0 ? move.durationMinutes() : TaskPlanner.DEFAULT_ESTIMATE_MINUTES;
                    if (load.taskCount + 1 > TaskPlanner.MAX_TASKS_PER_DAY || load.totalMinutes + duration > TaskPlanner.MAX_MINUTES_PER_DAY) {
                        skipped.add(new SkippedMove(move, "CAP_EXCEEDED"));
                        continue;
                    }

                    load.taskCount += 1;
                    load.totalMinutes += duration;

                    TaskOccurrence updated = new TaskOccurrence(
                            occ.id(),
                            occ.taskId(),
                            occ.ruleId(),
                            occ.occurrenceDate(),
                            move.toDate(),
                            occ.status(),
                            occ.titleOverride(),
                            occ.timeOverride(),
                            occ.noteOverride(),
                            occ.reflection(),
                            occ.autoRescheduleCount() + 1,
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
                            occ.effectiveDate(),
                            move.toDate(),
                            "BACKLOG_AUTO_RESCHEDULE",
                            now
                    ));

                    repository.insertTaskEvent(new TaskEvent(
                            0L,
                            occ.taskId(),
                            occ.id(),
                            "AUTO_RESCHEDULED",
                            "Occurrence auto-rescheduled from " + occ.effectiveDate() + " to " + move.toDate(),
                            now
                    ));

                    applied.add(move);
                }
            }

            return new BacklogReviewReport(applied, skipped);
        });
    }

    private Map<LocalDate, TaskPlanner.DayLoad> computeLoadsForHorizon(LocalDate today) {
        Map<LocalDate, TaskPlanner.DayLoad> loads = new HashMap<>();
        for (int i = 0; i < TaskPlanner.HORIZON_DAYS; i++) {
            LocalDate date = today.plusDays(i);
            int count = 0;
            int minutes = 0;

            List<Task> oneOffs = repository.findOneOffTasksByScheduledDate(date);
            for (Task t : oneOffs) {
                if (t.status() == TaskStatus.OPEN || t.status() == TaskStatus.IN_PROGRESS) {
                    count++;
                    minutes += (t.estimatedDurationMinutes() != null ? t.estimatedDurationMinutes() : TaskPlanner.DEFAULT_ESTIMATE_MINUTES);
                }
            }

            List<TaskOccurrence> occs = repository.findOccurrencesByEffectiveDate(date);
            for (TaskOccurrence o : occs) {
                if (o.status() == OccurrenceStatus.OPEN || o.status() == OccurrenceStatus.IN_PROGRESS) {
                    count++;
                    Task parent = repository.findTaskById(o.taskId());
                    int dur = (parent != null && parent.estimatedDurationMinutes() != null)
                            ? parent.estimatedDurationMinutes()
                            : TaskPlanner.DEFAULT_ESTIMATE_MINUTES;
                    minutes += dur;
                }
            }

            loads.put(date, new TaskPlanner.DayLoad(count, minutes));
        }
        return loads;
    }
}
