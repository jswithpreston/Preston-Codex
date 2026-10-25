package com.preston.codex.data;

import static org.junit.Assert.assertEquals;
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
}
