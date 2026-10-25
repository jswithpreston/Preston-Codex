package com.preston.codex.domain.scheduling;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Append-only audit log entry recording when an insight was selected,
 * either via initial automatic selection, shuffle, or invalidation replacement.
 */
public final class DailyInsightHistory {

    private final long id;
    private final LocalDate localDate;
    private final Long entryId;
    private final DailyInsightHistoryReason reason;
    private final Instant selectedAt;

    public DailyInsightHistory(long id, LocalDate localDate, Long entryId, DailyInsightHistoryReason reason, Instant selectedAt) {
        this.id = id;
        this.localDate = Objects.requireNonNull(localDate, "localDate must not be null");
        this.entryId = entryId;
        this.reason = Objects.requireNonNull(reason, "reason must not be null");
        this.selectedAt = Objects.requireNonNull(selectedAt, "selectedAt must not be null");
    }

    public long getId() {
        return id;
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

    public DailyInsightHistoryReason getReason() {
        return reason;
    }

    public Instant getSelectedAt() {
        return selectedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DailyInsightHistory that = (DailyInsightHistory) o;
        return id == that.id &&
                Objects.equals(localDate, that.localDate) &&
                Objects.equals(entryId, that.entryId) &&
                reason == that.reason &&
                Objects.equals(selectedAt, that.selectedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, localDate, entryId, reason, selectedAt);
    }

    @Override
    public String toString() {
        return "DailyInsightHistory{" +
                "id=" + id +
                ", localDate=" + localDate +
                ", entryId=" + entryId +
                ", reason=" + reason +
                ", selectedAt=" + selectedAt +
                '}';
    }
}
