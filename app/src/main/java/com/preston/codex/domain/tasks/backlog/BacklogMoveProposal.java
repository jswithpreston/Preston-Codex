package com.preston.codex.domain.tasks.backlog;

import com.preston.codex.domain.tasks.model.TaskKind;

import java.time.LocalDate;
import java.util.Objects;

public record BacklogMoveProposal(
        long id,
        TaskKind kind,
        String title,
        LocalDate fromDate,
        LocalDate toDate,
        int durationMinutes
) {
    public BacklogMoveProposal {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(title, "title must not be null");
        Objects.requireNonNull(fromDate, "fromDate must not be null");
        Objects.requireNonNull(toDate, "toDate must not be null");
    }
}
