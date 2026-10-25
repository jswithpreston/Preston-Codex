package com.preston.codex.domain.recall;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain model representing an entry's current spaced-repetition state.
 * Uses {@link LocalDate} in domain layer.
 */
public final class ReviewSchedule {

    private final long entryId;
    private final int step;
    private final LocalDate nextReviewDate;
    private final Instant lastReviewedAt;
    private final int reviewCount;

    public ReviewSchedule(long entryId, int step, LocalDate nextReviewDate, Instant lastReviewedAt, int reviewCount) {
        this.entryId = entryId;
        this.step = step;
        this.nextReviewDate = Objects.requireNonNull(nextReviewDate, "nextReviewDate must not be null");
        this.lastReviewedAt = Objects.requireNonNull(lastReviewedAt, "lastReviewedAt must not be null");
        this.reviewCount = reviewCount;
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

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewSchedule that = (ReviewSchedule) o;
        return entryId == that.entryId &&
                step == that.step &&
                reviewCount == that.reviewCount &&
                Objects.equals(nextReviewDate, that.nextReviewDate) &&
                Objects.equals(lastReviewedAt, that.lastReviewedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryId, step, nextReviewDate, lastReviewedAt, reviewCount);
    }

    @Override
    public String toString() {
        return "ReviewSchedule{" +
                "entryId=" + entryId +
                ", step=" + step +
                ", nextReviewDate=" + nextReviewDate +
                ", lastReviewedAt=" + lastReviewedAt +
                ", reviewCount=" + reviewCount +
                '}';
    }
}
