package com.preston.codex.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.database.Cursor;

import androidx.room.testing.MigrationTestHelper;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory;
import androidx.test.platform.app.InstrumentationRegistry;

import com.preston.codex.data.database.AppDatabase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class MigrationTest {

    private static final String TEST_DB = "migration-test.db";

    @Rule
    public MigrationTestHelper helper = new MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase.class,
            new ArrayList<>(),
            new FrameworkSQLiteOpenHelperFactory()
    );

    @Test
    public void migrate1To2_preservesAllV1DataAndValidatesV2Schema() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);

        // Enable and assert foreign keys
        db.execSQL("PRAGMA foreign_keys = ON;");
        try (Cursor c = db.query("PRAGMA foreign_keys;")) {
            assertTrue(c.moveToFirst());
            assertEquals(1, c.getInt(0));
        }

        // Seed v1 rows
        db.execSQL("INSERT INTO knowledge_source (id, title, author, title_key, author_key, source_type, created_at, updated_at) " +
                "VALUES (1, 'Source One', 'Author One', 'source one', 'author one', 'BOOK', 1000, 1000)");

        db.execSQL("INSERT INTO knowledge_entry (id, source_id, stable_id, fingerprint, content_hash, imported_hash, quote_text, entry_type, is_bookmarked, is_archived, is_trashed, created_at, updated_at) " +
                "VALUES (1, 1, 'STABLE-1', 'FP-1', 'CH-1', 'IH-1', 'Quote One', 'QUOTE', 0, 0, 0, 1000, 1000)");

        db.execSQL("INSERT INTO knowledge_tag (id, name) VALUES (1, 'philosophy')");
        db.execSQL("INSERT INTO knowledge_entry_tag (entry_id, tag_id) VALUES (1, 1)");

        db.execSQL("INSERT INTO knowledge_entry (id, source_id, stable_id, fingerprint, content_hash, imported_hash, quote_text, entry_type, is_bookmarked, is_archived, is_trashed, created_at, updated_at) " +
                "VALUES (2, 1, 'STABLE-2', 'FP-2', 'CH-2', 'IH-2', 'Quote Two', 'QUOTE', 0, 0, 0, 1000, 1000)");
        db.execSQL("INSERT INTO knowledge_relationship (id, from_id, to_id, rel_type, created_at) VALUES (1, 1, 2, 'RELATED', 1000)");

        db.close();

        // Migrate to v2 and validate schema against 2.json
        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2);

        // Assert v1 rows are intact
        try (Cursor c = db.query("SELECT title, author FROM knowledge_source WHERE id = 1")) {
            assertTrue(c.moveToFirst());
            assertEquals("Source One", c.getString(0));
            assertEquals("Author One", c.getString(1));
        }

        try (Cursor c = db.query("SELECT quote_text, stable_id FROM knowledge_entry WHERE id = 1")) {
            assertTrue(c.moveToFirst());
            assertEquals("Quote One", c.getString(0));
            assertEquals("STABLE-1", c.getString(1));
        }

        try (Cursor c = db.query("SELECT name FROM knowledge_tag WHERE id = 1")) {
            assertTrue(c.moveToFirst());
            assertEquals("philosophy", c.getString(0));
        }

        try (Cursor c = db.query("SELECT from_id, to_id FROM knowledge_relationship WHERE id = 1")) {
            assertTrue(c.moveToFirst());
            assertEquals(1, c.getLong(0));
            assertEquals(2, c.getLong(1));
        }
    }

    @Test
    public void foreignKey_deleteEntry_setsNullOnDailyInsightSelectionAndHistory_andCascadesReviewTables() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 1);
        db.close();

        db = helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase.MIGRATION_1_2);

        db.execSQL("PRAGMA foreign_keys = ON;");
        try (Cursor c = db.query("PRAGMA foreign_keys;")) {
            assertTrue(c.moveToFirst());
            assertEquals(1, c.getInt(0));
        }

        // Insert source and entry
        db.execSQL("INSERT INTO knowledge_source (id, title, author, title_key, author_key, source_type, created_at, updated_at) " +
                "VALUES (10, 'Source Ten', 'Author', 'source ten', 'author', 'BOOK', 1000, 1000)");
        db.execSQL("INSERT INTO knowledge_entry (id, source_id, stable_id, fingerprint, content_hash, imported_hash, quote_text, entry_type, is_bookmarked, is_archived, is_trashed, created_at, updated_at) " +
                "VALUES (10, 10, 'STABLE-10', 'FP-10', 'CH-10', 'IH-10', 'Quote Ten', 'QUOTE', 0, 0, 0, 1000, 1000)");

        // Insert into the 4 new v2 tables referencing entry 10
        db.execSQL("INSERT INTO daily_insight_selection (local_date, entry_id, selected_at, was_shuffled) VALUES ('2026-10-25', 10, 1000, 0)");
        db.execSQL("INSERT INTO daily_insight_history (local_date, entry_id, reason, selected_at) VALUES ('2026-10-25', 10, 'AUTO', 1000)");
        db.execSQL("INSERT INTO review_schedule (entry_id, step, next_review_date, last_reviewed_at, review_count) VALUES (10, 1, '2026-10-28', 1000, 1)");
        db.execSQL("INSERT INTO review_record (entry_id, reviewed_at, reviewed_local_date, rating, previous_step, previous_interval_days, previous_next_review_date, new_step, new_interval_days, next_review_date) " +
                "VALUES (10, 1000, '2026-10-25', 'REMEMBERED', 0, 1, NULL, 1, 3, '2026-10-28')");

        // Delete entry 10 from knowledge_entry
        db.execSQL("DELETE FROM knowledge_entry WHERE id = 10");

        // Verify ON DELETE SET NULL for daily_insight_selection
        try (Cursor c = db.query("SELECT entry_id FROM daily_insight_selection WHERE local_date = '2026-10-25'")) {
            assertTrue(c.moveToFirst());
            assertTrue("entry_id in daily_insight_selection must be set to NULL", c.isNull(0));
        }

        // Verify ON DELETE SET NULL for daily_insight_history
        try (Cursor c = db.query("SELECT entry_id FROM daily_insight_history WHERE local_date = '2026-10-25'")) {
            assertTrue(c.moveToFirst());
            assertTrue("entry_id in daily_insight_history must be set to NULL", c.isNull(0));
        }

        // Verify ON DELETE CASCADE for review_schedule
        try (Cursor c = db.query("SELECT COUNT(*) FROM review_schedule WHERE entry_id = 10")) {
            assertTrue(c.moveToFirst());
            assertEquals(0, c.getInt(0));
        }

        // Verify ON DELETE CASCADE for review_record
        try (Cursor c = db.query("SELECT COUNT(*) FROM review_record WHERE entry_id = 10")) {
            assertTrue(c.moveToFirst());
            assertEquals(0, c.getInt(0));
        }
    }

    @Test
    public void migrate2To3_preservesAllV2DataAndValidatesV3Schema() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);

        // Seed v2 rows
        db.execSQL("INSERT INTO knowledge_source (id, title, author, title_key, author_key, source_type, created_at, updated_at) " +
                "VALUES (100, 'Source 100', 'Author 100', 'source 100', 'author 100', 'BOOK', 2000, 2000)");

        db.execSQL("INSERT INTO knowledge_entry (id, source_id, stable_id, fingerprint, content_hash, imported_hash, quote_text, entry_type, is_bookmarked, is_archived, is_trashed, created_at, updated_at) " +
                "VALUES (100, 100, 'STABLE-100', 'FP-100', 'CH-100', 'IH-100', 'Quote 100', 'QUOTE', 0, 0, 0, 2000, 2000)");

        db.execSQL("INSERT INTO daily_insight_selection (local_date, entry_id, selected_at, was_shuffled) VALUES ('2026-10-30', 100, 2000, 0)");
        db.execSQL("INSERT INTO daily_insight_history (local_date, entry_id, reason, selected_at) VALUES ('2026-10-30', 100, 'AUTO', 2000)");
        db.execSQL("INSERT INTO review_schedule (entry_id, step, next_review_date, last_reviewed_at, review_count) VALUES (100, 2, '2026-11-05', 2000, 2)");
        db.execSQL("INSERT INTO review_record (id, entry_id, reviewed_at, reviewed_local_date, rating, previous_step, previous_interval_days, previous_next_review_date, new_step, new_interval_days, next_review_date) " +
                "VALUES (100, 100, 2000, '2026-10-30', 'REMEMBERED', 1, 3, '2026-10-30', 2, 7, '2026-11-05')");

        db.close();

        // Migrate to v3 and validate against 3.json
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3);

        // Verify v2 data is preserved
        try (Cursor c = db.query("SELECT title FROM knowledge_source WHERE id = 100")) {
            assertTrue(c.moveToFirst());
            assertEquals("Source 100", c.getString(0));
        }

        try (Cursor c = db.query("SELECT quote_text FROM knowledge_entry WHERE id = 100")) {
            assertTrue(c.moveToFirst());
            assertEquals("Quote 100", c.getString(0));
        }

        try (Cursor c = db.query("SELECT entry_id FROM daily_insight_selection WHERE local_date = '2026-10-30'")) {
            assertTrue(c.moveToFirst());
            assertEquals(100, c.getLong(0));
        }

        // Verify newly created v3 tables accept valid rows
        db.execSQL("INSERT INTO task (id, task_kind, title, status, is_flexible, priority, auto_reschedule_count, created_at, updated_at) " +
                "VALUES (1, 'ONE_OFF', 'Test Task', 'OPEN', 1, 'HIGH', 0, 3000, 3000)");

        try (Cursor c = db.query("SELECT title, priority FROM task WHERE id = 1")) {
            assertTrue(c.moveToFirst());
            assertEquals("Test Task", c.getString(0));
            assertEquals("HIGH", c.getString(1));
        }
    }

    @Test
    public void foreignKey_behaviorOnMigratedV3Database_withFkPragmaOn() throws IOException {
        SupportSQLiteDatabase db = helper.createDatabase(TEST_DB, 2);

        // Seed knowledge source and entry in v2
        db.execSQL("INSERT INTO knowledge_source (id, title, author, title_key, author_key, source_type, created_at, updated_at) " +
                "VALUES (50, 'Ref Source', 'Ref Author', 'ref source', 'ref author', 'BOOK', 1000, 1000)");

        db.execSQL("INSERT INTO knowledge_entry (id, source_id, stable_id, fingerprint, content_hash, imported_hash, quote_text, entry_type, is_bookmarked, is_archived, is_trashed, created_at, updated_at) " +
                "VALUES (50, 50, 'STABLE-50', 'FP-50', 'CH-50', 'IH-50', 'Quote 50', 'QUOTE', 0, 0, 0, 1000, 1000)");

        db.close();

        // Migrate to v3
        db = helper.runMigrationsAndValidate(TEST_DB, 3, true, AppDatabase.MIGRATION_2_3);

        // Assert FK pragma ON
        db.execSQL("PRAGMA foreign_keys = ON;");
        try (Cursor c = db.query("PRAGMA foreign_keys;")) {
            assertTrue(c.moveToFirst());
            assertEquals(1, c.getInt(0));
        }

        // Insert task linked to knowledge_entry 50 with reflection
        db.execSQL("INSERT INTO task (id, task_kind, title, status, knowledge_entry_id, reflection, is_flexible, priority, auto_reschedule_count, created_at, updated_at) " +
                "VALUES (500, 'ONE_OFF', 'Knowledge Task', 'OPEN', 50, 'Great reflection on life', 1, 'MEDIUM', 0, 2000, 2000)");

        // Insert tag for task 500
        db.execSQL("INSERT INTO task_tag (task_id, tag) VALUES (500, 'learning')");

        // Insert recurrence rule for task 500
        db.execSQL("INSERT INTO recurrence_rule (id, task_id, type, interval, start_date, missed_policy, created_at, updated_at) " +
                "VALUES (10, 500, 'DAILY', 1, '2026-10-30', 'KEEP_OVERDUE', 2000, 2000)");

        // Insert occurrence
        db.execSQL("INSERT INTO task_occurrence (id, task_id, rule_id, occurrence_date, effective_date, status, auto_reschedule_count, created_at, updated_at) " +
                "VALUES (1001, 500, 10, '2026-10-30', '2026-10-30', 'OPEN', 0, 2000, 2000)");

        // Insert event linked to task and occurrence
        db.execSQL("INSERT INTO task_event (id, task_id, occurrence_id, event_type, details, timestamp) " +
                "VALUES (1, 500, 1001, 'CREATED', 'Initial occurrence', 2000)");

        // Insert reschedule history linked to task and occurrence
        db.execSQL("INSERT INTO reschedule_history (id, task_id, occurrence_id, from_date, to_date, reason, timestamp) " +
                "VALUES (1, 500, 1001, '2026-10-29', '2026-10-30', 'Carried', 2000)");

        // TEST 1: Deleting knowledge entry nulls task.knowledge_entry_id and keeps the task and reflection
        db.execSQL("DELETE FROM knowledge_entry WHERE id = 50");

        try (Cursor c = db.query("SELECT id, title, knowledge_entry_id, reflection FROM task WHERE id = 500")) {
            assertTrue("Task row must still exist", c.moveToFirst());
            assertEquals(500, c.getLong(0));
            assertEquals("Knowledge Task", c.getString(1));
            assertTrue("knowledge_entry_id must be NULL", c.isNull(2));
            assertEquals("Great reflection on life", c.getString(3));
        }

        // TEST 2: Deleting a task cascades to tags, occurrences, events and history
        db.execSQL("DELETE FROM task WHERE id = 500");

        try (Cursor c = db.query("SELECT COUNT(*) FROM task_tag WHERE task_id = 500")) {
            assertTrue(c.moveToFirst());
            assertEquals(0, c.getInt(0));
        }

        try (Cursor c = db.query("SELECT COUNT(*) FROM recurrence_rule WHERE task_id = 500")) {
            assertTrue(c.moveToFirst());
            assertEquals(0, c.getInt(0));
        }

        try (Cursor c = db.query("SELECT COUNT(*) FROM task_occurrence WHERE task_id = 500")) {
            assertTrue(c.moveToFirst());
            assertEquals(0, c.getInt(0));
        }

        try (Cursor c = db.query("SELECT COUNT(*) FROM task_event WHERE task_id = 500")) {
            assertTrue(c.moveToFirst());
            assertEquals(0, c.getInt(0));
        }

        try (Cursor c = db.query("SELECT COUNT(*) FROM reschedule_history WHERE task_id = 500")) {
            assertTrue(c.moveToFirst());
            assertEquals(0, c.getInt(0));
        }
    }
}
