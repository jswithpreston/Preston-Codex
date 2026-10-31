package com.preston.codex.domain.tasks.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public record TaskOccurrence(
        long id,
        long taskId,
        long ruleId,
        LocalDate occurrenceDate,
        LocalDate effectiveDate,
        OccurrenceStatus status,
        String titleOverride,
        LocalTime timeOverride,
        String noteOverride,
        String reflection,
        int autoRescheduleCount,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt
) {
    public TaskOccurrence {
        Objects.requireNonNull(occurrenceDate, "occurrenceDate must not be null");
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
