package com.preston.codex.domain.scheduling;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class NextRunCalculatorTest {

    @Test
    public void normalDay_targetInFuture_delayIsExactDifference() {
        ZoneId zone = ZoneId.of("America/New_York");
        ZonedDateTime now = ZonedDateTime.of(2026, 6, 15, 7, 0, 0, 0, zone);
        LocalTime target = LocalTime.of(8, 0);

        ZonedDateTime nextRun = NextRunCalculator.calculateNextRun(now, target, zone);
        Duration delay = NextRunCalculator.calculateDelay(now, target, zone);

        assertEquals(ZonedDateTime.of(2026, 6, 15, 8, 0, 0, 0, zone), nextRun);
        assertEquals(Duration.ofHours(1), delay);
    }

    @Test
    public void normalDay_targetAlreadyPassed_delayIsUntilTomorrowTarget() {
        ZoneId zone = ZoneId.of("America/New_York");
        ZonedDateTime now = ZonedDateTime.of(2026, 6, 15, 9, 30, 0, 0, zone);
        LocalTime target = LocalTime.of(8, 0);

        ZonedDateTime nextRun = NextRunCalculator.calculateNextRun(now, target, zone);
        Duration delay = NextRunCalculator.calculateDelay(now, target, zone);

        assertEquals(ZonedDateTime.of(2026, 6, 16, 8, 0, 0, 0, zone), nextRun);
        assertEquals(Duration.ofHours(22).plusMinutes(30), delay);
    }

    @Test
    public void springForwardDst_23HourDay_correctDuration() {
        // America/New_York spring-forward in 2026 is Sunday, March 8: 2:00 AM -> 3:00 AM
        ZoneId zone = ZoneId.of("America/New_York");
        ZonedDateTime now = ZonedDateTime.of(2026, 3, 7, 20, 0, 0, 0, zone);
        LocalTime target = LocalTime.of(8, 0);

        ZonedDateTime nextRun = NextRunCalculator.calculateNextRun(now, target, zone);
        Duration delay = NextRunCalculator.calculateDelay(now, target, zone);

        assertEquals(ZonedDateTime.of(2026, 3, 8, 8, 0, 0, 0, zone), nextRun);
        // Normally 20:00 to 08:00 is 12 hours, but clocks spring forward 1 hour -> 11 hours
        assertEquals(Duration.ofHours(11), delay);
    }

    @Test
    public void fallBackDst_25HourDay_correctDuration() {
        // America/New_York fall-back in 2026 is Sunday, November 1: 2:00 AM -> 1:00 AM
        ZoneId zone = ZoneId.of("America/New_York");
        ZonedDateTime now = ZonedDateTime.of(2026, 10, 31, 20, 0, 0, 0, zone);
        LocalTime target = LocalTime.of(8, 0);

        ZonedDateTime nextRun = NextRunCalculator.calculateNextRun(now, target, zone);
        Duration delay = NextRunCalculator.calculateDelay(now, target, zone);

        assertEquals(ZonedDateTime.of(2026, 11, 1, 8, 0, 0, 0, zone), nextRun);
        // Normally 20:00 to 08:00 is 12 hours, but clocks fall back 1 hour -> 13 hours
        assertEquals(Duration.ofHours(13), delay);
    }

    @Test
    public void havanaMidnightDstJump_handledSafely() {
        // Cuba/Havana historically transitions DST at midnight (00:00 jumps to 01:00)
        // e.g. March 8, 2026 or March 14, 2021
        ZoneId zone = ZoneId.of("America/Havana");
        // Check midnight calculation using atStartOfDay
        ZonedDateTime now = ZonedDateTime.of(2021, 3, 13, 23, 0, 0, 0, zone);
        ZonedDateTime nextMidnight = NextRunCalculator.calculateNextMidnight(now, zone);

        assertTrue(nextMidnight.isAfter(now));
        // Next date start of day in Havana on jump day is 01:00
        assertEquals(LocalDate.of(2021, 3, 14), nextMidnight.toLocalDate());
        assertEquals(5, nextMidnight.getSecond());
        Duration delay = NextRunCalculator.calculateDelayUntilMidnight(now, zone);
        assertTrue(delay.toMillis() > 0);
    }

    @Test
    public void timezoneChange_recomputesAccurately() {
        // 14:00 UTC = 10:00 EDT (New York) and 07:00 PDT (Los Angeles)
        ZonedDateTime utcNow = ZonedDateTime.of(2026, 6, 15, 14, 0, 0, 0, ZoneId.of("UTC"));
        LocalTime target = LocalTime.of(9, 0);

        // In NY (10:00 EDT), 9:00 has passed today -> schedules for tomorrow 9:00 EDT
        ZoneId nyZone = ZoneId.of("America/New_York");
        ZonedDateTime nyNext = NextRunCalculator.calculateNextRun(utcNow.withZoneSameInstant(nyZone), target, nyZone);
        assertEquals(LocalDate.of(2026, 6, 16), nyNext.toLocalDate());

        // In LA (07:00 PDT), 9:00 is still ahead today -> schedules for today 9:00 PDT
        ZoneId laZone = ZoneId.of("America/Los_Angeles");
        ZonedDateTime laNext = NextRunCalculator.calculateNextRun(utcNow.withZoneSameInstant(laZone), target, laZone);
        assertEquals(LocalDate.of(2026, 6, 15), laNext.toLocalDate());
        assertEquals(Duration.ofHours(2), Duration.between(utcNow.withZoneSameInstant(laZone), laNext));
    }

    @Test
    public void targetTimeExactlyNow_schedulesForTomorrow() {
        ZoneId zone = ZoneId.of("UTC");
        ZonedDateTime now = ZonedDateTime.of(2026, 6, 15, 8, 0, 0, 0, zone);
        LocalTime target = LocalTime.of(8, 0);

        ZonedDateTime nextRun = NextRunCalculator.calculateNextRun(now, target, zone);
        assertEquals(ZonedDateTime.of(2026, 6, 16, 8, 0, 0, 0, zone), nextRun);
        assertEquals(Duration.ofHours(24), NextRunCalculator.calculateDelay(now, target, zone));
    }

    @Test
    public void leapYearTransition_feb28ToFeb29() {
        ZoneId zone = ZoneId.of("UTC");
        ZonedDateTime now = ZonedDateTime.of(2028, 2, 28, 12, 0, 0, 0, zone);
        ZonedDateTime nextMidnight = NextRunCalculator.calculateNextMidnight(now, zone);

        assertEquals(LocalDate.of(2028, 2, 29), nextMidnight.toLocalDate());
    }
}
