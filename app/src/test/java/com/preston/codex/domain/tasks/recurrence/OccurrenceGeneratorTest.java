package com.preston.codex.domain.tasks.recurrence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.preston.codex.domain.tasks.model.MissedPolicy;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.RecurrenceType;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class OccurrenceGeneratorTest {

    private OccurrenceGenerator generator;
    private final LocalDate today = LocalDate.of(2026, 10, 30);
    private final Instant now = Instant.ofEpochMilli(1000000L);

    @Before
    public void setUp() {
        generator = new OccurrenceGenerator();
    }

    @Test
    public void daily_startsToday_generates90Days() {
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.DAILY, 1, null,
                today, null, null, null,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        List<LocalDate> dates = generator.generate(rule, today, Collections.emptySet(), Collections.emptySet(), 0);
        assertEquals(90, dates.size());
        assertEquals(today, dates.get(0));
        assertEquals(today.plusDays(89), dates.get(dates.size() - 1));
    }

    @Test
    public void daily_futureStartDate_startsAtStartDate() {
        LocalDate futureStart = today.plusDays(10);
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.DAILY, 1, null,
                futureStart, null, null, null,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        List<LocalDate> dates = generator.generate(rule, today, Collections.emptySet(), Collections.emptySet(), 0);
        assertEquals(80, dates.size());
        assertEquals(futureStart, dates.get(0));
        assertEquals(today.plusDays(89), dates.get(dates.size() - 1));
    }

    @Test
    public void daily_pastStartDate_doesNotBackfillPastDates() {
        LocalDate pastStart = today.minusDays(30);
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.DAILY, 1, null,
                pastStart, null, null, null,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        List<LocalDate> dates = generator.generate(rule, today, Collections.emptySet(), Collections.emptySet(), 0);
        assertEquals(90, dates.size());
        assertEquals(today, dates.get(0));
        assertFalse(dates.contains(pastStart));
    }

    @Test
    public void existingDates_areSkippedIdempotently() {
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.DAILY, 1, null,
                today, null, null, null,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        Set<LocalDate> existing = Set.of(today, today.plusDays(1), today.plusDays(2));
        List<LocalDate> dates = generator.generate(rule, today, existing, Collections.emptySet(), 3);
        assertEquals(87, dates.size());
        assertEquals(today.plusDays(3), dates.get(0));
    }

    @Test
    public void retainedDates_areSkippedForSplitSeries() {
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.DAILY, 1, null,
                today, null, null, null,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        Set<LocalDate> retained = Set.of(today.plusDays(5), today.plusDays(6));
        List<LocalDate> dates = generator.generate(rule, today, Collections.emptySet(), retained, 0);
        assertFalse(dates.contains(today.plusDays(5)));
        assertFalse(dates.contains(today.plusDays(6)));
    }

    @Test
    public void countLimitedRule_stopsWhenOccurrenceCountReached() {
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.DAILY, 1, null,
                today, null, 5, null,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        List<LocalDate> dates = generator.generate(rule, today, Collections.emptySet(), Collections.emptySet(), 2);
        // occurrenceCount = 5, existingCount = 2 -> generates 3 more
        assertEquals(3, dates.size());
        assertEquals(today, dates.get(0));
        assertEquals(today.plusDays(2), dates.get(2));
    }

    @Test
    public void countLimitedRule_withZeroRemaining_returnsEmpty() {
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.DAILY, 1, null,
                today, null, 5, null,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        List<LocalDate> dates = generator.generate(rule, today, Collections.emptySet(), Collections.emptySet(), 5);
        assertTrue(dates.isEmpty());
    }

    @Test
    public void weekly_interval2_usesIsoWeekIndex() {
        // Monday 2026-11-02
        LocalDate startMonday = LocalDate.of(2026, 11, 2);
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.WEEKLY, 2, "MON",
                startMonday, null, null, null,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        List<LocalDate> dates = generator.generate(rule, startMonday, Collections.emptySet(), Collections.emptySet(), 0);
        // Every other Monday: Nov 2, Nov 16, Nov 30, Dec 14, Dec 28, Jan 11, Jan 25
        assertTrue(dates.contains(LocalDate.of(2026, 11, 2)));
        assertFalse("Week 1 (Nov 9) must not be generated", dates.contains(LocalDate.of(2026, 11, 9)));
        assertTrue("Week 2 (Nov 16) must be generated", dates.contains(LocalDate.of(2026, 11, 16)));
        assertFalse("Week 3 (Nov 23) must not be generated", dates.contains(LocalDate.of(2026, 11, 23)));
        assertTrue("Week 4 (Nov 30) must be generated", dates.contains(LocalDate.of(2026, 11, 30)));
    }

    @Test
    public void weekly_multipleWeekdays_generatesAllActiveDaysInWeek() {
        LocalDate startMonday = LocalDate.of(2026, 11, 2);
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.WEEKLY, 1, "MON,WED,FRI",
                startMonday, null, null, null,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        List<LocalDate> dates = generator.generate(rule, startMonday, Collections.emptySet(), Collections.emptySet(), 0);
        assertTrue(dates.contains(LocalDate.of(2026, 11, 2))); // Monday
        assertTrue(dates.contains(LocalDate.of(2026, 11, 4))); // Wednesday
        assertTrue(dates.contains(LocalDate.of(2026, 11, 6))); // Friday
        assertFalse(dates.contains(LocalDate.of(2026, 11, 3))); // Tuesday
        assertFalse(dates.contains(LocalDate.of(2026, 11, 5))); // Thursday
    }

    @Test
    public void monthly_anchorDay31_clampsOn30DayMonths() {
        // Start date: 2026-03-31, interval 1 month
        LocalDate start = LocalDate.of(2026, 3, 31);
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.MONTHLY, 1, null,
                start, null, null, 31,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        List<LocalDate> dates = generator.generate(rule, start, Collections.emptySet(), Collections.emptySet(), 0);
        assertTrue(dates.contains(LocalDate.of(2026, 3, 31)));
        // April has 30 days -> clamped to April 30
        assertTrue(dates.contains(LocalDate.of(2026, 4, 30)));
        // May has 31 days -> May 31
        assertTrue(dates.contains(LocalDate.of(2026, 5, 31)));
    }

    @Test
    public void monthly_anchorDay31_clampsOnLeapYearFebruary() {
        // Leap year 2024: Feb has 29 days
        LocalDate start = LocalDate.of(2024, 1, 31);
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.MONTHLY, 1, null,
                start, null, null, 31,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        List<LocalDate> dates = generator.generate(rule, start, Collections.emptySet(), Collections.emptySet(), 0);
        assertTrue(dates.contains(LocalDate.of(2024, 1, 31)));
        assertTrue("Leap year Feb must clamp to 29", dates.contains(LocalDate.of(2024, 2, 29)));
        assertTrue(dates.contains(LocalDate.of(2024, 3, 31)));
    }

    @Test
    public void monthly_anchorDay31_clampsOnNonLeapYearFebruary() {
        // Non-leap year 2026: Feb has 28 days
        LocalDate start = LocalDate.of(2026, 1, 31);
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.MONTHLY, 1, null,
                start, null, null, 31,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        List<LocalDate> dates = generator.generate(rule, start, Collections.emptySet(), Collections.emptySet(), 0);
        assertTrue(dates.contains(LocalDate.of(2026, 1, 31)));
        assertTrue("Non-leap year Feb must clamp to 28", dates.contains(LocalDate.of(2026, 2, 28)));
        assertTrue(dates.contains(LocalDate.of(2026, 3, 31)));
    }

    @Test
    public void customInterval_stepsCorrectlyAcrossMonths() {
        LocalDate start = LocalDate.of(2026, 10, 15);
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.CUSTOM_INTERVAL, 10, null,
                start, null, null, null,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        List<LocalDate> dates = generator.generate(rule, start, Collections.emptySet(), Collections.emptySet(), 0);
        assertTrue(dates.contains(LocalDate.of(2026, 10, 15)));
        assertTrue(dates.contains(LocalDate.of(2026, 10, 25)));
        assertTrue(dates.contains(LocalDate.of(2026, 11, 4)));
        assertTrue(dates.contains(LocalDate.of(2026, 11, 14)));
    }

    @Test
    public void endDate_strictlyRespected() {
        LocalDate start = LocalDate.of(2026, 10, 30);
        LocalDate end = LocalDate.of(2026, 11, 5);
        RecurrenceRule rule = new RecurrenceRule(
                1L, 10L, RecurrenceType.DAILY, 1, null,
                start, end, null, null,
                MissedPolicy.KEEP_OVERDUE, now, now
        );

        List<LocalDate> dates = generator.generate(rule, start, Collections.emptySet(), Collections.emptySet(), 0);
        assertEquals(7, dates.size());
        assertEquals(end, dates.get(dates.size() - 1));
        assertFalse(dates.contains(end.plusDays(1)));
    }
}
