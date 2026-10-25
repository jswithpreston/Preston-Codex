package com.preston.codex.domain.recall;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable audit record capturing the outcome of a review assessment.
 * Uses {@link LocalDate} in domain layer.
 */
public final class ReviewRecord {

    private final long id;
    private final long entryId;
    private final Instant reviewedAt;
    private final LocalDate reviewedLocalDate;
    private final ReviewRating rating;
    private final int previousStep;
    private final int previousIntervalDays;
    private final LocalDate previousNextReviewDate;
    private final int newStep;
    private final int newIntervalDays;
    private final LocalDate nextReviewDate;

    public ReviewRecord(
            long id,
            long entryId,
            Instant reviewedAt,
            LocalDate reviewedLocalDate,
            ReviewRating rating,
            int previousStep,
            int previousIntervalDays,
            LocalDate previousNextReviewDate,
            int newStep,
            int newIntervalDays,
            LocalDate nextReviewDate
    ) {
        this.id = id;
        this.entryId = entryId;
        this.reviewedAt = Objects.requireNonNull(reviewedAt, "reviewedAt must not be null");
        this.reviewedLocalDate = Objects.requireNonNull(reviewedLocalDate, "reviewedLocalDate must not be null");
        this.rating = Objects.requireNonNull(rating, "rating must not be null");
        this.previousStep = previousStep;
        this.previousIntervalDays = previousIntervalDays;
        this.previousNextReviewDate = previousNextReviewDate;
        this.newStep = newStep;
        this.newIntervalDays = newIntervalDays;
        this.nextReviewDate = Objects.requireNonNull(nextReviewDate, "nextReviewDate must not be null");
    }

    public long getId() {
        return id;
    }

    public long getEntryId() {
        return entryId;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public LocalDate getReviewedLocalDate() {
        return reviewedLocalDate;
    }

    public ReviewRating getRating() {
        return rating;
    }

    public int getPreviousStep() {
        return previousStep;
    }

    public int getPreviousIntervalDays() {
        return previousIntervalDays;
    }

    public Optional<LocalDate> getPreviousNextReviewDate() {
        return Optional.ofNullable(previousNextReviewDate);
    }

    public LocalDate getRawPreviousNextReviewDate() {
        return previousNextReviewDate;
    }

    public int getNewStep() {
        return newStep;
    }

    public int getNewIntervalDays() {
        return newIntervalDays;
    }

    public LocalDate getNextReviewDate() {
        return nextReviewDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReviewRecord that = (ReviewRecord) o;
        return id == that.id &&
                entryId == that.entryId &&
                previousStep == that.previousStep &&
                previousIntervalDays == that.previousIntervalDays &&
                newStep == that.newStep &&
                newIntervalDays == that.newIntervalDays &&
                Objects.equals(reviewedAt, that.reviewedAt) &&
                Objects.equals(reviewedLocalDate, that.reviewedLocalDate) &&
                rating == that.rating &&
                Objects.equals(previousNextReviewDate, that.previousNextReviewDate) &&
                Objects.equals(nextReviewDate, that.nextReviewDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, entryId, reviewedAt, reviewedLocalDate, rating,
                previousStep, previousIntervalDays, previousNextReviewDate,
                newStep, newIntervalDays, nextReviewDate);
    }

    @Override
    public String toString() {
        return "ReviewRecord{" +
                "id=" + id +
                ", entryId=" + entryId +
                ", reviewedAt=" + reviewedAt +
                ", reviewedLocalDate=" + reviewedLocalDate +
                ", rating=" + rating +
                ", previousStep=" + previousStep +
                ", previousIntervalDays=" + previousIntervalDays +
                ", previousNextReviewDate=" + previousNextReviewDate +
                ", newStep=" + newStep +
                ", newIntervalDays=" + newIntervalDays +
                ", nextReviewDate=" + nextReviewDate +
                '}';
    }
}
