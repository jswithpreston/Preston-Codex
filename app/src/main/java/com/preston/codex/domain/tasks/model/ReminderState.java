package com.preston.codex.domain.tasks.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

public record ReminderState(
        long id,
        String reminderType,
        String ownerType,
        long ownerId,
        Instant triggerAt,
        LocalDate triggerLocalDate,
        LocalTime triggerLocalTime,
        String channel,
        String status,
        Instant firedAt,
        String idempotencyKey,
        Instant createdAt
) {
    public ReminderState {
        Objects.requireNonNull(reminderType, "reminderType must not be null");
        Objects.requireNonNull(ownerType, "ownerType must not be null");
        Objects.requireNonNull(triggerAt, "triggerAt must not be null");
        Objects.requireNonNull(triggerLocalDate, "triggerLocalDate must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(idempotencyKey, "idempotencyKey must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
