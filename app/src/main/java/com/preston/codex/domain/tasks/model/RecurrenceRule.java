package com.preston.codex.domain.tasks.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record RecurrenceRule(
        long id,
        long taskId,
        RecurrenceType type,
        int interval,
        String weekdays,
        LocalDate startDate,
        LocalDate endDate,
        Integer occurrenceCount,
        Integer anchorDayOfMonth,
        MissedPolicy missedPolicy,
        Instant createdAt,
        Instant updatedAt
) {
    public RecurrenceRule {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(missedPolicy, "missedPolicy must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
