package com.preston.codex.domain.scheduling;

import java.util.Objects;
import java.util.Optional;

/**
 * Result of a shuffle operation including the status code and selected entry ID.
 */
public final class ShuffleOutcome {

    private final ShuffleResult result;
    private final Long selectedEntryId;

    public ShuffleOutcome(ShuffleResult result, Long selectedEntryId) {
        this.result = Objects.requireNonNull(result, "result must not be null");
        this.selectedEntryId = selectedEntryId;
    }

    public ShuffleResult getResult() {
        return result;
    }

    public Optional<Long> getSelectedEntryId() {
        return Optional.ofNullable(selectedEntryId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShuffleOutcome that = (ShuffleOutcome) o;
        return result == that.result && Objects.equals(selectedEntryId, that.selectedEntryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(result, selectedEntryId);
    }

    @Override
    public String toString() {
        return "ShuffleOutcome{" +
                "result=" + result +
                ", selectedEntryId=" + selectedEntryId +
                '}';
    }
}
