package com.preston.codex.domain.tasks.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record RescheduleRecord(
        long id,
        long taskId,
        Long occurrenceId,
        LocalDate fromDate,
        LocalDate toDate,
        String reason,
        Instant timestamp
) {
    public RescheduleRecord {
        Objects.requireNonNull(toDate, "toDate must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
