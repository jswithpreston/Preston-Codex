package com.preston.codex.domain.scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.preston.codex.domain.TransactionRunner;

import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class DailyInsightEngineTest {

    private InMemoryDailyInsightRepository repository;
    private MutableClock clock;
    private AtomicReference<ZoneId> currentZone;
    private DirectTransactionRunner transactionRunner;
    private DailyInsightEngine engine;

    @Before
    public void setUp() {
        repository = new InMemoryDailyInsightRepository();
        // Start on 2026-10-25 at 10:00:00 UTC
        clock = new MutableClock(Instant.parse("2026-10-25T10:00:00Z"), ZoneOffset.UTC);
        currentZone = new AtomicReference<>(ZoneOffset.UTC);
        transactionRunner = new DirectTransactionRunner();
        // Deterministic selector for predictable tests: picks candidate at index 0 (or next available)
        InsightSelector deterministicSelector = candidates -> candidates.get(0);
        engine = new DailyInsightEngine(repository, deterministicSelector, clock, currentZone::get, transactionRunner);
    }

    @Test
    public void getDailyInsight_emptyLibrary_returnsEmpty() {
        Optional<Long> result = engine.getOrSelectDailyInsight();
        assertFalse("Empty library must return empty Optional", result.isPresent());
        assertTrue("No history should be written for empty library", repository.historyLog.isEmpty());
    }

    @Test
    public void getDailyInsight_singleEntry_returnsSameEntryEveryDay() {
        repository.addEligibleEntry(42L);

        Optional<Long> day1 = engine.getOrSelectDailyInsight();
        assertTrue(day1.isPresent());
        assertEquals(Long.valueOf(42L), day1.get());

        // Rollover to next day
        clock.advanceDays(1);
        Optional<Long> day2 = engine.getOrSelectDailyInsight();
        assertTrue(day2.isPresent());
        assertEquals("Single entry allowed to repeat consecutively", Long.valueOf(42L), day2.get());
    }

    @Test
    public void getDailyInsight_multiEntry_excludesMostRecentPriorSelection() {
        repository.addEligibleEntry(101L);
        repository.addEligibleEntry(102L);

        // Day 1: candidates [101, 102] -> selector picks 101
        Optional<Long> day1 = engine.getOrSelectDailyInsight();
        assertEquals(Long.valueOf(101L), day1.get());

        // Advance 3 days to simulate skipping a weekend: last prior was 101
        clock.advanceDays(3);
        // Day 4: candidates should exclude 101 -> candidates [102] -> selected 102
        Optional<Long> day4 = engine.getOrSelectDailyInsight();
        assertEquals("Multi-entry library must exclude most recent prior selection", Long.valueOf(102L), day4.get());
        assertNotEquals(day1.get(), day4.get());
    }

    @Test
    public void getDailyInsight_archivedAndTrashedEntries_excluded() {
        repository.addEligibleEntry(10L);
        repository.addArchivedEntry(20L);
        repository.addTrashedEntry(30L);

        Optional<Long> selected = engine.getOrSelectDailyInsight();
        assertTrue(selected.isPresent());
        assertEquals(Long.valueOf(10L), selected.get());
    }

    @Test
    public void getDailyInsight_stableAcrossMultipleCallsSameDate() {
        repository.addEligibleEntry(101L);
        repository.addEligibleEntry(102L);

        Optional<Long> call1 = engine.getOrSelectDailyInsight();
        Optional<Long> call2 = engine.getOrSelectDailyInsight();
        assertEquals(call1, call2);
        assertEquals("Only 1 history entry should exist for the date", 1, repository.historyLog.size());
    }

    @Test
    public void getDailyInsight_recordsInitialSelectionAsAutoInHistory() {
        repository.addEligibleEntry(55L);

        Optional<Long> selected = engine.getOrSelectDailyInsight();
        assertTrue(selected.isPresent());

        List<DailyInsightHistory> history = repository.getHistoryForDate(LocalDate.of(2026, 10, 25));
        assertEquals(1, history.size());
        assertEquals(DailyInsightHistoryReason.AUTO, history.get(0).getReason());
        assertEquals(Long.valueOf(55L), history.get(0).getEntryId().get());
    }

    @Test
    public void getDailyInsight_nullEntryId_reselectsAndRecordsInvalidated() {
        repository.addEligibleEntry(1L);
        repository.addEligibleEntry(2L);

        // Select initial
        engine.getOrSelectDailyInsight();

        // Simulate entry #1 deleted in DB -> ON DELETE SET NULL
        LocalDate today = LocalDate.of(2026, 10, 25);
        repository.selections.put(today, new DailyInsightSelection(today, null, clock.instant(), false));
        repository.eligibleIds.remove(1L);

        // Calling engine again must re-select entry #2 with reason INVALIDATED
        Optional<Long> reselected = engine.getOrSelectDailyInsight();
        assertTrue(reselected.isPresent());
        assertEquals(Long.valueOf(2L), reselected.get());

        List<DailyInsightHistory> history = repository.getHistoryForDate(today);
        assertEquals(2, history.size());
        assertEquals(DailyInsightHistoryReason.AUTO, history.get(0).getReason());
        assertEquals(DailyInsightHistoryReason.INVALIDATED, history.get(1).getReason());
        assertEquals(Long.valueOf(2L), history.get(1).getEntryId().get());
    }

    @Test
    public void getDailyInsight_concurrentCallers_safelyResolveSameSelection() {
        repository.addEligibleEntry(101L);
        repository.addEligibleEntry(102L);

        // Simulate race condition where insertOrIgnoreSelection returns false because
        // another thread concurrently inserted entry 102
        repository.simulateConcurrentInsertOnNextCall(new DailyInsightSelection(
                LocalDate.of(2026, 10, 25), 102L, clock.instant(), false
        ));

        Optional<Long> result = engine.getOrSelectDailyInsight();
        assertTrue(result.isPresent());
        assertEquals("Must safely adopt the concurrently inserted selection", Long.valueOf(102L), result.get());
    }

    @Test
    public void dateRollover_advancesSelectionAndAppendsHistory() {
        repository.addEligibleEntry(1L);
        repository.addEligibleEntry(2L);

        Optional<Long> day1 = engine.getOrSelectDailyInsight();
        clock.advanceDays(1);
        Optional<Long> day2 = engine.getOrSelectDailyInsight();

        assertNotEquals(day1.get(), day2.get());
        assertEquals(2, repository.historyLog.size());
    }

    @Test
    public void dateRollover_dstObservingZone_correctDateRollover() {
        // America/New_York transitions from EDT to EST in early November
        ZoneId nyZone = ZoneId.of("America/New_York");
        currentZone.set(nyZone);

        repository.addEligibleEntry(1L);
        repository.addEligibleEntry(2L);

        // 2026-11-01 01:30 EDT -> instant is 2026-11-01T05:30:00Z
        clock.setInstant(Instant.parse("2026-11-01T05:30:00Z"));
        Optional<Long> day1 = engine.getOrSelectDailyInsight();
        assertEquals(LocalDate.of(2026, 11, 1), repository.selections.keySet().iterator().next());

        // Advance 25 hours (crossing 2:00 AM DST fallback)
        clock.advanceHours(25);
        Optional<Long> day2 = engine.getOrSelectDailyInsight();
        assertTrue(repository.selections.containsKey(LocalDate.of(2026, 11, 2)));
    }

    @Test
    public void dateRollover_zoneChange_evaluatesLocalDayInNewZone() {
        repository.addEligibleEntry(1L);
        repository.addEligibleEntry(2L);

        // Set time to 2026-10-25 18:00 UTC (still Oct 25 in UTC)
        clock.setInstant(Instant.parse("2026-10-25T18:00:00Z"));
        currentZone.set(ZoneOffset.UTC);
        Optional<Long> utcSelection = engine.getOrSelectDailyInsight();
        assertEquals(Long.valueOf(1L), utcSelection.get());
        assertTrue(repository.selections.containsKey(LocalDate.of(2026, 10, 25)));

        // User moves to Tokyo (UTC+9), where 18:00 UTC is Oct 26 03:00 AM!
        currentZone.set(ZoneId.of("Asia/Tokyo"));
        Optional<Long> tokyoSelection = engine.getOrSelectDailyInsight();
        assertEquals(Long.valueOf(2L), tokyoSelection.get());
        assertTrue(repository.selections.containsKey(LocalDate.of(2026, 10, 26)));
    }

    @Test
    public void shuffle_emptyLibrary_returnsEmpty() {
        ShuffleOutcome outcome = engine.shuffle();
        assertEquals(ShuffleResult.EMPTY, outcome.getResult());
        assertFalse(outcome.getSelectedEntryId().isPresent());
    }

    @Test
    public void shuffle_singleEntry_returnsNoAlternative() {
        repository.addEligibleEntry(7L);
        ShuffleOutcome outcome = engine.shuffle();
        assertEquals(ShuffleResult.NO_ALTERNATIVE, outcome.getResult());
        assertEquals(Long.valueOf(7L), outcome.getSelectedEntryId().get());
    }

    @Test
    public void shuffle_multiEntry_returnsShuffledAndPicksDifferentEntry() {
        repository.addEligibleEntry(1L);
        repository.addEligibleEntry(2L);

        // Initially select entry 1
        Optional<Long> initial = engine.getOrSelectDailyInsight();
        assertEquals(Long.valueOf(1L), initial.get());

        // Shuffle must pick alternative 2
        ShuffleOutcome outcome = engine.shuffle();
        assertEquals(ShuffleResult.SHUFFLED, outcome.getResult());
        assertEquals(Long.valueOf(2L), outcome.getSelectedEntryId().get());
        assertTrue(repository.selections.get(LocalDate.of(2026, 10, 25)).isWasShuffled());
    }

    @Test
    public void shuffle_recordsSelectedEntryAsShuffleInHistory() {
        repository.addEligibleEntry(1L);
        repository.addEligibleEntry(2L);

        engine.getOrSelectDailyInsight();
        engine.shuffle();

        List<DailyInsightHistory> history = repository.getHistoryForDate(LocalDate.of(2026, 10, 25));
        assertEquals(2, history.size());
        assertEquals(DailyInsightHistoryReason.AUTO, history.get(0).getReason());
        assertEquals(Long.valueOf(1L), history.get(0).getEntryId().get());

        // Amendment 8: Shuffle records the entry that became selected (entry 2) with reason SHUFFLE
        assertEquals(DailyInsightHistoryReason.SHUFFLE, history.get(1).getReason());
        assertEquals(Long.valueOf(2L), history.get(1).getEntryId().get());
    }

    // --- In-Memory Test Fixtures ---

    static class DirectTransactionRunner implements TransactionRunner {
        @Override
        public <T> T runInTransaction(Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }

    static class MutableClock extends Clock {
        private Instant instant;
        private final ZoneId zone;

        public MutableClock(Instant initial, ZoneId zone) {
            this.instant = initial;
            this.zone = zone;
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }

        public void advanceDays(int days) {
            this.instant = this.instant.plusSeconds(days * 86400L);
        }

        public void advanceHours(int hours) {
            this.instant = this.instant.plusSeconds(hours * 3600L);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    static class InMemoryDailyInsightRepository implements DailyInsightRepository {
        final Map<LocalDate, DailyInsightSelection> selections = new HashMap<>();
        final List<DailyInsightHistory> historyLog = new ArrayList<>();
        final Set<Long> eligibleIds = new HashSet<>();
        final Set<Long> archivedIds = new HashSet<>();
        final Set<Long> trashedIds = new HashSet<>();
        private DailyInsightSelection concurrentInsert = null;

        void addEligibleEntry(long id) { eligibleIds.add(id); }
        void addArchivedEntry(long id) { archivedIds.add(id); }
        void addTrashedEntry(long id) { trashedIds.add(id); }

        void simulateConcurrentInsertOnNextCall(DailyInsightSelection selection) {
            this.concurrentInsert = selection;
        }

        @Override
        public Optional<DailyInsightSelection> getSelectionForDate(LocalDate date) {
            return Optional.ofNullable(selections.get(date));
        }

        @Override
        public boolean insertOrIgnoreSelection(DailyInsightSelection selection) {
            if (concurrentInsert != null) {
                selections.put(concurrentInsert.getLocalDate(), concurrentInsert);
                concurrentInsert = null;
                return false;
            }
            if (selections.containsKey(selection.getLocalDate())) {
                return false;
            }
            selections.put(selection.getLocalDate(), selection);
            return true;
        }

        @Override
        public void updateSelection(DailyInsightSelection selection) {
            selections.put(selection.getLocalDate(), selection);
        }

        @Override
        public void recordHistory(DailyInsightHistory history) {
            historyLog.add(history);
        }

        @Override
        public List<DailyInsightHistory> getHistoryForDate(LocalDate date) {
            List<DailyInsightHistory> result = new ArrayList<>();
            for (DailyInsightHistory h : historyLog) {
                if (h.getLocalDate().equals(date)) {
                    result.add(h);
                }
            }
            return result;
        }

        @Override
        public Optional<DailyInsightSelection> getMostRecentPriorSelection(LocalDate beforeDate) {
            LocalDate bestDate = null;
            for (LocalDate date : selections.keySet()) {
                if (date.isBefore(beforeDate)) {
                    if (bestDate == null || date.isAfter(bestDate)) {
                        bestDate = date;
                    }
                }
            }
            return bestDate != null ? Optional.ofNullable(selections.get(bestDate)) : Optional.empty();
        }

        @Override
        public List<Long> getEligibleEntryIds() {
            List<Long> result = new ArrayList<>(eligibleIds);
            Collections.sort(result);
            return result;
        }

        @Override
        public boolean isEntryEligible(long entryId) {
            return eligibleIds.contains(entryId);
        }
    }
}
