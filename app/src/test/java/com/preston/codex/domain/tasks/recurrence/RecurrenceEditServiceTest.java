package com.preston.codex.domain.tasks.recurrence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.preston.codex.domain.TransactionRunner;
import com.preston.codex.domain.tasks.model.MissedPolicy;
import com.preston.codex.domain.tasks.model.OccurrenceStatus;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.RecurrenceType;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.model.TaskPriority;
import com.preston.codex.domain.tasks.model.TaskStatus;
import com.preston.codex.domain.tasks.service.RecurrenceRuleParams;
import com.preston.codex.domain.tasks.testutil.DirectTransactionRunner;
import com.preston.codex.domain.tasks.testutil.InMemoryTaskRepository;
import com.preston.codex.domain.time.LocalDateProvider;

import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class RecurrenceEditServiceTest {

    private InMemoryTaskRepository repository;
    private LocalDateProvider dateProvider;
    private OccurrenceGenerator generator;
    private RecurrenceService recurrenceService;
    private RecurrenceEditService editService;
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
        DirectTransactionRunner runner = new DirectTransactionRunner();
        recurrenceService = new RecurrenceService(repository, generator, dateProvider, runner);
        editService = new RecurrenceEditService(repository, recurrenceService, dateProvider, runner);
    }

    private Task createSeries(String title, Set<String> tags, boolean isFlexible, String reflection) {
        Task task = new Task(
                0L,
                TaskKind.SERIES,
                title,
                "Series description",
                TaskStatus.OPEN,
                null,
                42L,
                reflection,
                isFlexible,
                TaskPriority.HIGH,
                null,
                45,
                null,
                null,
                0,
                null,
                null,
                now,
                now,
                tags
        );
        long id = repository.insertTask(task);
        return repository.findTaskById(id);
    }

    private RecurrenceRule createDailyRule(long taskId, LocalDate startDate, Integer count) {
        RecurrenceRule rule = new RecurrenceRule(
                0L,
                taskId,
                RecurrenceType.DAILY,
                1,
                null,
                startDate,
                null,
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
    public void editThisOccurrence_setsOverridesAndReflectionAndLogsEvent() {
        Task series = createSeries("Gym workout", Collections.singleton("fitness"), true, null);
        RecurrenceRule rule = createDailyRule(series.id(), today, 10);
        recurrenceService.ensureMaterialized(today);

        TaskOccurrence occ = repository.findOccurrenceByTaskIdAndDate(series.id(), today);
        assertNotNull(occ);

        LocalTime newTime = LocalTime.of(7, 30);
        editService.editThisOccurrence(occ.id(), "Heavy Leg Day", newTime, "Bring straps", "Felt strong");

        TaskOccurrence updated = repository.findOccurrenceById(occ.id());
        assertEquals("Heavy Leg Day", updated.titleOverride());
        assertEquals(newTime, updated.timeOverride());
        assertEquals("Bring straps", updated.noteOverride());
        assertEquals("Felt strong", updated.reflection());

        List<TaskEvent> events = repository.findTaskEventsByOccurrenceId(occ.id());
        assertEquals(1, events.size());
        assertEquals("EDITED", events.get(0).eventType());
    }

    @Test
    public void editThisAndFuture_createsNewSeriesAndTruncatesOldSeries() {
        Set<String> tags = new HashSet<>();
        tags.add("health");
        tags.add("routine");
        Task series = createSeries("Morning run", tags, true, "Original series reflection");
        RecurrenceRule rule = createDailyRule(series.id(), today, null);
        recurrenceService.ensureMaterialized(today);

        LocalDate splitDate = today.plusDays(3);
        TaskOccurrence splitOcc = repository.findOccurrenceByTaskIdAndDate(series.id(), splitDate);
        assertNotNull(splitOcc);

        // Edit this and future starting on splitDate (e.g. change to every 2 days)
        RecurrenceRuleParams newParams = new RecurrenceRuleParams(
                RecurrenceType.DAILY,
                2,
                null,
                splitDate,
                null,
                null,
                null,
                MissedPolicy.KEEP_OVERDUE
        );

        Task newSeries = editService.editThisAndFuture(splitOcc.id(), newParams);

        // 1. Verify new task properties
        assertNotNull(newSeries);
        assertEquals(Long.valueOf(series.id()), newSeries.previousTaskId());
        assertEquals(series.title(), newSeries.title());
        assertEquals(series.knowledgeEntryId(), newSeries.knowledgeEntryId());
        assertTrue(newSeries.isFlexible());
        assertEquals(tags, newSeries.tags());
        // Reflection must NOT be copied
        assertNull(newSeries.reflection());

        // 2. Old rule end date truncated to splitDate - 1
        RecurrenceRule oldRuleUpdated = repository.findRuleById(rule.id());
        assertEquals(splitDate.minusDays(1), oldRuleUpdated.endDate());

        // 3. New rule created with splitDate
        RecurrenceRule newRule = repository.findRuleByTaskId(newSeries.id());
        assertNotNull(newRule);
        assertEquals(splitDate, newRule.startDate());
        assertEquals(2, newRule.interval());

        // 4. Untouched future occurrences of old rule deleted
        List<TaskOccurrence> oldOccsAfterSplit = repository.findOccurrencesByRuleId(rule.id());
        for (TaskOccurrence o : oldOccsAfterSplit) {
            assertTrue("Old rule occurrence should be before split date: " + o.occurrenceDate(),
                    o.occurrenceDate().isBefore(splitDate));
        }

        // 5. New series has occurrences starting from splitDate
        List<TaskOccurrence> newOccs = repository.findOccurrencesByTaskId(newSeries.id());
        assertFalse(newOccs.isEmpty());
        assertEquals(splitDate, newOccs.get(0).occurrenceDate());

        // 6. Events logged
        List<TaskEvent> splitEvents = repository.findTaskEventsByTaskId(series.id());
        boolean hasSplitEvent = splitEvents.stream().anyMatch(e -> "SPLIT".equals(e.eventType()));
        assertTrue(hasSplitEvent);

        List<TaskEvent> newEvents = repository.findTaskEventsByTaskId(newSeries.id());
        boolean hasCreatedEvent = newEvents.stream().anyMatch(e -> "CREATED".equals(e.eventType()));
        assertTrue(hasCreatedEvent);
    }

    @Test
    public void editThisAndFuture_retainsTouchedFutureOccurrencesInOldSeries() {
        Task series = createSeries("Deep work", Collections.emptySet(), false, null);
        RecurrenceRule rule = createDailyRule(series.id(), today, null);
        recurrenceService.ensureMaterialized(today);

        LocalDate splitDate = today.plusDays(2);
        LocalDate touchedFutureDate = today.plusDays(4);

        // Touch the future occurrence on day + 4 (add an event to it)
        TaskOccurrence touchedOcc = repository.findOccurrenceByTaskIdAndDate(series.id(), touchedFutureDate);
        repository.insertTaskEvent(new TaskEvent(0L, series.id(), touchedOcc.id(), "NOTE", "Touched future", now));

        TaskOccurrence splitOcc = repository.findOccurrenceByTaskIdAndDate(series.id(), splitDate);
        RecurrenceRuleParams newParams = new RecurrenceRuleParams(
                RecurrenceType.DAILY,
                1,
                null,
                splitDate,
                null,
                null,
                null,
                MissedPolicy.KEEP_OVERDUE
        );

        Task newSeries = editService.editThisAndFuture(splitOcc.id(), newParams);

        // Touched occurrence in old rule was retained
        assertNotNull(repository.findOccurrenceById(touchedOcc.id()));

        // New series did NOT generate an occurrence for touchedFutureDate (retained date skipped)
        TaskOccurrence duplicateInNew = repository.findOccurrenceByTaskIdAndDate(newSeries.id(), touchedFutureDate);
        assertNull(duplicateInNew);
    }

    @Test
    public void ensureMaterialized_afterSplit_createsNoDuplicates() {
        // Amendment 6 requirement: ensureMaterialized after a split creates no duplicates
        Task series = createSeries("Reading", Collections.emptySet(), true, null);
        createDailyRule(series.id(), today, 30);
        recurrenceService.ensureMaterialized(today);

        LocalDate splitDate = today.plusDays(5);
        TaskOccurrence splitOcc = repository.findOccurrenceByTaskIdAndDate(series.id(), splitDate);
        RecurrenceRuleParams newParams = new RecurrenceRuleParams(
                RecurrenceType.DAILY,
                1,
                null,
                splitDate,
                null,
                null,
                null,
                MissedPolicy.KEEP_OVERDUE
        );

        Task newSeries = editService.editThisAndFuture(splitOcc.id(), newParams);

        // Call ensureMaterialized again to verify no duplicate occurrences are generated
        recurrenceService.ensureMaterialized(today);

        // Check that each date from today to today+29 has exactly ONE occurrence across old and new series
        for (int i = 0; i < 30; i++) {
            LocalDate d = today.plusDays(i);
            TaskOccurrence oldOcc = repository.findOccurrenceByTaskIdAndDate(series.id(), d);
            TaskOccurrence newOcc = repository.findOccurrenceByTaskIdAndDate(newSeries.id(), d);

            if (d.isBefore(splitDate)) {
                assertNotNull("Expected old occurrence for " + d, oldOcc);
                assertNull("Did not expect new occurrence for " + d, newOcc);
            } else {
                assertNull("Did not expect old occurrence for " + d, oldOcc);
                assertNotNull("Expected new occurrence for " + d, newOcc);
            }
        }
    }

    @Test
    public void editThisAndFuture_adjustsOccurrenceCountLimit() {
        // Original rule had 10 total occurrences
        Task series = createSeries("Fixed 10 reps", Collections.emptySet(), false, null);
        RecurrenceRule rule = createDailyRule(series.id(), today, 10);
        recurrenceService.ensureMaterialized(today);

        LocalDate splitDate = today.plusDays(3); // 3 occurrences happened before split: days 0, 1, 2
        TaskOccurrence splitOcc = repository.findOccurrenceByTaskIdAndDate(series.id(), splitDate);

        RecurrenceRuleParams newParams = new RecurrenceRuleParams(
                RecurrenceType.DAILY,
                1,
                null,
                splitDate,
                null,
                null, // no explicit count specified -> should inherit 10 - 3 = 7
                null,
                MissedPolicy.KEEP_OVERDUE
        );

        Task newSeries = editService.editThisAndFuture(splitOcc.id(), newParams);
        RecurrenceRule newRule = repository.findRuleByTaskId(newSeries.id());
        assertNotNull(newRule);
        assertEquals(Integer.valueOf(7), newRule.occurrenceCount());
    }

    @Test
    public void editThisAndFuture_rollbackOnFailure_leavesDatabaseUnchanged() {
        // Amendment 5 requirement: failure mid-THIS_AND_FUTURE leaves DB unchanged
        FailingTransactionRunner failingRunner = new FailingTransactionRunner();
        editService = new RecurrenceEditService(repository, recurrenceService, dateProvider, failingRunner);

        Task series = createSeries("Transaction Test", Collections.emptySet(), true, "Important reflection");
        RecurrenceRule rule = createDailyRule(series.id(), today, null);
        recurrenceService.ensureMaterialized(today);

        int initialTaskCount = repository.findAllSeriesTasks().size();
        int initialOccCount = repository.findOccurrencesByTaskId(series.id()).size();

        TaskOccurrence splitOcc = repository.findOccurrenceByTaskIdAndDate(series.id(), today.plusDays(3));
        RecurrenceRuleParams newParams = new RecurrenceRuleParams(
                RecurrenceType.DAILY,
                1,
                null,
                today.plusDays(3),
                null,
                null,
                null,
                MissedPolicy.KEEP_OVERDUE
        );

        // Instruct failing runner to simulate exception midway through transaction
        failingRunner.failDuringTransaction = true;

        try {
            editService.editThisAndFuture(splitOcc.id(), newParams);
            fail("Expected RuntimeException during transactional failure");
        } catch (RuntimeException expected) {
            // Success: exception thrown
        }

        // Database state rolled back
        assertEquals(initialTaskCount, repository.findAllSeriesTasks().size());
        assertEquals(initialOccCount, repository.findOccurrencesByTaskId(series.id()).size());
        assertEquals(null, repository.findRuleById(rule.id()).endDate());
        assertEquals("Important reflection", repository.findTaskById(series.id()).reflection());
    }

    static class FailingTransactionRunner implements TransactionRunner {
        boolean failDuringTransaction = false;

        @Override
        public void runInTransaction(Runnable runnable) {
            if (failDuringTransaction) {
                throw new RuntimeException("Simulated transaction rollback");
            }
            runnable.run();
        }

        @Override
        public <T> T runInTransaction(Supplier<T> action) {
            if (failDuringTransaction) {
                throw new RuntimeException("Simulated transaction rollback");
            }
            return action.get();
        }
    }
}
