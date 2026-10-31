package com.preston.codex.domain.tasks.backlog;

import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskPriority;

import java.time.LocalDate;
import java.util.Objects;

public record BacklogCandidate(
        long id,
        TaskKind kind, // ONE_OFF or SERIES
        String title,
        TaskPriority priority,
        LocalDate originalDate, // scheduledDate or effectiveDate
        LocalDate deadline,
        int durationMinutes,
        int autoRescheduleCount,
        boolean isFlexible
) {
    public BacklogCandidate {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(priority, "priority must not be null");
        Objects.requireNonNull(originalDate, "originalDate must not be null");
    }
}
