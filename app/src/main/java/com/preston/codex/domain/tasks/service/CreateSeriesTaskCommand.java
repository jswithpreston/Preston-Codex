package com.preston.codex.domain.tasks.service;

import com.preston.codex.domain.tasks.model.TaskPriority;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public record CreateSeriesTaskCommand(
        String title,
        String description,
        TaskPriority priority,
        boolean isFlexible,
        Long knowledgeEntryId,
        String reflection,
        Set<String> tags
) {
    public CreateSeriesTaskCommand {
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        tags = tags == null ? Collections.emptySet() : Collections.unmodifiableSet(tags);
    }
}
