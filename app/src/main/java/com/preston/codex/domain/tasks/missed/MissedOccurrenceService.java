package com.preston.codex.domain.tasks.missed;

import com.preston.codex.domain.TransactionRunner;
import com.preston.codex.domain.tasks.model.OccurrenceStatus;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.RescheduleRecord;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.repository.TaskRepository;
import com.preston.codex.domain.time.LocalDateProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class MissedOccurrenceService {

    public static final int MAX_AUTO_RESCHEDULE_COUNT = 3;

    private final TaskRepository repository;
    private final LocalDateProvider dateProvider;
    private final TransactionRunner transactionRunner;

    public MissedOccurrenceService(
            TaskRepository repository,
            LocalDateProvider dateProvider,
            TransactionRunner transactionRunner
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.dateProvider = Objects.requireNonNull(dateProvider, "dateProvider must not be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
    }

    public List<TaskOccurrence> handleMissedOccurrences(LocalDate today) {
        return transactionRunner.runInTransaction(() -> {
            List<TaskOccurrence> missed = repository.findMissedOccurrences(today);
            Instant now = dateProvider.getInstant();
            List<TaskOccurrence> handled = new ArrayList<>();

            for (TaskOccurrence occ : missed) {
                // Must evaluate effective_date < today (never occurrence_date)
                if (!occ.effectiveDate().isBefore(today)) {
                    continue;
                }

                RecurrenceRule rule = repository.findRuleById(occ.ruleId());
                if (rule == null) {
                    continue;
                }
                Task parent = repository.findTaskById(occ.taskId());
                if (parent == null) {
                    continue;
                }

                switch (rule.missedPolicy()) {
                    case KEEP_OVERDUE:
                        // No change
                        break;

                    case SKIP_AUTOMATICALLY: {
                        TaskOccurrence skipped = new TaskOccurrence(
                                occ.id(),
                                occ.taskId(),
                                occ.ruleId(),
                                occ.occurrenceDate(),
                                occ.effectiveDate(),
                                OccurrenceStatus.SKIPPED,
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
                        repository.updateOccurrence(skipped);
                        repository.insertTaskEvent(new TaskEvent(0L, occ.taskId(), occ.id(), "POLICY", "Skipped automatically by missed policy", now));
                        handled.add(skipped);
                        break;
                    }

                    case CARRY_FORWARD_FLEXIBLE_ONLY: {
                        if (parent.isFlexible() && occ.autoRescheduleCount() < MAX_AUTO_RESCHEDULE_COUNT) {
                            TaskOccurrence carried = new TaskOccurrence(
                                    occ.id(),
                                    occ.taskId(),
                                    occ.ruleId(),
                                    occ.occurrenceDate(),
                                    today, // set effective_date to today
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
                            repository.updateOccurrence(carried);
                            repository.insertRescheduleRecord(new RescheduleRecord(
                                    0L,
                                    occ.taskId(),
                                    occ.id(),
                                    occ.effectiveDate(),
                                    today,
                                    "CARRY_FORWARD",
                                    now
                            ));
                            repository.insertTaskEvent(new TaskEvent(0L, occ.taskId(), occ.id(), "POLICY", "Carried forward to " + today, now));
                            handled.add(carried);
                        }
                        // Non-flexible or count >= N behaves as KEEP_OVERDUE
                        break;
                    }
                }
            }

            return handled;
        });
    }

    public List<TaskOccurrence> recentlyHandled(LocalDate date) {
        // Query occurrences with effective_date == date or SKIPPED
        List<TaskOccurrence> occurrences = repository.findOccurrencesByEffectiveDate(date);
        List<TaskOccurrence> result = new ArrayList<>();
        for (TaskOccurrence occ : occurrences) {
            List<RescheduleRecord> history = repository.findRescheduleRecordsByOccurrenceId(occ.id());
            for (RescheduleRecord record : history) {
                if (record.toDate().equals(date) && "CARRY_FORWARD".equals(record.reason())) {
                    result.add(occ);
                    break;
                }
            }
        }
        return result;
    }
}
