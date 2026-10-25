package com.preston.codex.domain.recall;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ReviewSchedulerTest {

    private InMemoryReviewRepository repository;
    private MutableClock clock;
    private AtomicReference<ZoneId> currentZone;
    private DirectTransactionRunner transactionRunner;
    private ReviewConfig config;
    private ReviewScheduler scheduler;

    @Before
    public void setUp() {
        repository = new InMemoryReviewRepository();
        clock = new MutableClock(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC);
        currentZone = new AtomicReference<>(ZoneOffset.UTC);
        transactionRunner = new DirectTransactionRunner();
        config = new ReviewConfig(Arrays.asList(1, 3, 7, 14, 30, 60, 120), 20);
        scheduler = new ReviewScheduler(repository, config, clock, currentZone::get, transactionRunner);
    }

    // --- Amendment 10: Tests for First Review of Never-Reviewed Entry ---

    @Test
    public void submitReview_newEntry_forgot_initializesWithZerosAndInterval1() {
        repository.addEligibleEntry(1L);

        ReviewSchedule schedule = scheduler.submitReview(1L, ReviewRating.FORGOT);
        assertEquals(0, schedule.getStep());
        assertEquals(LocalDate.of(2026, 10, 26), schedule.getNextReviewDate()); // today + 1
        assertEquals(1, schedule.getReviewCount());

        ReviewRecord record = repository.records.get(0);
        assertEquals(0, record.getPreviousStep());
        assertEquals(0, record.getPreviousIntervalDays());
        assertNull(record.getRawPreviousNextReviewDate());
        assertEquals(0, record.getNewStep());
        assertEquals(1, record.getNewIntervalDays());
    }

    @Test
    public void submitReview_newEntry_difficult_initializesWithZerosAndInterval1() {
        repository.addEligibleEntry(1L);

        ReviewSchedule schedule = scheduler.submitReview(1L, ReviewRating.DIFFICULT);
        assertEquals(0, schedule.getStep()); // max(0, 0 - 1) = 0
        assertEquals(LocalDate.of(2026, 10, 26), schedule.getNextReviewDate()); // today + 1

        ReviewRecord record = repository.records.get(0);
        assertEquals(0, record.getPreviousStep());
        assertEquals(0, record.getPreviousIntervalDays());
        assertNull(record.getRawPreviousNextReviewDate());
        assertEquals(0, record.getNewStep());
        assertEquals(1, record.getNewIntervalDays());
    }

    @Test
    public void submitReview_newEntry_remembered_initializesWithZerosAndAdvancesStep1() {
        repository.addEligibleEntry(1L);

        ReviewSchedule schedule = scheduler.submitReview(1L, ReviewRating.REMEMBERED);
        assertEquals(1, schedule.getStep()); // 0 + 1 = 1
        assertEquals(LocalDate.of(2026, 10, 28), schedule.getNextReviewDate()); // today + 3

        ReviewRecord record = repository.records.get(0);
        assertEquals(0, record.getPreviousStep());
        assertEquals(0, record.getPreviousIntervalDays());
        assertNull(record.getRawPreviousNextReviewDate());
        assertEquals(1, record.getNewStep());
        assertEquals(3, record.getNewIntervalDays());
    }

    @Test
    public void submitReview_newEntry_easy_initializesWithZerosAndAdvancesStep2() {
        repository.addEligibleEntry(1L);

        ReviewSchedule schedule = scheduler.submitReview(1L, ReviewRating.EASY);
        assertEquals(2, schedule.getStep()); // 0 + 2 = 2
        assertEquals(LocalDate.of(2026, 11, 1), schedule.getNextReviewDate()); // today + 7

        ReviewRecord record = repository.records.get(0);
        assertEquals(0, record.getPreviousStep());
        assertEquals(0, record.getPreviousIntervalDays());
        assertNull(record.getRawPreviousNextReviewDate());
        assertEquals(2, record.getNewStep());
        assertEquals(7, record.getNewIntervalDays());
    }

    // --- Transitions on Previously Reviewed Entries ---

    @Test
    public void submitReview_forgot_resetsToStep0() {
        repository.addEligibleEntry(1L);
        // Pre-existing schedule at step 4 (30 days)
        repository.schedules.put(1L, new ReviewSchedule(1L, 4, LocalDate.of(2026, 10, 25), clock.instant(), 5));

        ReviewSchedule updated = scheduler.submitReview(1L, ReviewRating.FORGOT);
        assertEquals(0, updated.getStep());
        assertEquals(LocalDate.of(2026, 10, 26), updated.getNextReviewDate());

        ReviewRecord record = repository.records.get(0);
        assertEquals(4, record.getPreviousStep());
        assertEquals(30, record.getPreviousIntervalDays());
        assertEquals(LocalDate.of(2026, 10, 25), record.getRawPreviousNextReviewDate());
        assertEquals(0, record.getNewStep());
        assertEquals(1, record.getNewIntervalDays());
    }

    @Test
    public void submitReview_difficult_dropsOneStepClampedTo0() {
        repository.addEligibleEntry(1L);
        repository.schedules.put(1L, new ReviewSchedule(1L, 3, LocalDate.of(2026, 10, 25), clock.instant(), 3));

        ReviewSchedule updated = scheduler.submitReview(1L, ReviewRating.DIFFICULT);
        assertEquals(2, updated.getStep()); // 3 - 1 = 2
        assertEquals(LocalDate.of(2026, 11, 1), updated.getNextReviewDate()); // today + 7
    }

    @Test
    public void submitReview_remembered_advancesStepBy1() {
        repository.addEligibleEntry(1L);
        repository.schedules.put(1L, new ReviewSchedule(1L, 2, LocalDate.of(2026, 10, 25), clock.instant(), 2));

        ReviewSchedule updated = scheduler.submitReview(1L, ReviewRating.REMEMBERED);
        assertEquals(3, updated.getStep()); // 2 + 1 = 3
        assertEquals(LocalDate.of(2026, 11, 8), updated.getNextReviewDate()); // today + 14
    }

    @Test
    public void submitReview_easy_advancesStepBy2() {
        repository.addEligibleEntry(1L);
        repository.schedules.put(1L, new ReviewSchedule(1L, 1, LocalDate.of(2026, 10, 25), clock.instant(), 1));

        ReviewSchedule updated = scheduler.submitReview(1L, ReviewRating.EASY);
        assertEquals(3, updated.getStep()); // 1 + 2 = 3
        assertEquals(LocalDate.of(2026, 11, 8), updated.getNextReviewDate()); // today + 14
    }

    @Test
    public void submitReview_clampsAtMaxLadderStep() {
        repository.addEligibleEntry(1L);
        // Step 6 is max (index 6 = 120 days)
        repository.schedules.put(1L, new ReviewSchedule(1L, 6, LocalDate.of(2026, 10, 25), clock.instant(), 6));

        ReviewSchedule updated = scheduler.submitReview(1L, ReviewRating.EASY);
        assertEquals(6, updated.getStep()); // clamped to max step 6
        assertEquals(LocalDate.of(2027, 2, 22), updated.getNextReviewDate()); // today + 120
    }

    // --- Amendment 1: Reviewed Local Date & Zone Shift Test ---

    @Test
    public void countReviewsForDate_reviewAt2330AndSubsequentZoneChange_countsForOriginalLocalDate() {
        repository.addEligibleEntry(1L);
        repository.addEligibleEntry(2L);

        // 2026-10-25 23:30 in UTC
        clock.setInstant(Instant.parse("2026-10-25T23:30:00Z"));
        currentZone.set(ZoneOffset.UTC);

        scheduler.submitReview(1L, ReviewRating.REMEMBERED);

        ReviewRecord record = repository.records.get(0);
        assertEquals(LocalDate.of(2026, 10, 25), record.getReviewedLocalDate());
        assertEquals(1, repository.countReviewsForDate(LocalDate.of(2026, 10, 25)));

        // User changes zone to Tokyo (UTC+9) after the review
        currentZone.set(ZoneId.of("Asia/Tokyo"));
        // The recorded review must still count for original local date Oct 25!
        assertEquals(1, repository.countReviewsForDate(LocalDate.of(2026, 10, 25)));
        assertEquals(0, repository.countReviewsForDate(LocalDate.of(2026, 10, 26)));
    }

    // --- Amendment 7 & Queue Ordering Tests ---

    @Test
    public void queue_unreviewedCard_countsAsDueNowAtStep0() {
        repository.addEligibleEntry(10L); // never reviewed

        List<ReviewQueueItem> queue = scheduler.getReviewQueue();
        assertEquals(1, queue.size());
        ReviewQueueItem item = queue.get(0);
        assertEquals(10L, item.getEntryId());
        assertEquals(0, item.getStep());
        assertEquals(0, item.getDaysOverdue());
        assertFalse(item.isPreviouslyReviewed());
    }

    @Test
    public void queue_ordering_daysOverdueDesc_thenPreviouslyReviewedBeforeNeverReviewed_thenStepAsc_thenEntryIdAsc() {
        // Entry 1: Genuinely overdue by 3 days (nextReviewDate = Oct 22), step 2, previously reviewed
        repository.addEligibleEntry(1L);
        repository.schedules.put(1L, new ReviewSchedule(1L, 2, LocalDate.of(2026, 10, 22), clock.instant(), 2));

        // Entry 2: Genuinely overdue by 1 day (nextReviewDate = Oct 24), step 1, previously reviewed
        repository.addEligibleEntry(2L);
        repository.schedules.put(2L, new ReviewSchedule(2L, 1, LocalDate.of(2026, 10, 24), clock.instant(), 1));

        // Entry 3: Due today (Oct 25, 0 days overdue), step 1, previously reviewed
        repository.addEligibleEntry(3L);
        repository.schedules.put(3L, new ReviewSchedule(3L, 1, LocalDate.of(2026, 10, 25), clock.instant(), 1));

        // Entry 4: Due today (Oct 25, 0 days overdue), step 0, previously reviewed
        repository.addEligibleEntry(4L);
        repository.schedules.put(4L, new ReviewSchedule(4L, 0, LocalDate.of(2026, 10, 25), clock.instant(), 1));

        // Entry 5: Never reviewed (0 days overdue, step 0, not previously reviewed), entryId 5
        repository.addEligibleEntry(5L);

        // Entry 6: Never reviewed (0 days overdue, step 0, not previously reviewed), entryId 6
        repository.addEligibleEntry(6L);

        List<ReviewQueueItem> queue = scheduler.getReviewQueue();
        assertEquals(6, queue.size());

        // Expected order:
        // 1st: Entry 1 (3 days overdue)
        // 2nd: Entry 2 (1 day overdue)
        // 3rd: Entry 4 (0 days overdue, previously reviewed, step 0)
        // 4th: Entry 3 (0 days overdue, previously reviewed, step 1)
        // 5th: Entry 5 (0 days overdue, never reviewed, step 0, entryId 5)
        // 6th: Entry 6 (0 days overdue, never reviewed, step 0, entryId 6)
        assertEquals(1L, queue.get(0).getEntryId());
        assertEquals(2L, queue.get(1).getEntryId());
        assertEquals(4L, queue.get(2).getEntryId());
        assertEquals(3L, queue.get(3).getEntryId());
        assertEquals(5L, queue.get(4).getEntryId());
        assertEquals(6L, queue.get(5).getEntryId());
    }

    // --- Amendment 3: Daily Limit Net of Reviews Completed Today ---

    @Test
    public void queue_dailyLimit_netOfReviewsAlreadyDoneToday() {
        ReviewConfig customConfig = new ReviewConfig(Arrays.asList(1, 3, 7), 3);
        ReviewScheduler customScheduler = new ReviewScheduler(repository, customConfig, clock, currentZone::get, transactionRunner);

        repository.addEligibleEntry(1L);
        repository.addEligibleEntry(2L);
        repository.addEligibleEntry(3L);
        repository.addEligibleEntry(4L);

        // Before any reviews today, limit is 3
        assertEquals(3, customScheduler.getReviewQueue().size());

        // User reviews entry 1 today
        customScheduler.submitReview(1L, ReviewRating.REMEMBERED);

        // Now completed today = 1, remaining queue size must be 2
        List<ReviewQueueItem> remainingQueue = customScheduler.getReviewQueue();
        assertEquals(2, remainingQueue.size());
    }

    @Test
    public void queue_dailyLimitReached_returnsEmptyQueue() {
        ReviewConfig customConfig = new ReviewConfig(Arrays.asList(1, 3, 7), 1);
        ReviewScheduler customScheduler = new ReviewScheduler(repository, customConfig, clock, currentZone::get, transactionRunner);

        repository.addEligibleEntry(1L);
        repository.addEligibleEntry(2L);

        customScheduler.submitReview(1L, ReviewRating.REMEMBERED);

        // Limit reached: remaining quota is 0
        assertTrue(customScheduler.getReviewQueue().isEmpty());
    }

    @Test
    public void queue_reviewsFromYesterday_doNotDecrementTodayLimit() {
        ReviewConfig customConfig = new ReviewConfig(Arrays.asList(1, 3, 7), 2);
        ReviewScheduler customScheduler = new ReviewScheduler(repository, customConfig, clock, currentZone::get, transactionRunner);

        repository.addEligibleEntry(1L);
        repository.addEligibleEntry(2L);

        // Record a review on yesterday (Oct 24)
        repository.records.add(new ReviewRecord(
                1, 1L, clock.instant().minusSeconds(86400),
                LocalDate.of(2026, 10, 24), ReviewRating.EASY,
                0, 0, null, 2, 7, LocalDate.of(2026, 10, 31)
        ));

        // Queue today must still allow 2 reviews
        assertEquals(2, customScheduler.getReviewQueue().size());
    }

    @Test
    public void queue_archivedAndTrashedEntries_excluded() {
        repository.addEligibleEntry(1L);
        repository.addArchivedEntry(2L);
        repository.addTrashedEntry(3L);

        // Even if scheduled for today, archived/trashed must not appear in queue
        repository.schedules.put(2L, new ReviewSchedule(2L, 0, LocalDate.of(2026, 10, 25), clock.instant(), 1));
        repository.schedules.put(3L, new ReviewSchedule(3L, 0, LocalDate.of(2026, 10, 25), clock.instant(), 1));

        List<ReviewQueueItem> queue = scheduler.getReviewQueue();
        assertEquals(1, queue.size());
        assertEquals(1L, queue.get(0).getEntryId());
    }

    @Test
    public void history_changingLadderConfig_doesNotRewritePastRecords() {
        repository.addEligibleEntry(1L);
        scheduler.submitReview(1L, ReviewRating.REMEMBERED);

        ReviewRecord pastRecord = repository.records.get(0);
        assertEquals(1, pastRecord.getNewStep());
        assertEquals(3, pastRecord.getNewIntervalDays());

        // Reconfigure ladder with different days
        ReviewConfig newConfig = new ReviewConfig(Arrays.asList(2, 5, 10, 20), 20);
        ReviewScheduler newScheduler = new ReviewScheduler(repository, newConfig, clock, currentZone::get, transactionRunner);

        // Past record in repository is untouched
        ReviewRecord recordAfter = repository.records.get(0);
        assertEquals(pastRecord, recordAfter);
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

    static class InMemoryReviewRepository implements ReviewRepository {
        final Map<Long, ReviewSchedule> schedules = new HashMap<>();
        final List<ReviewRecord> records = new ArrayList<>();
        final Set<Long> eligibleIds = new HashSet<>();
        final Set<Long> archivedIds = new HashSet<>();
        final Set<Long> trashedIds = new HashSet<>();

        void addEligibleEntry(long id) { eligibleIds.add(id); }
        void addArchivedEntry(long id) { archivedIds.add(id); }
        void addTrashedEntry(long id) { trashedIds.add(id); }

        @Override
        public Optional<ReviewSchedule> getScheduleForEntry(long entryId) {
            return Optional.ofNullable(schedules.get(entryId));
        }

        @Override
        public void saveSchedule(ReviewSchedule schedule) {
            schedules.put(schedule.getEntryId(), schedule);
        }

        @Override
        public void recordReview(ReviewRecord record) {
            records.add(record);
        }

        @Override
        public List<ReviewSchedule> getSchedulesDueOnOrBefore(LocalDate asOfDate) {
            List<ReviewSchedule> result = new ArrayList<>();
            for (ReviewSchedule s : schedules.values()) {
                if (!s.getNextReviewDate().isAfter(asOfDate)) {
                    result.add(s);
                }
            }
            return result;
        }

        @Override
        public int countReviewsForDate(LocalDate date) {
            int count = 0;
            for (ReviewRecord r : records) {
                if (r.getReviewedLocalDate().equals(date)) {
                    count++;
                }
            }
            return count;
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

        @Override
        public List<ReviewRecord> getHistoryForEntry(long entryId) {
            List<ReviewRecord> result = new ArrayList<>();
            for (ReviewRecord r : records) {
                if (r.getEntryId() == entryId) {
                    result.add(r);
                }
            }
            return result;
        }
    }
}
