package com.preston.codex.domain.scheduling;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for daily insight selection and audit history.
 * Operates purely with {@link LocalDate} and domain models without android.* imports.
 */
public interface DailyInsightRepository {

    /**
     * Retrieves the canonical selection for the given local date, if any.
     */
    Optional<DailyInsightSelection> getSelectionForDate(LocalDate date);

    /**
     * Attempts to insert a new selection record. If a record already exists for the date,
     * returns false (insert-or-ignore semantics for thread safety).
     */
    boolean insertOrIgnoreSelection(DailyInsightSelection selection);

    /**
     * Updates an existing selection record for its date.
     */
    void updateSelection(DailyInsightSelection selection);

    /**
     * Appends a record to the daily insight history audit log.
     */
    void recordHistory(DailyInsightHistory history);

    /**
     * Retrieves all history records for a specific date, ordered chronologically.
     */
    List<DailyInsightHistory> getHistoryForDate(LocalDate date);

    /**
     * Finds the most recent canonical selection record before the given date.
     */
    Optional<DailyInsightSelection> getMostRecentPriorSelection(LocalDate beforeDate);

    /**
     * Retrieves IDs of all eligible entries (not archived, not trashed), sorted ascending.
     */
    List<Long> getEligibleEntryIds();

    /**
     * Checks if a specific entry ID exists and is eligible (not archived, not trashed).
     */
    boolean isEntryEligible(long entryId);
}
