package com.preston.codex.domain.recall;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable configuration for spaced repetition review ladder and daily review limit.
 */
public final class ReviewConfig {

    public static final List<Integer> DEFAULT_LADDER = Collections.unmodifiableList(
            Arrays.asList(1, 3, 7, 14, 30, 60, 120)
    );
    public static final int DEFAULT_DAILY_LIMIT = 20;

    private final List<Integer> ladderDays;
    private final int dailyReviewLimit;

    public ReviewConfig() {
        this(DEFAULT_LADDER, DEFAULT_DAILY_LIMIT);
    }

    public ReviewConfig(List<Integer> ladderDays, int dailyReviewLimit) {
        if (ladderDays == null || ladderDays.isEmpty()) {
            throw new IllegalArgumentException("ladderDays must not be null or empty");
        }
        if (dailyReviewLimit < 0) {
            throw new IllegalArgumentException("dailyReviewLimit must not be negative");
        }
        this.ladderDays = Collections.unmodifiableList(new ArrayList<>(ladderDays));
        this.dailyReviewLimit = dailyReviewLimit;
    }

    public List<Integer> getLadderDays() {
        return ladderDays;
    }

    public int getDailyReviewLimit() {
        return dailyReviewLimit;
    }

    public int getMaxStep() {
        return ladderDays.size() - 1;
    }

    public int getIntervalForStep(int step) {
        int clamped = Math.max(0, Math.min(step, getMaxStep()));
        return ladderDays.get(clamped);
    }
}
