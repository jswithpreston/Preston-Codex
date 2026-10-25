package com.preston.codex.domain.scheduling;

import java.util.List;

/**
 * Strategy interface for selecting one insight ID from a non-empty list of candidate IDs.
 * Candidates are guaranteed to be sorted ascending by ID.
 */
@FunctionalInterface
public interface InsightSelector {

    /**
     * Selects one ID from the non-empty, sorted candidate list.
     *
     * @param candidates non-empty list of candidate IDs, sorted ascending
     * @return the selected entry ID
     */
    long select(List<Long> candidates);
}
