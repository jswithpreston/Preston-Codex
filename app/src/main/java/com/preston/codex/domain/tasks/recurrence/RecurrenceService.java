package com.preston.codex.domain.tasks.recurrence;

import com.preston.codex.domain.TransactionRunner;
import com.preston.codex.domain.tasks.model.OccurrenceStatus;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.model.TaskStatus;
import com.preston.codex.domain.tasks.repository.TaskRepository;
import com.preston.codex.domain.time.LocalDateProvider;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class RecurrenceService {

    private final TaskRepository repository;
    private final OccurrenceGenerator generator;
    private final LocalDateProvider dateProvider;
    private final TransactionRunner transactionRunner;

    public RecurrenceService(
            TaskRepository repository,
            OccurrenceGenerator generator,
            LocalDateProvider dateProvider,
            TransactionRunner transactionRunner
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.generator = Objects.requireNonNull(generator, "generator must not be null");
        this.dateProvider = Objects.requireNonNull(dateProvider, "dateProvider must not be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
    }

    public void ensureMaterialized(LocalDate today) {
        transactionRunner.runInTransaction(() -> {
            List<RecurrenceRule> rules = repository.findAllRules();
            Instant now = dateProvider.getInstant();

            for (RecurrenceRule rule : rules) {
                Task parent = repository.findTaskById(rule.taskId());
                if (parent == null || parent.status() == TaskStatus.CANCELLED) {
                    continue;
                }
                if (rule.endDate() != null && rule.endDate().isBefore(today)) {
                    continue;
                }
                materializeRule(rule, today, Collections.emptySet(), now);
            }
        });
    }

    public void materializeRule(RecurrenceRule rule, LocalDate today, Set<LocalDate> retainedDates, Instant now) {
        List<TaskOccurrence> existing = repository.findOccurrencesByRuleId(rule.id());
        Set<LocalDate> existingDates = new HashSet<>();
        for (TaskOccurrence occ : existing) {
            existingDates.add(occ.occurrenceDate());
        }

        List<LocalDate> generatedDates = generator.generate(
                rule,
                today,
                existingDates,
                retainedDates,
                existing.size()
        );

        if (generatedDates.isEmpty()) {
            return;
        }

        List<TaskOccurrence> toInsert = new ArrayList<>(generatedDates.size());
        for (LocalDate date : generatedDates) {
            TaskOccurrence occurrence = new TaskOccurrence(
                    0L,
                    rule.taskId(),
                    rule.id(),
                    date,
                    date,
                    OccurrenceStatus.OPEN,
                    null,
                    null,
                    null,
                    null,
                    0,
                    null,
                    null,
                    now,
                    now
            );
            toInsert.add(occurrence);
        }

        repository.insertOccurrences(toInsert);
    }
}
