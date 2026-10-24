package com.preston.codex.domain;

import org.junit.Test;

import java.time.Clock;
import java.time.Instant;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Phase 0 placeholder — verifies JVM test infrastructure and that
 * {@link Clock} (the injection interface used throughout domain code)
 * is functional. Contains zero {@code android.*} imports.
 */
public class ClockTest {

    @Test
    public void systemClock_returnsNonNullInstant() {
        Clock clock = Clock.systemUTC();
        Instant now = clock.instant();
        assertNotNull("Clock.instant() must not be null", now);
    }

    @Test
    public void fixedClock_returnsExpectedInstant() {
        Instant expected = Instant.parse("2024-01-15T09:00:00Z");
        Clock fixed = Clock.fixed(expected, java.time.ZoneOffset.UTC);
        Instant actual = fixed.instant();
        assertTrue("Fixed clock must return the pinned instant",
                expected.equals(actual));
    }
}
