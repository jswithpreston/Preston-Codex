package com.preston.codex.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.preston.codex.data.entity.RecurrenceRuleEntity;
import com.preston.codex.data.entity.ReminderStateEntity;
import com.preston.codex.data.entity.RescheduleHistoryEntity;
import com.preston.codex.data.entity.TaskEntity;
import com.preston.codex.data.entity.TaskEventEntity;
import com.preston.codex.data.entity.TaskOccurrenceEntity;
import com.preston.codex.data.mapper.TaskMapper;
import com.preston.codex.domain.tasks.model.MissedPolicy;
import com.preston.codex.domain.tasks.model.OccurrenceStatus;
import com.preston.codex.domain.tasks.model.RecurrenceRule;
import com.preston.codex.domain.tasks.model.RecurrenceType;
import com.preston.codex.domain.tasks.model.ReminderState;
import com.preston.codex.domain.tasks.model.RescheduleRecord;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskOccurrence;
import com.preston.codex.domain.tasks.model.TaskEvent;
import com.preston.codex.domain.tasks.model.TaskKind;
import com.preston.codex.domain.tasks.model.TaskPriority;
import com.preston.codex.domain.tasks.model.TaskStatus;

import org.junit.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public class TaskMapperTest {

    @Test
    public void task_roundTripMapping_preservesAllFields() {
        Task original = new Task(
                42L,
                TaskKind.ONE_OFF,
                "Study Algorithms",
                "Chapter 4 on Graphs",
                TaskStatus.IN_PROGRESS,
                10L,
                99L,
                "Reflection note",
                true,
                TaskPriority.HIGH,
                LocalDate.of(2026, 11, 15),
                45,
                LocalDate.of(2026, 11, 1),
                LocalTime.of(14, 30),
                1,
                null,
                null,
                Instant.ofEpochMilli(1000000L),
                Instant.ofEpochMilli(2000000L),
                Set.of("algorithms", "study")
        );

        TaskEntity entity = TaskMapper.toEntity(original);
        assertNotNull(entity);
        assertEquals(42L, entity.id);
        assertEquals("ONE_OFF", entity.taskKind);
        assertEquals("Study Algorithms", entity.title);
        assertEquals("Chapter 4 on Graphs", entity.description);
        assertEquals("IN_PROGRESS", entity.status);
        assertEquals(Long.valueOf(10L), entity.previousTaskId);
        assertEquals(Long.valueOf(99L), entity.knowledgeEntryId);
        assertEquals("Reflection note", entity.reflection);
        assertTrue(entity.isFlexible);
        assertEquals("HIGH", entity.priority);
        assertEquals("2026-11-15", entity.deadline);
        assertEquals(Integer.valueOf(45), entity.estimatedDurationMinutes);
        assertEquals("2026-11-01", entity.scheduledDate);
        assertEquals("14:30", entity.scheduledTime);
        assertEquals(1, entity.autoRescheduleCount);
        assertEquals(1000000L, entity.createdAt);
        assertEquals(2000000L, entity.updatedAt);

        Task mappedBack = TaskMapper.toDomain(entity, Set.of("algorithms", "study"));
        assertEquals(original, mappedBack);
    }

    @Test
    public void taskOccurrence_roundTripMapping_preservesAllFields() {
        TaskOccurrence original = new TaskOccurrence(
                101L,
                42L,
                5L,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 2),
                OccurrenceStatus.OPEN,
                "Overridden Title",
                LocalTime.of(9, 15),
                "Note override",
                "Occurrence reflection",
                2,
                null,
                null,
                Instant.ofEpochMilli(3000000L),
                Instant.ofEpochMilli(4000000L)
        );

        TaskOccurrenceEntity entity = TaskMapper.toEntity(original);
        assertNotNull(entity);
        assertEquals(101L, entity.id);
        assertEquals(42L, entity.taskId);
        assertEquals(5L, entity.ruleId);
        assertEquals("2026-11-01", entity.occurrenceDate);
        assertEquals("2026-11-02", entity.effectiveDate);
        assertEquals("OPEN", entity.status);
        assertEquals("Overridden Title", entity.titleOverride);
        assertEquals("09:15", entity.timeOverride);

        TaskOccurrence mappedBack = TaskMapper.toDomain(entity);
        assertEquals(original, mappedBack);
    }

    @Test
    public void recurrenceRule_roundTripMapping_preservesAllFields() {
        RecurrenceRule original = new RecurrenceRule(
                5L,
                42L,
                RecurrenceType.WEEKLY,
                2,
                "MON,WED,FRI",
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 12, 31),
                10,
                15,
                MissedPolicy.CARRY_FORWARD_FLEXIBLE_ONLY,
                Instant.ofEpochMilli(5000000L),
                Instant.ofEpochMilli(6000000L)
        );

        RecurrenceRuleEntity entity = TaskMapper.toEntity(original);
        assertNotNull(entity);
        assertEquals(5L, entity.id);
        assertEquals(42L, entity.taskId);
        assertEquals("WEEKLY", entity.type);
        assertEquals(2, entity.interval);
        assertEquals("MON,WED,FRI", entity.weekdays);
        assertEquals("2026-11-01", entity.startDate);
        assertEquals("2026-12-31", entity.endDate);
        assertEquals(Integer.valueOf(10), entity.occurrenceCount);
        assertEquals(Integer.valueOf(15), entity.anchorDayOfMonth);
        assertEquals("CARRY_FORWARD_FLEXIBLE_ONLY", entity.missedPolicy);

        RecurrenceRule mappedBack = TaskMapper.toDomain(entity);
        assertEquals(original, mappedBack);
    }

    @Test
    public void taskEvent_roundTripMapping_preservesAllFields() {
        TaskEvent original = new TaskEvent(
                7L,
                42L,
                101L,
                "STATUS_CHANGE",
                "Moved from OPEN to IN_PROGRESS",
                Instant.ofEpochMilli(7000000L)
        );

        TaskEventEntity entity = TaskMapper.toEntity(original);
        assertNotNull(entity);
        assertEquals(7L, entity.id);
        assertEquals(42L, entity.taskId);
        assertEquals(Long.valueOf(101L), entity.occurrenceId);
        assertEquals("STATUS_CHANGE", entity.eventType);
        assertEquals("Moved from OPEN to IN_PROGRESS", entity.details);

        TaskEvent mappedBack = TaskMapper.toDomain(entity);
        assertEquals(original, mappedBack);
    }

    @Test
    public void rescheduleRecord_roundTripMapping_preservesAllFields() {
        RescheduleRecord original = new RescheduleRecord(
                8L,
                42L,
                101L,
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 3),
                "Delayed by meeting",
                Instant.ofEpochMilli(8000000L)
        );

        RescheduleHistoryEntity entity = TaskMapper.toEntity(original);
        assertNotNull(entity);
        assertEquals(8L, entity.id);
        assertEquals(42L, entity.taskId);
        assertEquals("2026-11-01", entity.fromDate);
        assertEquals("2026-11-03", entity.toDate);
        assertEquals("Delayed by meeting", entity.reason);

        RescheduleRecord mappedBack = TaskMapper.toDomain(entity);
        assertEquals(original, mappedBack);
    }

    @Test
    public void reminderState_roundTripMapping_preservesAllFields() {
        ReminderState original = new ReminderState(
                9L,
                "TIMED_TASK",
                "TASK",
                42L,
                Instant.ofEpochMilli(9000000L),
                LocalDate.of(2026, 11, 1),
                LocalTime.of(8, 0),
                "DEFAULT",
                "PENDING",
                null,
                "KEY-12345",
                Instant.ofEpochMilli(8500000L)
        );

        ReminderStateEntity entity = TaskMapper.toEntity(original);
        assertNotNull(entity);
        assertEquals("KEY-12345", entity.idempotencyKey);
        assertEquals("2026-11-01", entity.triggerLocalDate);
        assertEquals("08:00", entity.triggerLocalTime);

        ReminderState mappedBack = TaskMapper.toDomain(entity);
        assertEquals(original, mappedBack);
    }

    @Test
    public void nullHandling_returnsNullSafely() {
        assertNull(TaskMapper.toEntity((Task) null));
        assertNull(TaskMapper.toDomain((TaskEntity) null, Set.of()));
        assertNull(TaskMapper.toEntity((TaskOccurrence) null));
        assertNull(TaskMapper.toDomain((TaskOccurrenceEntity) null));
        assertNull(TaskMapper.toEntity((RecurrenceRule) null));
        assertNull(TaskMapper.toDomain((RecurrenceRuleEntity) null));
        assertNull(TaskMapper.toEntity((TaskEvent) null));
        assertNull(TaskMapper.toDomain((TaskEventEntity) null));
        assertNull(TaskMapper.toEntity((RescheduleRecord) null));
        assertNull(TaskMapper.toDomain((RescheduleHistoryEntity) null));
        assertNull(TaskMapper.toEntity((ReminderState) null));
        assertNull(TaskMapper.toDomain((ReminderStateEntity) null));
    }
}
