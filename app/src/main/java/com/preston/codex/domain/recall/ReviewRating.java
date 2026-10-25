package com.preston.codex.domain.recall;

/**
 * Spaced repetition recall ratings per Decision 5 and Spec §8.2.
 */
public enum ReviewRating {
    /** Failed recall: resets step to 0; interval = ladder[0] (1 day). */
    FORGOT,
    /** Difficult recall: repeat at max(0, step - 1) ladder position. */
    DIFFICULT,
    /** Successful recall: advance 1 step (clamped to last ladder step). */
    REMEMBERED,
    /** Effortless recall: advance 2 steps (clamped to last ladder step). */
    EASY
}
