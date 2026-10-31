package com.preston.codex.domain.tasks.service;

import com.preston.codex.domain.tasks.model.TaskPriority;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public record CreateOneOffTaskCommand(
        String title,
        String description,
        TaskPriority priority,
        LocalDate deadline,
        Integer estimatedDurationMinutes,
        LocalDate scheduledDate,
        LocalTime scheduledTime,
        boolean isFlexible,
        Long knowledgeEntryId,
        String reflection,
        Set<String> tags
) {
    public CreateOneOffTaskCommand {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        tags = tags == null ? Collections.emptySet() : Collections.unmodifiableSet(tags);
    }
}
