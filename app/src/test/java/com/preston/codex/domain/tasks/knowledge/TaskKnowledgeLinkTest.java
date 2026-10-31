package com.preston.codex.domain.tasks.knowledge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;

import com.preston.codex.data.database.AppDatabase;
import com.preston.codex.domain.tasks.model.Task;
import com.preston.codex.domain.tasks.model.TaskPriority;
import com.preston.codex.domain.tasks.model.TaskStatus;
import com.preston.codex.domain.tasks.postpone.PostponementService;
import com.preston.codex.domain.tasks.service.CreateOneOffTaskCommand;
import com.preston.codex.domain.tasks.service.TaskService;
import com.preston.codex.domain.tasks.testutil.DirectTransactionRunner;
import com.preston.codex.domain.tasks.testutil.InMemoryTaskRepository;
import com.preston.codex.domain.time.LocalDateProvider;
import com.preston.codex.integration.KeyValueStore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class TaskKnowledgeLinkTest {

    private InMemoryTaskRepository memoryRepository;
    private LocalDateProvider dateProvider;
    private TaskService taskService;
    private PostponementService postponementService;
    private LocalDate today;
    private Instant now;

    private AppDatabase roomDb;

    @Before
    public void setUp() {
        memoryRepository = new InMemoryTaskRepository();
        today = LocalDate.of(2026, 5, 20);
        ZoneId zone = ZoneId.of("UTC");
        now = today.atStartOfDay(zone).toInstant();
        Clock clock = Clock.fixed(now, zone);
        dateProvider = new LocalDateProvider(clock, () -> zone);

        InMemoryKeyValueStore kvStore = new InMemoryKeyValueStore();
        DirectTransactionRunner runner = new DirectTransactionRunner();
        com.preston.codex.domain.tasks.recurrence.OccurrenceGenerator generator = new com.preston.codex.domain.tasks.recurrence.OccurrenceGenerator();
        com.preston.codex.domain.tasks.recurrence.RecurrenceService recurrenceService = new com.preston.codex.domain.tasks.recurrence.RecurrenceService(memoryRepository, generator, dateProvider, runner);
        taskService = new TaskService(memoryRepository, recurrenceService, dateProvider, kvStore, runner);
        postponementService = new PostponementService(memoryRepository, dateProvider, runner);

        roomDb = Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                AppDatabase.class
        ).allowMainThreadQueries().build();
    }

    @After
    public void tearDown() throws IOException {
        if (roomDb != null && roomDb.isOpen()) {
            roomDb.close();
        }
    }

    @Test
    public void linkAndUnlinkKnowledgeEntry_updatesTask() {
        CreateOneOffTaskCommand cmd = new CreateOneOffTaskCommand(
                "Study Seneca",
                "Read letters",
                TaskPriority.MEDIUM,
                null,
                30,
                today,
                null,
                false,
                null,
                null,
                Collections.emptySet()
        );
        Task task = taskService.createOneOffTask(cmd);
        assertNull(task.knowledgeEntryId());

        taskService.linkKnowledge(task.id(), 101L);
        Task linked = memoryRepository.findTaskById(task.id());
        assertEquals(Long.valueOf(101L), linked.knowledgeEntryId());

        taskService.unlinkKnowledge(task.id());
        Task unlinked = memoryRepository.findTaskById(task.id());
        assertNull(unlinked.knowledgeEntryId());
    }

    @Test
    public void reflection_persistsAndSurvivesPostponementAndCompletion() {
        CreateOneOffTaskCommand cmd = new CreateOneOffTaskCommand(
                "Read Epictetus",
                null,
                TaskPriority.HIGH,
                null,
                30,
                today,
                null,
                false,
                null,
                null,
                Collections.emptySet()
        );
        Task task = taskService.createOneOffTask(cmd);

        taskService.linkKnowledge(task.id(), 202L);
        taskService.setTaskReflection(task.id(), "Focus only on what you can control.");

        Task withReflection = memoryRepository.findTaskById(task.id());
        assertEquals("Focus only on what you can control.", withReflection.reflection());
        assertEquals(Long.valueOf(202L), withReflection.knowledgeEntryId());

        // Postpone task to tomorrow
        postponementService.postponeOneOffTask(task.id(), today.plusDays(1), "Need more time");
        Task postponed = memoryRepository.findTaskById(task.id());
        assertEquals("Focus only on what you can control.", postponed.reflection());
        assertEquals(Long.valueOf(202L), postponed.knowledgeEntryId());

        // Complete task
        taskService.completeTask(task.id());
        Task completed = memoryRepository.findTaskById(task.id());
        assertEquals(TaskStatus.COMPLETED, completed.status());
        assertEquals("Focus only on what you can control.", completed.reflection());
        assertEquals(Long.valueOf(202L), completed.knowledgeEntryId());
    }

    @Test
    public void roomForeignKey_deletingKnowledgeEntry_nullsKnowledgeEntryId_andPreservesTaskAndReflection() {
        SupportSQLiteDatabase sqlDb = roomDb.getOpenHelper().getWritableDatabase();

        // Enable foreign key enforcement
        sqlDb.execSQL("PRAGMA foreign_keys = ON;");

        // Insert Source & Entry
        sqlDb.execSQL("INSERT INTO knowledge_source (id, title, author, title_key, author_key, source_type, created_at, updated_at) " +
                "VALUES (50, 'Ref Source', 'Ref Author', 'ref source', 'ref author', 'BOOK', 1000, 1000)");

        sqlDb.execSQL("INSERT INTO knowledge_entry (id, source_id, stable_id, fingerprint, content_hash, imported_hash, quote_text, entry_type, is_bookmarked, is_archived, is_trashed, created_at, updated_at) " +
                "VALUES (50, 50, 'STABLE-50', 'FP-50', 'CH-50', 'IH-50', 'Quote 50', 'QUOTE', 0, 0, 0, 1000, 1000)");

        // Insert Task linked to KnowledgeEntry 50 with reflection
        sqlDb.execSQL("INSERT INTO task (id, task_kind, title, status, knowledge_entry_id, reflection, is_flexible, priority, auto_reschedule_count, created_at, updated_at) " +
                "VALUES (500, 'ONE_OFF', 'Knowledge Task', 'OPEN', 50, 'Action speaks louder than words.', 1, 'HIGH', 0, 2000, 2000)");

        // Verify inserted
        try (Cursor c = sqlDb.query("SELECT id, knowledge_entry_id, reflection FROM task WHERE id = 500")) {
            assertTrue(c.moveToFirst());
            assertEquals(50L, c.getLong(1));
            assertEquals("Action speaks louder than words.", c.getString(2));
        }

        // Delete knowledge entry
        sqlDb.execSQL("DELETE FROM knowledge_entry WHERE id = 50");

        // Verify task still exists, reflection preserved, knowledgeEntryId set to NULL (ON DELETE SET NULL)
        try (Cursor c = sqlDb.query("SELECT id, knowledge_entry_id, reflection FROM task WHERE id = 500")) {
            assertTrue(c.moveToFirst());
            assertTrue("knowledge_entry_id must be NULL", c.isNull(1));
            assertEquals("Action speaks louder than words.", c.getString(2));
        }
    }

    static class InMemoryKeyValueStore implements KeyValueStore {
        private final Map<String, Object> map = new HashMap<>();

        @Override public boolean getBoolean(String key, boolean defValue) { return defValue; }
        @Override public void putBoolean(String key, boolean value) {}
        @Override public int getInt(String key, int defValue) { return defValue; }
        @Override public void putInt(String key, int value) {}
        @Override public String getString(String key, String defValue) {
            Object val = map.get(key);
            return val instanceof String ? (String) val : defValue;
        }
        @Override public void putString(String key, String value) { map.put(key, value); }
    }
}
