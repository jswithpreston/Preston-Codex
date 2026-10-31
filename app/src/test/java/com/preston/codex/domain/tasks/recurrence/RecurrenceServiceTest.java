package com.preston.codex.domain.tasks.recurrence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.preston.codex.domain.tasks.model.MissedPolicy;
import com.preston.codex.domain.tasks.model.OccurrenceStatus;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.RecurrenceType;
import com.preston.codex.domain.tasks.model.Task;
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

public class RecurrenceServiceTest {

    private InMemoryTaskRepository repository;
    private LocalDateProvider dateProvider;
    private OccurrenceGenerator generator;
    private RecurrenceService recurrenceService;
    private LocalDate today;
    private Instant now;

    @Before
    public void setUp() {
        repository = new InMemoryTaskRepository();
        today = LocalDate.of(2026, 4, 1);
        ZoneId zone = ZoneId.of("UTC");
        now = today.atStartOfDay(zone).toInstant();
        Clock clock = Clock.fixed(now, zone);
        dateProvider = new LocalDateProvider(clock, () -> zone);
        generator = new OccurrenceGenerator();
        recurrenceService = new RecurrenceService(repository, generator, dateProvider, new DirectTransactionRunner());
    }

    private Task createSeries(String title, TaskStatus status) {
        Task task = new Task(
                0L,
                TaskKind.SERIES,
                title,
                null,
                status,
                null,
                null,
                null,
                false,
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

    private RecurrenceRule createRule(long taskId, RecurrenceType type, int interval, LocalDate startDate, LocalDate endDate, Integer count) {
        RecurrenceRule rule = new RecurrenceRule(
                0L,
                taskId,
                type,
                interval,
                null,
                startDate,
                endDate,
                count,
                null,
                MissedPolicy.KEEP_OVERDUE,
                now,
                now
        );
        long id = repository.insertRecurrenceRule(rule);
        return repository.findRuleById(id);
    }

    @Test
    public void ensureMaterialized_generatesOccurrences_upTo90Days() {
        Task series = createSeries("Daily Standup", TaskStatus.OPEN);
        createRule(series.id(), RecurrenceType.DAILY, 1, today, null, null);

        recurrenceService.ensureMaterialized(today);

        List<TaskOccurrence> stored = repository.findOccurrencesByTaskId(series.id());
        assertEquals(90, stored.size());
        assertEquals(today, stored.get(0).occurrenceDate());
        assertEquals(today.plusDays(89), stored.get(89).occurrenceDate());
    }

    @Test
    public void ensureMaterialized_idempotentOnSubsequentRuns() {
        Task series = createSeries("Weekly Planning", TaskStatus.OPEN);
        createRule(series.id(), RecurrenceType.WEEKLY, 1, today, null, null);

        recurrenceService.ensureMaterialized(today);
        int firstCount = repository.findOccurrencesByTaskId(series.id()).size();
        assertTrue(firstCount > 0);

        recurrenceService.ensureMaterialized(today);
        int secondCount = repository.findOccurrencesByTaskId(series.id()).size();
        assertEquals(firstCount, secondCount);
    }

    @Test
    public void ensureMaterialized_skipsCancelledSeries() {
        Task series = createSeries("Cancelled Series", TaskStatus.CANCELLED);
        createRule(series.id(), RecurrenceType.DAILY, 1, today, null, null);

        recurrenceService.ensureMaterialized(today);
        assertTrue(repository.findOccurrencesByTaskId(series.id()).isEmpty());
    }

    @Test
    public void ensureMaterialized_respectsCountLimit_includingCancelledAndSkipped() {
        Task series = createSeries("Limited Series", TaskStatus.OPEN);
        RecurrenceRule rule = createRule(series.id(), RecurrenceType.DAILY, 1, today, null, 5);

        // Pre-insert 2 occurrences (one completed, one cancelled)
        TaskOccurrence occ1 = new TaskOccurrence(0L, series.id(), rule.id(), today, today, OccurrenceStatus.COMPLETED, null, null, null, null, 0, null, null, now, now);
        TaskOccurrence occ2 = new TaskOccurrence(0L, series.id(), rule.id(), today.plusDays(1), today.plusDays(1), OccurrenceStatus.CANCELLED, null, null, null, null, 0, null, null, now, now);
        repository.insertOccurrence(occ1);
        repository.insertOccurrence(occ2);

        recurrenceService.ensureMaterialized(today);

        List<TaskOccurrence> all = repository.findOccurrencesByTaskId(series.id());
        // Total count limit is 5. We already had 2. Exactly 3 new ones were created.
        assertEquals(5, all.size());
    }

    @Test
    public void ensureMaterialized_respectsEndDate() {
        Task series = createSeries("Short Series", TaskStatus.OPEN);
        createRule(series.id(), RecurrenceType.DAILY, 1, today, today.plusDays(4), null);

        recurrenceService.ensureMaterialized(today);

        List<TaskOccurrence> stored = repository.findOccurrencesByTaskId(series.id());
        assertEquals(5, stored.size());
        assertEquals(today.plusDays(4), stored.get(stored.size() - 1).occurrenceDate());
    }

    @Test
    public void ensureMaterialized_materializesAllOpenSeriesInBatch() {
        Task series1 = createSeries("Series 1", TaskStatus.OPEN);
        createRule(series1.id(), RecurrenceType.DAILY, 1, today, today.plusDays(2), null);

        Task series2 = createSeries("Series 2", TaskStatus.OPEN);
        createRule(series2.id(), RecurrenceType.DAILY, 1, today, today.plusDays(3), null);

        Task series3 = createSeries("Series 3", TaskStatus.CANCELLED);
        createRule(series3.id(), RecurrenceType.DAILY, 1, today, today.plusDays(10), null);

        recurrenceService.ensureMaterialized(today);

        assertEquals(3, repository.findOccurrencesByTaskId(series1.id()).size());
        assertEquals(4, repository.findOccurrencesByTaskId(series2.id()).size());
        assertEquals(0, repository.findOccurrencesByTaskId(series3.id()).size());
    }
}
