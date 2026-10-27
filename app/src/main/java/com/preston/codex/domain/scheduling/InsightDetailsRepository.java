package com.preston.codex.domain.scheduling;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Domain repository interface for reading canonical insight display details for a given date.
 * Pure Java, zero android.* imports.
 */
public interface InsightDetailsRepository {

    /**
     * Retrieves the display details for the insight selected for the specified date.
     * Returns empty if no selection exists, the library is empty, or the entry is no longer eligible.
     */
    Optional<InsightDetails> getDailyInsightDetails(LocalDate date);

    /**
     * Retrieves the display details for a specific entry ID, if it exists and is eligible.
     */
    Optional<InsightDetails> getInsightDetailsById(long entryId);
}
