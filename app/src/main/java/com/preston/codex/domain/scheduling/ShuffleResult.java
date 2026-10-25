package com.preston.codex.domain.scheduling;

/**
 * Outcome category of a shuffle operation.
 */
public enum ShuffleResult {
    /** A different eligible entry was selected and saved. */
    SHUFFLED,
    /** Exactly 1 eligible entry exists in the library, so no alternative exists. */
    NO_ALTERNATIVE,
    /** Zero eligible entries exist in the library. */
    EMPTY
}
