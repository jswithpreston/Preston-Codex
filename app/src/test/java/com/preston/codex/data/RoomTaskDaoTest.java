package com.preston.codex.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.preston.codex.data.database.AppDatabase;
import com.preston.codex.data.entity.RecurrenceRuleEntity;
import com.preston.codex.data.entity.ReminderStateEntity;
import com.preston.codex.data.entity.RescheduleHistoryEntity;
import com.preston.codex.data.entity.TaskEntity;
import com.preston.codex.data.entity.TaskEventEntity;
import com.preston.codex.data.entity.TaskOccurrenceEntity;
import com.preston.codex.data.entity.TaskTagEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class RoomTaskDaoTest {

    private AppDatabase db;

    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .addCallback(AppDatabase.CALLBACK)
                .build();
    }

    @After
    public void closeDb() throws IOException {
        db.close();
    }

    @Test
    public void insertAndFindTask_withAllFields() {
        TaskEntity task = new TaskEntity();
        task.taskKind = "ONE_OFF";
        task.title = "Read Chapter 1";
        task.description = "Deep study";
        task.status = "OPEN";
        task.isFlexible = true;
        task.priority = "HIGH";
        task.deadline = "2026-11-15";
        task.estimatedDurationMinutes = 45;
        task.scheduledDate = "2026-11-01";
        task.scheduledTime = "09:00";
        task.autoRescheduleCount = 0;
        task.createdAt = 1000L;
        task.updatedAt = 1000L;

        long id = db.taskDao().insert(task);
        assertTrue(id > 0);

        TaskEntity loaded = db.taskDao().findById(id);
        assertNotNull(loaded);
        assertEquals("ONE_OFF", loaded.taskKind);
        assertEquals("Read Chapter 1", loaded.title);
        assertEquals("Deep study", loaded.description);
        assertEquals("OPEN", loaded.status);
        assertTrue(loaded.isFlexible);
        assertEquals("HIGH", loaded.priority);
        assertEquals("2026-11-15", loaded.deadline);
        assertEquals(Integer.valueOf(45), loaded.estimatedDurationMinutes);
        assertEquals("2026-11-01", loaded.scheduledDate);
        assertEquals("09:00", loaded.scheduledTime);
    }

    @Test
    public void taskTags_cascadeDeleteWithTask() {
        TaskEntity task = new TaskEntity();
        task.taskKind = "ONE_OFF";
        task.title = "Task with tags";
        task.status = "OPEN";
        task.isFlexible = false;
        task.priority = "LOW";
        task.autoRescheduleCount = 0;
        task.createdAt = 1000L;
        task.updatedAt = 1000L;

        long taskId = db.taskDao().insert(task);

        db.taskTagDao().insertAll(List.of(
                new TaskTagEntity(taskId, "reading"),
                new TaskTagEntity(taskId, "habits")
        ));

        List<String> tags = db.taskTagDao().findTagsByTaskId(taskId);
        assertEquals(2, tags.size());

        db.taskDao().deleteById(taskId);

        List<String> tagsAfterDelete = db.taskTagDao().findTagsByTaskId(taskId);
        assertTrue(tagsAfterDelete.isEmpty());
    }

    @Test
    public void recurrenceRuleAndOccurrences_cascadeDeleteWithTask() {
        TaskEntity task = new TaskEntity();
        task.taskKind = "SERIES";
        task.title = "Weekly Review Series";
        task.status = "OPEN";
        task.isFlexible = false;
        task.priority = "MEDIUM";
        task.autoRescheduleCount = 0;
        task.createdAt = 1000L;
        task.updatedAt = 1000L;

        long taskId = db.taskDao().insert(task);

        RecurrenceRuleEntity rule = new RecurrenceRuleEntity();
        rule.taskId = taskId;
        rule.type = "WEEKLY";
        rule.interval = 1;
        rule.startDate = "2026-11-02";
        rule.missedPolicy = "KEEP_OVERDUE";
        rule.createdAt = 1000L;
        rule.updatedAt = 1000L;
        long ruleId = db.recurrenceRuleDao().insert(rule);

        TaskOccurrenceEntity occ = new TaskOccurrenceEntity();
        occ.taskId = taskId;
        occ.ruleId = ruleId;
        occ.occurrenceDate = "2026-11-02";
        occ.effectiveDate = "2026-11-02";
        occ.status = "OPEN";
        occ.autoRescheduleCount = 0;
        occ.createdAt = 1000L;
        occ.updatedAt = 1000L;
        long occId = db.taskOccurrenceDao().insert(occ);

        TaskEventEntity event = new TaskEventEntity();
        event.taskId = taskId;
        event.occurrenceId = occId;
        event.eventType = "CREATED";
        event.timestamp = 1000L;
        db.taskEventDao().insert(event);

        RescheduleHistoryEntity history = new RescheduleHistoryEntity();
        history.taskId = taskId;
        history.occurrenceId = occId;
        history.fromDate = "2026-11-01";
        history.toDate = "2026-11-02";
        history.reason = "Shifted";
        history.timestamp = 1000L;
        db.rescheduleHistoryDao().insert(history);

        assertEquals(1, db.taskOccurrenceDao().findByTaskId(taskId).size());
        assertEquals(1, db.taskEventDao().findByTaskId(taskId).size());
        assertEquals(1, db.rescheduleHistoryDao().findByTaskId(taskId).size());

        db.taskDao().deleteById(taskId);

        assertEquals(0, db.recurrenceRuleDao().findByTaskId(taskId) == null ? 0 : 1);
        assertEquals(0, db.taskOccurrenceDao().findByTaskId(taskId).size());
        assertEquals(0, db.taskEventDao().findByTaskId(taskId).size());
        assertEquals(0, db.rescheduleHistoryDao().findByTaskId(taskId).size());
    }

    @Test
    public void deletingOccurrence_setsOccurrenceIdToNullOnEventsAndHistory() {
        TaskEntity task = new TaskEntity();
        task.taskKind = "SERIES";
        task.title = "Occ Event Series";
        task.status = "OPEN";
        task.isFlexible = false;
        task.priority = "MEDIUM";
        task.autoRescheduleCount = 0;
        task.createdAt = 1000L;
        task.updatedAt = 1000L;
        long taskId = db.taskDao().insert(task);

        RecurrenceRuleEntity rule = new RecurrenceRuleEntity();
        rule.taskId = taskId;
        rule.type = "DAILY";
        rule.interval = 1;
        rule.startDate = "2026-11-01";
        rule.missedPolicy = "KEEP_OVERDUE";
        rule.createdAt = 1000L;
        rule.updatedAt = 1000L;
        long ruleId = db.recurrenceRuleDao().insert(rule);

        TaskOccurrenceEntity occ = new TaskOccurrenceEntity();
        occ.taskId = taskId;
        occ.ruleId = ruleId;
        occ.occurrenceDate = "2026-11-01";
        occ.effectiveDate = "2026-11-01";
        occ.status = "OPEN";
        occ.autoRescheduleCount = 0;
        occ.createdAt = 1000L;
        occ.updatedAt = 1000L;
        long occId = db.taskOccurrenceDao().insert(occ);

        TaskEventEntity event = new TaskEventEntity();
        event.taskId = taskId;
        event.occurrenceId = occId;
        event.eventType = "NOTE";
        event.timestamp = 1000L;
        long eventId = db.taskEventDao().insert(event);

        RescheduleHistoryEntity history = new RescheduleHistoryEntity();
        history.taskId = taskId;
        history.occurrenceId = occId;
        history.fromDate = "2026-10-31";
        history.toDate = "2026-11-01";
        history.reason = "Postponed";
        history.timestamp = 1000L;
        long histId = db.rescheduleHistoryDao().insert(history);

        // Delete only the occurrence
        db.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM task_occurrence WHERE id = " + occId);

        List<TaskEventEntity> events = db.taskEventDao().findByTaskId(taskId);
        assertEquals(1, events.size());
        assertNull("occurrence_id must be null on ON DELETE SET NULL", events.get(0).occurrenceId);

        List<RescheduleHistoryEntity> histories = db.rescheduleHistoryDao().findByTaskId(taskId);
        assertEquals(1, histories.size());
        assertNull("occurrence_id must be null on ON DELETE SET NULL", histories.get(0).occurrenceId);
    }

    @Test
    public void oneOffQueries_excludeSeriesTasks() {
        TaskEntity oneOff = new TaskEntity();
        oneOff.taskKind = "ONE_OFF";
        oneOff.title = "One Off";
        oneOff.status = "OPEN";
        oneOff.isFlexible = true;
        oneOff.priority = "HIGH";
        oneOff.scheduledDate = "2026-11-01";
        oneOff.autoRescheduleCount = 0;
        oneOff.createdAt = 1000L;
        oneOff.updatedAt = 1000L;
        db.taskDao().insert(oneOff);

        TaskEntity series = new TaskEntity();
        series.taskKind = "SERIES";
        series.title = "Series Parent";
        series.status = "OPEN";
        series.isFlexible = true;
        series.priority = "HIGH";
        series.scheduledDate = "2026-11-01";
        series.autoRescheduleCount = 0;
        series.createdAt = 1000L;
        series.updatedAt = 1000L;
        db.taskDao().insert(series);

        List<TaskEntity> oneOffs = db.taskDao().findOneOffTasksByScheduledDate("2026-11-01");
        assertEquals(1, oneOffs.size());
        assertEquals("One Off", oneOffs.get(0).title);

        List<TaskEntity> allOneOffs = db.taskDao().findAllOneOffTasks();
        assertEquals(1, allOneOffs.size());
        assertEquals("One Off", allOneOffs.get(0).title);
    }

    @Test
    public void uniqueOccurrenceConstraint_throwsOnDuplicateDateForSameTask() {
        TaskEntity task = new TaskEntity();
        task.taskKind = "SERIES";
        task.title = "Unique Constraint Task";
        task.status = "OPEN";
        task.isFlexible = false;
        task.priority = "LOW";
        task.autoRescheduleCount = 0;
        task.createdAt = 1000L;
        task.updatedAt = 1000L;
        long taskId = db.taskDao().insert(task);

        RecurrenceRuleEntity rule = new RecurrenceRuleEntity();
        rule.taskId = taskId;
        rule.type = "DAILY";
        rule.interval = 1;
        rule.startDate = "2026-11-01";
        rule.missedPolicy = "KEEP_OVERDUE";
        rule.createdAt = 1000L;
        rule.updatedAt = 1000L;
        long ruleId = db.recurrenceRuleDao().insert(rule);

        TaskOccurrenceEntity occ1 = new TaskOccurrenceEntity();
        occ1.taskId = taskId;
        occ1.ruleId = ruleId;
        occ1.occurrenceDate = "2026-11-01";
        occ1.effectiveDate = "2026-11-01";
        occ1.status = "OPEN";
        occ1.autoRescheduleCount = 0;
        occ1.createdAt = 1000L;
        occ1.updatedAt = 1000L;
        db.taskOccurrenceDao().insert(occ1);

        TaskOccurrenceEntity occ2 = new TaskOccurrenceEntity();
        occ2.taskId = taskId;
        occ2.ruleId = ruleId;
        occ2.occurrenceDate = "2026-11-01"; // Duplicate date!
        occ2.effectiveDate = "2026-11-02";
        occ2.status = "OPEN";
        occ2.autoRescheduleCount = 0;
        occ2.createdAt = 1000L;
        occ2.updatedAt = 1000L;

        try {
            db.taskOccurrenceDao().insert(occ2);
            fail("Expected SQLiteConstraintException on duplicate (task_id, occurrence_date)");
        } catch (SQLiteConstraintException expected) {
            // Success
        }
    }

    @Test
    public void reminderState_crudOperations() {
        ReminderStateEntity reminder = new ReminderStateEntity();
        reminder.reminderType = "TIMED_TASK";
        reminder.ownerType = "TASK";
        reminder.ownerId = 123L;
        reminder.triggerAt = 5000L;
        reminder.triggerLocalDate = "2026-11-01";
        reminder.triggerLocalTime = "08:00";
        reminder.channel = "CHANNEL_TASKS";
        reminder.status = "PENDING";
        reminder.idempotencyKey = "KEY-TEST-001";
        reminder.createdAt = 1000L;

        long id = db.reminderStateDao().insert(reminder);
        assertTrue(id > 0);

        ReminderStateEntity found = db.reminderStateDao().findByIdempotencyKey("KEY-TEST-001");
        assertNotNull(found);
        assertEquals(123L, found.ownerId);

        db.reminderStateDao().cancelPendingByOwner("TASK", 123L);
        found = db.reminderStateDao().findByIdempotencyKey("KEY-TEST-001");
        assertEquals("CANCELLED", found.status);

        db.reminderStateDao().deleteByOwner("TASK", 123L);
        found = db.reminderStateDao().findByIdempotencyKey("KEY-TEST-001");
        assertNull(found);
    }
}
