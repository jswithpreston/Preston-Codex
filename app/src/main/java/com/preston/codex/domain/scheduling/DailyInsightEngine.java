package com.preston.codex.domain.scheduling;

import com.preston.codex.domain.TransactionRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Domain engine orchestrating canonical daily insight selection, date rollover,
 * and shuffle behavior.
 * Pure Java, no android.* dependencies.
 */
public final class DailyInsightEngine {

    private final DailyInsightRepository repository;
    private final InsightSelector selector;
    private final Clock clock;
    private final Supplier<ZoneId> zoneSupplier;
    private final TransactionRunner transactionRunner;

    public DailyInsightEngine(
            DailyInsightRepository repository,
            InsightSelector selector,
            Clock clock,
            Supplier<ZoneId> zoneSupplier,
            TransactionRunner transactionRunner
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.selector = Objects.requireNonNull(selector, "selector must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.zoneSupplier = Objects.requireNonNull(zoneSupplier, "zoneSupplier must not be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
    }

    /**
     * Retrieves or computes the canonical daily insight selection for the current local date.
     * Atomic, thread-safe, and handles date rollover, mid-day invalidation, and empty/single-entry libraries.
     */
    public Optional<Long> getOrSelectDailyInsight() {
        return transactionRunner.runInTransaction(() -> {
            LocalDate today = LocalDate.now(clock.withZone(zoneSupplier.get()));
            Instant now = clock.instant();

            Optional<DailyInsightSelection> existingOpt = repository.getSelectionForDate(today);
            if (existingOpt.isPresent()) {
                DailyInsightSelection existing = existingOpt.get();
                if (existing.getEntryId().isPresent()) {
                    long entryId = existing.getEntryId().get();
                    if (repository.isEntryEligible(entryId)) {
                        return Optional.of(entryId);
                    }
                }
                // Selection exists but is invalidated (null entryId or entry no longer eligible)
                return reselectInvalidated(today, now, existing.isWasShuffled());
            }

            // No selection exists for today; create initial AUTO selection
            List<Long> eligible = new ArrayList<>(repository.getEligibleEntryIds());
            if (eligible.isEmpty()) {
                return Optional.empty();
            }
            Collections.sort(eligible);

            if (eligible.size() == 1) {
                long singleId = eligible.get(0);
                return persistInitialSelection(today, singleId, now);
            }

            // Exclude most recent prior selection if alternatives exist
            List<Long> candidates = filterPriorSelection(eligible, today);
            long selectedId = selector.select(candidates);
            return persistInitialSelection(today, selectedId, now);
        });
    }

    /**
     * Executes a shuffle operation for today's selection.
     */
    public ShuffleOutcome shuffle() {
        return transactionRunner.runInTransaction(() -> {
            LocalDate today = LocalDate.now(clock.withZone(zoneSupplier.get()));
            Instant now = clock.instant();

            // Ensure today has an active selection first
            Optional<Long> currentOpt = getOrSelectDailyInsight();
            List<Long> eligible = new ArrayList<>(repository.getEligibleEntryIds());
            if (eligible.isEmpty()) {
                return new ShuffleOutcome(ShuffleResult.EMPTY, null);
            }
            Collections.sort(eligible);

            if (eligible.size() == 1) {
                return new ShuffleOutcome(ShuffleResult.NO_ALTERNATIVE, eligible.get(0));
            }

            Long currentId = currentOpt.orElse(null);
            List<Long> candidates = new ArrayList<>();
            for (Long id : eligible) {
                if (currentId == null || !currentId.equals(id)) {
                    candidates.add(id);
                }
            }
            if (candidates.isEmpty()) {
                candidates = eligible;
            }

            long newSelectedId = selector.select(candidates);
            DailyInsightSelection updated = new DailyInsightSelection(today, newSelectedId, now, true);
            repository.updateSelection(updated);

            DailyInsightHistory history = new DailyInsightHistory(0, today, newSelectedId, DailyInsightHistoryReason.SHUFFLE, now);
            repository.recordHistory(history);

            return new ShuffleOutcome(ShuffleResult.SHUFFLED, newSelectedId);
        });
    }

    private Optional<Long> persistInitialSelection(LocalDate today, long selectedId, Instant now) {
        DailyInsightSelection newSelection = new DailyInsightSelection(today, selectedId, now, false);
        boolean inserted = repository.insertOrIgnoreSelection(newSelection);
        if (inserted) {
            DailyInsightHistory history = new DailyInsightHistory(0, today, selectedId, DailyInsightHistoryReason.AUTO, now);
            repository.recordHistory(history);
            return Optional.of(selectedId);
        } else {
            // Concurrent caller inserted; re-read canonical row
            return repository.getSelectionForDate(today)
                    .flatMap(DailyInsightSelection::getEntryId);
        }
    }

    private Optional<Long> reselectInvalidated(LocalDate today, Instant now, boolean wasShuffled) {
        List<Long> eligible = new ArrayList<>(repository.getEligibleEntryIds());
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        Collections.sort(eligible);

        long newSelectedId;
        if (eligible.size() == 1) {
            newSelectedId = eligible.get(0);
        } else {
            List<Long> candidates = filterPriorSelection(eligible, today);
            newSelectedId = selector.select(candidates);
        }

        DailyInsightSelection updated = new DailyInsightSelection(today, newSelectedId, now, wasShuffled);
        repository.updateSelection(updated);

        DailyInsightHistory history = new DailyInsightHistory(0, today, newSelectedId, DailyInsightHistoryReason.INVALIDATED, now);
        repository.recordHistory(history);

        return Optional.of(newSelectedId);
    }

    private List<Long> filterPriorSelection(List<Long> eligible, LocalDate today) {
        Optional<DailyInsightSelection> priorOpt = repository.getMostRecentPriorSelection(today);
        if (priorOpt.isPresent() && priorOpt.get().getEntryId().isPresent()) {
            long priorId = priorOpt.get().getEntryId().get();
            List<Long> candidates = new ArrayList<>();
            for (Long id : eligible) {
                if (id != priorId) {
                    candidates.add(id);
                }
            }
            if (!candidates.isEmpty()) {
                return candidates;
            }
        }
        return eligible;
    }
}
