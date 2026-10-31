package com.preston.codex.domain.tasks.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public record Task(
        long id,
        TaskKind taskKind,
        String title,
        String description,
        TaskStatus status,
        Long previousTaskId,
        Long knowledgeEntryId,
        String reflection,
        boolean isFlexible,
        TaskPriority priority,
        LocalDate deadline,
        Integer estimatedDurationMinutes,
        LocalDate scheduledDate,
        LocalTime scheduledTime,
        int autoRescheduleCount,
        Instant completedAt,
        Instant cancelledAt,
        Instant createdAt,
        Instant updatedAt,
        Set<String> tags
) {
    public Task {
        Objects.requireNonNull(taskKind, "taskKind must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        tags = tags == null ? Collections.emptySet() : Collections.unmodifiableSet(tags);
    }
}
