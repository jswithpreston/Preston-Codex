package com.preston.codex.domain.recall;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Item in the active recall review queue, with fields necessary for deterministic sorting.
 */
public final class ReviewQueueItem {

    private final long entryId;
    private final int step;
    private final LocalDate nextReviewDate;
    private final long daysOverdue;
    private final boolean previouslyReviewed;

    public ReviewQueueItem(
            long entryId,
            int step,
            LocalDate nextReviewDate,
            long daysOverdue,
            boolean previouslyReviewed
    ) {
        this.entryId = entryId;
        this.step = step;
        this.nextReviewDate = Objects.requireNonNull(nextReviewDate, "nextReviewDate must not be null");
        this.daysOverdue = daysOverdue;
        this.previouslyReviewed = previouslyReviewed;
    }

    public long getEntryId() {
        return entryId;
    }

    public int getStep() {
        return step;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    public long getDaysOverdue() {
        return daysOverdue;
    }

    public boolean isPreviouslyReviewed() {
        return previouslyReviewed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewQueueItem that = (ReviewQueueItem) o;
        return entryId == that.entryId &&
                step == that.step &&
                daysOverdue == that.daysOverdue &&
                previouslyReviewed == that.previouslyReviewed &&
                Objects.equals(nextReviewDate, that.nextReviewDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryId, step, nextReviewDate, daysOverdue, previouslyReviewed);
    }

    @Override
    public String toString() {
        return "ReviewQueueItem{" +
                "entryId=" + entryId +
                ", step=" + step +
                ", nextReviewDate=" + nextReviewDate +
                ", daysOverdue=" + daysOverdue +
                ", previouslyReviewed=" + previouslyReviewed +
                '}';
    }
}
