package com.preston.codex.domain.tasks.backlog;

import java.util.Objects;

public record SkippedMove(
        BacklogMoveProposal proposal,
        String reason
) {
    public SkippedMove {
        Objects.requireNonNull(proposal, "proposal must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
    }
}
