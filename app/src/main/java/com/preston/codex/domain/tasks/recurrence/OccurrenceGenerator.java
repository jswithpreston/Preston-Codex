package com.preston.codex.domain.tasks.recurrence;

import com.preston.codex.domain.tasks.model.RecurrenceRule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Pure domain generator for recurrence occurrences.
 * Free of Clock, ZoneId, and repository dependencies.
 */
public final class OccurrenceGenerator {

    private static final int HORIZON_DAYS = 90;

    public List<LocalDate> generate(
            RecurrenceRule rule,
            LocalDate today,
            Set<LocalDate> existingDates,
            Set<LocalDate> retainedDates,
            int existingCount
    ) {
        Objects.requireNonNull(rule, "rule must not be null");
        Objects.requireNonNull(today, "today must not be null");
        Set<LocalDate> existing = existingDates == null ? Collections.emptySet() : existingDates;
        Set<LocalDate> retained = retainedDates == null ? Collections.emptySet() : retainedDates;

        int maxToGenerate = Integer.MAX_VALUE;
        if (rule.occurrenceCount() != null) {
            maxToGenerate = rule.occurrenceCount() - existingCount;
            if (maxToGenerate <= 0) {
                return Collections.emptyList();
            }
        }

        // Window begins at max(rule.startDate, today) - NO backfill
        LocalDate windowStart = rule.startDate().isAfter(today) ? rule.startDate() : today;
        LocalDate windowEnd = today.plusDays(HORIZON_DAYS - 1);
        if (rule.endDate() != null && rule.endDate().isBefore(windowEnd)) {
            windowEnd = rule.endDate();
        }

        if (windowStart.isAfter(windowEnd)) {
            return Collections.emptyList();
        }

        List<LocalDate> result = new ArrayList<>();
        int interval = Math.max(1, rule.interval());

        switch (rule.type()) {
            case DAILY: {
                // Rule dates start at rule.startDate() stepping by interval
                long daysSinceStart = ChronoUnit.DAYS.between(rule.startDate(), windowStart);
                long initialStep;
                if (daysSinceStart <= 0) {
                    initialStep = 0;
                } else {
                    initialStep = (daysSinceStart + interval - 1) / interval;
                }

                LocalDate current = rule.startDate().plusDays(initialStep * interval);
                while (!current.isAfter(windowEnd)) {
                    if (!current.isBefore(windowStart) && !existing.contains(current) && !retained.contains(current)) {
                        result.add(current);
                        if (result.size() >= maxToGenerate) {
                            break;
                        }
                    }
                    current = current.plusDays(interval);
                }
                break;
            }

            case WEEKLY: {
                Set<DayOfWeek> activeDays = parseWeekdays(rule.weekdays(), rule.startDate().getDayOfWeek());
                LocalDate startMonday = rule.startDate().with(DayOfWeek.MONDAY);
                LocalDate current = windowStart;

                while (!current.isAfter(windowEnd)) {
                    LocalDate currentMonday = current.with(DayOfWeek.MONDAY);
                    long weeksBetween = ChronoUnit.WEEKS.between(startMonday, currentMonday);

                    if (weeksBetween >= 0 && (weeksBetween % interval == 0)) {
                        if (activeDays.contains(current.getDayOfWeek()) && !current.isBefore(rule.startDate())) {
                            if (!existing.contains(current) && !retained.contains(current)) {
                                result.add(current);
                                if (result.size() >= maxToGenerate) {
                                    break;
                                }
                            }
                        }
                    }
                    current = current.plusDays(1);
                }
                break;
            }

            case MONTHLY: {
                int anchor = rule.anchorDayOfMonth() != null ? rule.anchorDayOfMonth() : rule.startDate().getDayOfMonth();
                YearMonth startYm = YearMonth.from(rule.startDate());
                YearMonth windowStartYm = YearMonth.from(windowStart);
                YearMonth windowEndYm = YearMonth.from(windowEnd);

                long monthsSinceStart = ChronoUnit.MONTHS.between(startYm, windowStartYm);
                long initialStep = monthsSinceStart <= 0 ? 0 : (monthsSinceStart / interval);

                YearMonth currentYm = startYm.plusMonths(initialStep * interval);
                while (!currentYm.isAfter(windowEndYm)) {
                    int clampedDay = Math.min(anchor, currentYm.lengthOfMonth());
                    LocalDate candidate = currentYm.atDay(clampedDay);

                    if (!candidate.isBefore(rule.startDate()) && !candidate.isBefore(windowStart) && !candidate.isAfter(windowEnd)) {
                        if (!existing.contains(candidate) && !retained.contains(candidate)) {
                            result.add(candidate);
                            if (result.size() >= maxToGenerate) {
                                break;
                            }
                        }
                    }
                    currentYm = currentYm.plusMonths(interval);
                }
                break;
            }

            case CUSTOM_INTERVAL: {
                long daysSinceStart = ChronoUnit.DAYS.between(rule.startDate(), windowStart);
                long initialStep;
                if (daysSinceStart <= 0) {
                    initialStep = 0;
                } else {
                    initialStep = (daysSinceStart + interval - 1) / interval;
                }

                LocalDate current = rule.startDate().plusDays(initialStep * interval);
                while (!current.isAfter(windowEnd)) {
                    if (!current.isBefore(windowStart) && !existing.contains(current) && !retained.contains(current)) {
                        result.add(current);
                        if (result.size() >= maxToGenerate) {
                            break;
                        }
                    }
                    current = current.plusDays(interval);
                }
                break;
            }
        }

        return result;
    }

    private static Set<DayOfWeek> parseWeekdays(String weekdays, DayOfWeek defaultDay) {
        if (weekdays == null || weekdays.trim().isEmpty()) {
            return EnumSet.of(defaultDay);
        }
        Set<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
        String[] parts = weekdays.split(",");
        for (String part : parts) {
            String token = part.trim().toUpperCase();
            switch (token) {
                case "MON":
                    set.add(DayOfWeek.MONDAY);
                    break;
                case "TUE":
                    set.add(DayOfWeek.TUESDAY);
                    break;
                case "WED":
                    set.add(DayOfWeek.WEDNESDAY);
                    break;
                case "THU":
                    set.add(DayOfWeek.THURSDAY);
                    break;
                case "FRI":
                    set.add(DayOfWeek.FRIDAY);
                    break;
                case "SAT":
                    set.add(DayOfWeek.SATURDAY);
                    break;
                case "SUN":
                    set.add(DayOfWeek.SUNDAY);
                    break;
                default:
                    break;
            }
        }
        return set.isEmpty() ? EnumSet.of(defaultDay) : set;
    }
}
