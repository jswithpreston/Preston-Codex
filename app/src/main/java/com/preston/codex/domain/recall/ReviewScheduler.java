package com.preston.codex.domain.recall;

import com.preston.codex.domain.TransactionRunner;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Domain engine for spaced repetition review transitions, history logging,
 * and deterministic review queue evaluation.
 * Pure Java, no android.* dependencies.
 */
public final class ReviewScheduler {

    private final ReviewRepository repository;
    private final ReviewConfig config;
    private final Clock clock;
    private final Supplier<ZoneId> zoneSupplier;
    private final TransactionRunner transactionRunner;

    public ReviewScheduler(
            ReviewRepository repository,
            ReviewConfig config,
            Clock clock,
            Supplier<ZoneId> zoneSupplier,
            TransactionRunner transactionRunner
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.zoneSupplier = Objects.requireNonNull(zoneSupplier, "zoneSupplier must not be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
    }

    /**
     * Submits a recall assessment for an eligible entry.
     * Computes next review date, updates schedule, and writes an immutable review record.
     */
    public ReviewSchedule submitReview(long entryId, ReviewRating rating) {
        Objects.requireNonNull(rating, "rating must not be null");
        return transactionRunner.runInTransaction(() -> {
            if (!repository.isEntryEligible(entryId)) {
                throw new IllegalArgumentException("Entry " + entryId + " does not exist or is not eligible for review");
            }

            LocalDate today = LocalDate.now(clock.withZone(zoneSupplier.get()));
            Instant now = clock.instant();

            Optional<ReviewSchedule> currentOpt = repository.getScheduleForEntry(entryId);
            int prevStep;
            int prevInterval;
            LocalDate prevNextReviewDate;
            int reviewCount;

            if (currentOpt.isPresent()) {
                ReviewSchedule prev = currentOpt.get();
                prevStep = prev.getStep();
                prevInterval = config.getIntervalForStep(prevStep);
                prevNextReviewDate = prev.getNextReviewDate();
                reviewCount = prev.getReviewCount() + 1;
            } else {
                // First review of never-reviewed entry
                prevStep = 0;
                prevInterval = 0;
                prevNextReviewDate = null;
                reviewCount = 1;
            }

            int newStep;
            switch (rating) {
                case FORGOT:
                    newStep = 0;
                    break;
                case DIFFICULT:
                    newStep = Math.max(0, prevStep - 1);
                    break;
                case REMEMBERED:
                    newStep = Math.min(config.getMaxStep(), prevStep + 1);
                    break;
                case EASY:
                    newStep = Math.min(config.getMaxStep(), prevStep + 2);
                    break;
                default:
                    throw new IllegalStateException("Unexpected rating: " + rating);
            }

            int newInterval = config.getIntervalForStep(newStep);
            LocalDate nextReviewDate = today.plusDays(newInterval);

            ReviewSchedule updatedSchedule = new ReviewSchedule(entryId, newStep, nextReviewDate, now, reviewCount);
            repository.saveSchedule(updatedSchedule);

            ReviewRecord record = new ReviewRecord(
                    0,
                    entryId,
                    now,
                    today,
                    rating,
                    prevStep,
                    prevInterval,
                    prevNextReviewDate,
                    newStep,
                    newInterval,
                    nextReviewDate
            );
            repository.recordReview(record);

            return updatedSchedule;
        });
    }

    /**
     * Retrieves the due review queue for the current local date, sorted deterministically
     * and truncated to remaining daily review limit.
     */
    public List<ReviewQueueItem> getReviewQueue() {
        return transactionRunner.runInTransaction(() -> {
            LocalDate today = LocalDate.now(clock.withZone(zoneSupplier.get()));

            int completedToday = repository.countReviewsForDate(today);
            int remainingLimit = Math.max(0, config.getDailyReviewLimit() - completedToday);
            if (remainingLimit == 0) {
                return Collections.emptyList();
            }

            List<Long> eligibleList = repository.getEligibleEntryIds();
            Set<Long> eligibleSet = new HashSet<>(eligibleList);

            List<ReviewSchedule> dueSchedules = repository.getSchedulesDueOnOrBefore(today);
            Map<Long, ReviewSchedule> scheduleMap = new HashMap<>();
            List<ReviewQueueItem> queueItems = new ArrayList<>();

            for (ReviewSchedule schedule : dueSchedules) {
                long entryId = schedule.getEntryId();
                if (eligibleSet.contains(entryId)) {
                    scheduleMap.put(entryId, schedule);
                    long daysOverdue = ChronoUnit.DAYS.between(schedule.getNextReviewDate(), today);
                    queueItems.add(new ReviewQueueItem(entryId, schedule.getStep(), schedule.getNextReviewDate(), daysOverdue, true));
                }
            }

            for (Long entryId : eligibleList) {
                if (!scheduleMap.containsKey(entryId)) {
                    // Unreviewed entry counts as due now at step 0, 0 days overdue, previouslyReviewed = false
                    queueItems.add(new ReviewQueueItem(entryId, 0, today, 0, false));
                }
            }

            // Comparator:
            // 1. daysOverdue DESC
            // 2. previouslyReviewed before neverReviewed
            // 3. step ASC
            // 4. entryId ASC
            Comparator<ReviewQueueItem> comparator = (a, b) -> {
                int cmpOverdue = Long.compare(b.getDaysOverdue(), a.getDaysOverdue());
                if (cmpOverdue != 0) return cmpOverdue;

                int cmpPrev = Boolean.compare(b.isPreviouslyReviewed(), a.isPreviouslyReviewed());
                if (cmpPrev != 0) return cmpPrev;

                int cmpStep = Integer.compare(a.getStep(), b.getStep());
                if (cmpStep != 0) return cmpStep;

                return Long.compare(a.getEntryId(), b.getEntryId());
            };

            Collections.sort(queueItems, comparator);

            if (queueItems.size() > remainingLimit) {
                return new ArrayList<>(queueItems.subList(0, remainingLimit));
            }
            return queueItems;
        });
    }
}
