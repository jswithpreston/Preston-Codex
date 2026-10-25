package com.preston.codex.domain.recall;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Domain repository interface for review schedules, review history records,
 * and review queue queries. Pure Java.
 */
public interface ReviewRepository {

    /**
     * Finds the current schedule state for a knowledge entry.
     */
    Optional<ReviewSchedule> getScheduleForEntry(long entryId);

    /**
     * Saves or updates the schedule state for a knowledge entry.
     */
    void saveSchedule(ReviewSchedule schedule);

    /**
     * Appends an immutable review record to review history.
     */
    void recordReview(ReviewRecord record);

    /**
     * Retrieves all review schedules whose nextReviewDate is on or before asOfDate.
     */
    List<ReviewSchedule> getSchedulesDueOnOrBefore(LocalDate asOfDate);

    /**
     * Counts the number of reviews recorded for a specific local date.
     */
    int countReviewsForDate(LocalDate date);

    /**
     * Retrieves all eligible entry IDs (not archived, not trashed), sorted ascending.
     */
    List<Long> getEligibleEntryIds();

    /**
     * Checks if a specific entry ID exists and is eligible (not archived, not trashed).
     */
    boolean isEntryEligible(long entryId);

    /**
     * Retrieves all historical review records for a specific entry, chronologically ordered.
     */
    List<ReviewRecord> getHistoryForEntry(long entryId);
}
