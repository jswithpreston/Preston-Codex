package com.preston.codex.domain.tasks.backlog;

import java.util.Collections;
import java.util.List;

public record BacklogReviewPreview(
        List<BacklogMoveProposal> proposedMoves,
        List<BacklogCandidate> needsAttention
) {
    public BacklogReviewPreview {
        proposedMoves = proposedMoves == null ? Collections.emptyList() : Collections.unmodifiableList(proposedMoves);
        needsAttention = needsAttention == null ? Collections.emptyList() : Collections.unmodifiableList(needsAttention);
    }
}
