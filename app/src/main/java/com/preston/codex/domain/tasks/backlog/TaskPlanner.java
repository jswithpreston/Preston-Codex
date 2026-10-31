package com.preston.codex.domain.tasks.backlog;

import com.preston.codex.domain.tasks.model.TaskPriority;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TaskPlanner {

    public static final int MAX_TASKS_PER_DAY = 6;
    public static final int MAX_MINUTES_PER_DAY = 240;
    public static final int HORIZON_DAYS = 14;
    public static final int MAX_AUTO_RESCHEDULES = 3;
    public static final int DEFAULT_ESTIMATE_MINUTES = 30;

    public static final class DayLoad {
        public int taskCount;
        public int totalMinutes;

        public DayLoad(int taskCount, int totalMinutes) {
            this.taskCount = taskCount;
            this.totalMinutes = totalMinutes;
        }
    }

    public static BacklogReviewPreview plan(
            List<BacklogCandidate> candidates,
            LocalDate today,
            Map<LocalDate, DayLoad> initialLoads
    ) {
        Objects.requireNonNull(candidates, "candidates must not be null");
        Objects.requireNonNull(today, "today must not be null");

        Map<LocalDate, DayLoad> currentLoads = new HashMap<>();
        if (initialLoads != null) {
            for (Map.Entry<LocalDate, DayLoad> entry : initialLoads.entrySet()) {
                currentLoads.put(entry.getKey(), new DayLoad(entry.getValue().taskCount, entry.getValue().totalMinutes));
            }
        }

        List<BacklogCandidate> eligible = new ArrayList<>();
        List<BacklogCandidate> needsAttention = new ArrayList<>();

        for (BacklogCandidate c : candidates) {
            if (!c.isFlexible()) {
                // Non-flexible tasks are never moved
                continue;
            }
            if (c.autoRescheduleCount() >= MAX_AUTO_RESCHEDULES) {
                needsAttention.add(c);
                continue;
            }
            if (c.deadline() != null && c.deadline().isBefore(today)) {
                // Deadline has already passed
                needsAttention.add(c);
                continue;
            }
            eligible.add(c);
        }

        // Sort: priority desc, deadline asc (nulls last), days overdue desc, id asc
        Comparator<BacklogCandidate> comparator = Comparator
                .<BacklogCandidate>comparingInt(c -> -priorityWeight(c.priority()))
                .thenComparing(BacklogCandidate::deadline, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparingLong(c -> -ChronoUnit.DAYS.between(c.originalDate(), today))
                .thenComparingLong(BacklogCandidate::id);
        eligible.sort(comparator);

        List<BacklogMoveProposal> proposedMoves = new ArrayList<>();

        for (BacklogCandidate candidate : eligible) {
            boolean placed = false;
            for (int i = 0; i < HORIZON_DAYS; i++) {
                LocalDate targetDate = today.plusDays(i);

                // Deadline constraint: cannot place past deadline
                if (candidate.deadline() != null && targetDate.isAfter(candidate.deadline())) {
                    // Cannot place on or after this date
                    break;
                }

                DayLoad load = currentLoads.computeIfAbsent(targetDate, d -> new DayLoad(0, 0));
                int duration = candidate.durationMinutes() > 0 ? candidate.durationMinutes() : DEFAULT_ESTIMATE_MINUTES;

                if (load.taskCount + 1 <= MAX_TASKS_PER_DAY && load.totalMinutes + duration <= MAX_MINUTES_PER_DAY) {
                    load.taskCount += 1;
                    load.totalMinutes += duration;
                    proposedMoves.add(new BacklogMoveProposal(
                            candidate.id(),
                            candidate.kind(),
                            candidate.title(),
                            candidate.originalDate(),
                            targetDate,
                            duration
                    ));
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                needsAttention.add(candidate);
            }
        }

        return new BacklogReviewPreview(proposedMoves, needsAttention);
    }

    private static int priorityWeight(TaskPriority priority) {
        if (priority == null) return 0;
        switch (priority) {
            case URGENT: return 4;
            case HIGH: return 3;
            case MEDIUM: return 2;
            case LOW: return 1;
            default: return 0;
        }
    }
}
