package com.preston.codex.domain.tasks.backlog;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public record BacklogReviewReport(
        List<BacklogMoveProposal> appliedMoves,
        List<SkippedMove> skippedMoves
) {
    public BacklogReviewReport {
        appliedMoves = appliedMoves == null ? Collections.emptyList() : Collections.unmodifiableList(appliedMoves);
        skippedMoves = skippedMoves == null ? Collections.emptyList() : Collections.unmodifiableList(skippedMoves);
    }
}
