package com.preston.codex.domain.scheduling;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Canonical daily insight selection for a specific local date.
 * Uses {@link LocalDate} in domain layer.
 */
public final class DailyInsightSelection {

    private final LocalDate localDate;
    private final Long entryId;
    private final Instant selectedAt;
    private final boolean wasShuffled;

    public DailyInsightSelection(LocalDate localDate, Long entryId, Instant selectedAt, boolean wasShuffled) {
        this.localDate = Objects.requireNonNull(localDate, "localDate must not be null");
        this.entryId = entryId;
        this.selectedAt = Objects.requireNonNull(selectedAt, "selectedAt must not be null");
        this.wasShuffled = wasShuffled;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public Optional<Long> getEntryId() {
        return Optional.ofNullable(entryId);
    }

    public Long getRawEntryId() {
        return entryId;
    }

    public Instant getSelectedAt() {
        return selectedAt;
    }

    public boolean isWasShuffled() {
        return wasShuffled;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyInsightSelection that = (DailyInsightSelection) o;
        return wasShuffled == that.wasShuffled &&
                Objects.equals(localDate, that.localDate) &&
                Objects.equals(entryId, that.entryId) &&
                Objects.equals(selectedAt, that.selectedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localDate, entryId, selectedAt, wasShuffled);
    }

    @Override
    public String toString() {
        return "DailyInsightSelection{" +
                "localDate=" + localDate +
                ", entryId=" + entryId +
                ", selectedAt=" + selectedAt +
                ", wasShuffled=" + wasShuffled +
                '}';
    }
}
