package com.preston.codex.domain.scheduling;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * Pure Java utility for computing next run timestamps and delays for WorkManager scheduling.
 * Handles daylight saving transitions (23h and 25h days), leap years, and midnight skips.
 * Zero android.* dependencies.
 */
public final class NextRunCalculator {

    private NextRunCalculator() {
    }

    /**
     * Calculates the next occurrence of {@code targetTime} strictly after {@code now}
     * in the specified {@code zoneId}.
     */
    public static ZonedDateTime calculateNextRun(ZonedDateTime now, LocalTime targetTime, ZoneId zoneId) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(targetTime, "targetTime must not be null");
        Objects.requireNonNull(zoneId, "zoneId must not be null");

        ZonedDateTime localNow = now.withZoneSameInstant(zoneId);
        LocalDate today = localNow.toLocalDate();
        ZonedDateTime targetToday = today.atTime(targetTime).atZone(zoneId);

        if (targetToday.isAfter(localNow)) {
            return targetToday;
        }

        LocalDate tomorrow = today.plusDays(1);
        return tomorrow.atTime(targetTime).atZone(zoneId);
    }

    /**
     * Calculates the non-negative duration from {@code now} until the next occurrence of {@code targetTime}.
     */
    public static Duration calculateDelay(ZonedDateTime now, LocalTime targetTime, ZoneId zoneId) {
        ZonedDateTime nextRun = calculateNextRun(now, targetTime, zoneId);
        Duration delay = Duration.between(now, nextRun);
        return delay.isNegative() ? Duration.ZERO : delay;
    }

    /**
     * Calculates the next midnight (5 seconds past start of next calendar day)
     * using {@code date.atStartOfDay(zone).plusSeconds(5)}.
     * This safely handles timezones where DST jumps skip 00:00:00 (e.g. America/Havana).
     */
    public static ZonedDateTime calculateNextMidnight(ZonedDateTime now, ZoneId zoneId) {
        Objects.requireNonNull(now, "now must not be null");
        Objects.requireNonNull(zoneId, "zoneId must not be null");

        ZonedDateTime localNow = now.withZoneSameInstant(zoneId);
        LocalDate nextDate = localNow.toLocalDate().plusDays(1);
        return nextDate.atStartOfDay(zoneId).plusSeconds(5);
    }

    /**
     * Calculates the duration from {@code now} until the next midnight (+5s).
     */
    public static Duration calculateDelayUntilMidnight(ZonedDateTime now, ZoneId zoneId) {
        ZonedDateTime nextMidnight = calculateNextMidnight(now, zoneId);
        Duration delay = Duration.between(now, nextMidnight);
        return delay.isNegative() ? Duration.ZERO : delay;
    }
}
