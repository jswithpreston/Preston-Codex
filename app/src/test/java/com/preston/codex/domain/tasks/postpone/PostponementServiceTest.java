package com.preston.codex.domain.tasks.postpone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.preston.codex.domain.tasks.model.OccurrenceStatus;
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

public class PostponementServiceTest {

    private InMemoryTaskRepository repository;
    private LocalDateProvider dateProvider;
    private PostponementService postponementService;
    private LocalDate today;
    private Instant now;

    @Before
    public void setUp() {
        repository = new InMemoryTaskRepository();
        today = LocalDate.of(2026, 4, 15);
        ZoneId zone = ZoneId.of("UTC");
        now = today.atStartOfDay(zone).toInstant();
        Clock clock = Clock.fixed(now, zone);
        dateProvider = new LocalDateProvider(clock, () -> zone);
        postponementService = new PostponementService(repository, dateProvider, new DirectTransactionRunner());
    }

    private Task createOneOff(String title, LocalDate scheduledDate, LocalDate deadline) {
        Task task = new Task(
                0L,
                TaskKind.ONE_OFF,
                title,
                "Description",
                TaskStatus.OPEN,
                null,
                null,
                null,
                false,
                TaskPriority.MEDIUM,
                deadline,
                30,
                scheduledDate,
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

    @Test
    public void postponeOneOffTask_updatesScheduledDateAndAppendsHistoryAndEvent() {
        LocalDate initialDate = today.plusDays(1);
        LocalDate targetDate = today.plusDays(3);
        Task task = createOneOff("Prepare Tax Return", initialDate, today.plusDays(10));

        postponementService.postponeOneOffTask(task.id(), targetDate, "Waiting for W2 document");

        Task updated = repository.findTaskById(task.id());
        assertEquals(targetDate, updated.scheduledDate());

        List<RescheduleRecord> history = repository.findRescheduleRecordsByTaskId(task.id());
        assertEquals(1, history.size());
        assertEquals(initialDate, history.get(0).fromDate());
        assertEquals(targetDate, history.get(0).toDate());
        assertEquals("Waiting for W2 document", history.get(0).reason());

        List<TaskEvent> events = repository.findTaskEventsByTaskId(task.id());
        assertEquals(1, events.size());
        assertEquals("POSTPONED", events.get(0).eventType());
        assertTrue(events.get(0).details().contains("Waiting for W2 document"));
    }

    @Test
    public void postponeOneOffTask_pastDeadline_allowedWithWarningInEvent() {
        LocalDate deadline = today.plusDays(2);
        LocalDate targetDate = today.plusDays(5); // past deadline
        Task task = createOneOff("Ship Package", today, deadline);

        postponementService.postponeOneOffTask(task.id(), targetDate, "Weather delay");

        Task updated = repository.findTaskById(task.id());
        assertEquals(targetDate, updated.scheduledDate());

        List<TaskEvent> events = repository.findTaskEventsByTaskId(task.id());
        assertEquals(1, events.size());
        assertTrue(events.get(0).details().contains("WARNING: past deadline"));
    }

    @Test
    public void postponeOccurrence_updatesEffectiveDateLeavesOccurrenceDateUntouched() {
        LocalDate originalDate = today.plusDays(2);
        LocalDate targetDate = today.plusDays(4);

        TaskOccurrence occ = new TaskOccurrence(
                0L,
                100L,
                200L,
                originalDate,
                originalDate,
                OccurrenceStatus.OPEN,
                null,
                null,
                null,
                null,
                0,
                null,
                null,
                now,
                now
        );
        long id = repository.insertOccurrence(occ);

        postponementService.postponeOccurrence(id, targetDate, "Doctor appointment conflict");

        TaskOccurrence updated = repository.findOccurrenceById(id);
        assertEquals(originalDate, updated.occurrenceDate()); // original date preserved
        assertEquals(targetDate, updated.effectiveDate());    // effective date updated

        List<RescheduleRecord> history = repository.findRescheduleRecordsByOccurrenceId(id);
        assertEquals(1, history.size());
        assertEquals(originalDate, history.get(0).fromDate());
        assertEquals(targetDate, history.get(0).toDate());
        assertEquals("Doctor appointment conflict", history.get(0).reason());

        List<TaskEvent> events = repository.findTaskEventsByOccurrenceId(id);
        assertEquals(1, events.size());
        assertEquals("POSTPONED", events.get(0).eventType());
    }
}
