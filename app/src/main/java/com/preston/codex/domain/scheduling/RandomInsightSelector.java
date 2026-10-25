package com.preston.codex.domain.scheduling;

import java.util.List;
import java.util.Objects;
import java.util.Random;

/**
 * Production implementation of {@link InsightSelector} providing a uniform random
 * selection over the sorted candidate list.
 */
public final class RandomInsightSelector implements InsightSelector {

    private final Random random;

    public RandomInsightSelector(Random random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    @Override
    public long select(List<Long> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates list must not be null or empty");
        }
        int index = random.nextInt(candidates.size());
        return candidates.get(index);
    }
}
