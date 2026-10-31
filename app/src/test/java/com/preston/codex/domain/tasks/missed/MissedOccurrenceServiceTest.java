package com.preston.codex.domain.tasks.missed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.preston.codex.domain.tasks.model.MissedPolicy;
import com.preston.codex.domain.tasks.model.OccurrenceStatus;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.RecurrenceType;
import com.preston.codex.domain.tasks.model.RescheduleRecord;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.model.TaskPriority;
import com.preston.codex.domain.tasks.model.TaskStatus;
import com.preston.codex.domain.tasks.testutil.DirectTransactionRunner;
import com.preston.codex.domain.tasks.testutil.InMemoryTaskRepository;
import com.preston.codex.domain.time.LocalDateProvider;

import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

public class MissedOccurrenceServiceTest {

    private InMemoryTaskRepository repository;
    private LocalDateProvider dateProvider;
    private MissedOccurrenceService missedService;
    private LocalDate today;
    private Instant now;

    @Before
    public void setUp() {
        repository = new InMemoryTaskRepository();
        today = LocalDate.of(2026, 4, 10);
        ZoneId zone = ZoneId.of("UTC");
        now = today.atStartOfDay(zone).toInstant();
        Clock clock = Clock.fixed(now, zone);
        dateProvider = new LocalDateProvider(clock, () -> zone);
        missedService = new MissedOccurrenceService(repository, dateProvider, new DirectTransactionRunner());
    }

    private Task createSeries(String title, boolean isFlexible) {
        Task task = new Task(
                0L,
                TaskKind.SERIES,
                title,
                null,
                TaskStatus.OPEN,
                null,
                null,
                null,
                isFlexible,
                TaskPriority.MEDIUM,
                null,
                30,
                null,
                null,
                0,
                null,
                null,
                now,
                now,
                Collections.emptySet()
        );
        long id = repository.insertTask(task);
        return repository.findTaskById(id);
    }

    private RecurrenceRule createRule(long taskId, MissedPolicy policy) {
        RecurrenceRule rule = new RecurrenceRule(
                0L,
                taskId,
                RecurrenceType.DAILY,
                1,
                null,
                today.minusDays(10),
                null,
                null,
                null,
                policy,
                now,
                now
        );
        long id = repository.insertRecurrenceRule(rule);
        return repository.findRuleById(id);
    }

    private TaskOccurrence createOccurrence(long taskId, long ruleId, LocalDate occDate, LocalDate effDate, int rescheduleCount) {
        TaskOccurrence occ = new TaskOccurrence(
                0L,
                taskId,
                ruleId,
                occDate,
                effDate,
                OccurrenceStatus.OPEN,
                null,
                null,
                null,
                null,
                rescheduleCount,
                null,
                null,
                now,
                now
        );
        long id = repository.insertOccurrence(occ);
        return repository.findOccurrenceById(id);
    }

    @Test
    public void keepOverdue_leavesOccurrenceUntouched() {
        Task series = createSeries("Keep Overdue Series", true);
        RecurrenceRule rule = createRule(series.id(), MissedPolicy.KEEP_OVERDUE);
        TaskOccurrence occ = createOccurrence(series.id(), rule.id(), today.minusDays(2), today.minusDays(2), 0);

        List<TaskOccurrence> handled = missedService.handleMissedOccurrences(today);
        assertTrue(handled.isEmpty());

        TaskOccurrence unchanged = repository.findOccurrenceById(occ.id());
        assertEquals(OccurrenceStatus.OPEN, unchanged.status());
        assertEquals(today.minusDays(2), unchanged.effectiveDate());
    }

    @Test
    public void skipAutomatically_marksOccurrenceAsSkippedAndLogsEvent() {
        Task series = createSeries("Auto Skip Series", false);
        RecurrenceRule rule = createRule(series.id(), MissedPolicy.SKIP_AUTOMATICALLY);
        TaskOccurrence occ = createOccurrence(series.id(), rule.id(), today.minusDays(1), today.minusDays(1), 0);

        List<TaskOccurrence> handled = missedService.handleMissedOccurrences(today);
        assertEquals(1, handled.size());
        assertEquals(OccurrenceStatus.SKIPPED, handled.get(0).status());

        TaskOccurrence updated = repository.findOccurrenceById(occ.id());
        assertEquals(OccurrenceStatus.SKIPPED, updated.status());

        List<TaskEvent> events = repository.findTaskEventsByOccurrenceId(occ.id());
        assertEquals(1, events.size());
        assertEquals("POLICY", events.get(0).eventType());
    }

    @Test
    public void carryForward_flexibleTask_advancesEffectiveDateToTodayAndIncrementsCount() {
        Task series = createSeries("Flexible Series", true);
        RecurrenceRule rule = createRule(series.id(), MissedPolicy.CARRY_FORWARD_FLEXIBLE_ONLY);
        TaskOccurrence occ = createOccurrence(series.id(), rule.id(), today.minusDays(1), today.minusDays(1), 0);

        List<TaskOccurrence> handled = missedService.handleMissedOccurrences(today);
        assertEquals(1, handled.size());
        assertEquals(today, handled.get(0).effectiveDate());
        assertEquals(1, handled.get(0).autoRescheduleCount());

        TaskOccurrence updated = repository.findOccurrenceById(occ.id());
        assertEquals(today, updated.effectiveDate());
        assertEquals(1, updated.autoRescheduleCount());

        List<RescheduleRecord> history = repository.findRescheduleRecordsByOccurrenceId(occ.id());
        assertEquals(1, history.size());
        assertEquals("CARRY_FORWARD", history.get(0).reason());
        assertEquals(today.minusDays(1), history.get(0).fromDate());
        assertEquals(today, history.get(0).toDate());

        // Idempotency: running again same day does NOT re-handle it
        List<TaskOccurrence> handledSecondRun = missedService.handleMissedOccurrences(today);
        assertTrue(handledSecondRun.isEmpty());
    }

    @Test
    public void carryForward_nonFlexibleTask_leavesUntouched() {
        Task series = createSeries("Strict Series", false);
        RecurrenceRule rule = createRule(series.id(), MissedPolicy.CARRY_FORWARD_FLEXIBLE_ONLY);
        TaskOccurrence occ = createOccurrence(series.id(), rule.id(), today.minusDays(1), today.minusDays(1), 0);

        List<TaskOccurrence> handled = missedService.handleMissedOccurrences(today);
        assertTrue(handled.isEmpty());

        TaskOccurrence unchanged = repository.findOccurrenceById(occ.id());
        assertEquals(today.minusDays(1), unchanged.effectiveDate());
        assertEquals(0, unchanged.autoRescheduleCount());
    }

    @Test
    public void carryForward_stopsAtMaxCountThree() {
        Task series = createSeries("Flexible Max Series", true);
        RecurrenceRule rule = createRule(series.id(), MissedPolicy.CARRY_FORWARD_FLEXIBLE_ONLY);
        // Already carried forward 3 times
        TaskOccurrence occ = createOccurrence(series.id(), rule.id(), today.minusDays(4), today.minusDays(1), 3);

        List<TaskOccurrence> handled = missedService.handleMissedOccurrences(today);
        assertTrue(handled.isEmpty());

        TaskOccurrence unchanged = repository.findOccurrenceById(occ.id());
        assertEquals(today.minusDays(1), unchanged.effectiveDate());
        assertEquals(3, unchanged.autoRescheduleCount());
    }

    @Test
    public void evaluatesEffectiveDateNotOccurrenceDate() {
        // Occurrence date was 5 days ago, but effective date was postponed to tomorrow (today + 1)
        Task series = createSeries("Postponed Series", true);
        RecurrenceRule rule = createRule(series.id(), MissedPolicy.SKIP_AUTOMATICALLY);
        createOccurrence(series.id(), rule.id(), today.minusDays(5), today.plusDays(1), 0);

        // Should not be treated as missed because effective_date >= today
        List<TaskOccurrence> handled = missedService.handleMissedOccurrences(today);
        assertTrue(handled.isEmpty());
    }

    @Test
    public void recentlyHandled_returnsCarriedForwardOccurrencesForDate() {
        Task series = createSeries("Flexible Series", true);
        RecurrenceRule rule = createRule(series.id(), MissedPolicy.CARRY_FORWARD_FLEXIBLE_ONLY);
        TaskOccurrence occ = createOccurrence(series.id(), rule.id(), today.minusDays(1), today.minusDays(1), 0);

        missedService.handleMissedOccurrences(today);

        List<TaskOccurrence> recent = missedService.recentlyHandled(today);
        assertEquals(1, recent.size());
        assertEquals(occ.id(), recent.get(0).id());
    }
}
