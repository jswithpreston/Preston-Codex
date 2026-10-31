package com.preston.codex.domain.tasks.model;

import java.time.Instant;
import java.util.Objects;

public record TaskEvent(
        long id,
        long taskId,
        Long occurrenceId,
        String eventType,
        String details,
        Instant timestamp
) {
    public TaskEvent {
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
