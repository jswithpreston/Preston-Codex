package com.preston.codex.domain.tasks.state;

import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskStatus;

import java.time.LocalDate;
import java.util.Objects;

public record TaskStateInput(
        TaskStatus status,
        LocalDate date,
        LocalDate today,
        boolean hasRescheduleHistory,
        TaskKind kind
) {
    public TaskStateInput {
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(today, "today must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
    }
}
