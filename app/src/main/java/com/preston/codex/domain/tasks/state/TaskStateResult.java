package com.preston.codex.domain.tasks.state;

import com.preston.codex.domain.tasks.model.DerivedTaskState;

import java.util.Objects;

public record TaskStateResult(
        DerivedTaskState state,
        boolean isOverdue
) {
    public TaskStateResult {
        Objects.requireNonNull(state, "state must not be null");
    }
}
