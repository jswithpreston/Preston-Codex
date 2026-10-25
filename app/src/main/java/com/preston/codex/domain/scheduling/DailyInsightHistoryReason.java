package com.preston.codex.domain.scheduling;

/**
 * Audit reason for a record in daily insight history.
 */
public enum DailyInsightHistoryReason {
    /** Initial automatic daily selection for the local date. */
    AUTO,
    /** Selection resulting from a manual user shuffle. */
    SHUFFLE,
    /** Selection resulting from an invalidation (e.g. entry deleted/archived). */
    INVALIDATED
}
