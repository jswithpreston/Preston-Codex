# Phase 2 Implementation Plan: Insight Engine (Approved with Amendments)

> **Spec Scope:** Sections 7 (Daily Insight Selection), 8 (Active Recall & Spaced Repetition), 9 (Explicit Related-Entry Links & Bookmarks).  
> **Architecture Rules:** All business logic in `com.preston.codex.domain.*` (pure Java, zero `android.*` imports, JVM-testable with injected `Clock` and `Supplier<ZoneId>`). Room persistence in `com.preston.codex.data.*` (Schema v2 with non-destructive migration). Tasks are deferred to Phase 4.

---

## 1. Goal Description

Phase 2 builds the core reasoning engines of Preston Codex:
1. **Daily Insight Selection Engine (§7, Locked Decision #4):** Atomic daily insight selection, date rollover handling with dynamic `Supplier<ZoneId>`, shuffle mechanics with append-only history recording the entry that became selected, and eligibility filtering (excluding archived/trashed entries). Re-selects if current selection is invalidated or entry is deleted.
2. **Active Recall & Spaced Repetition Engine (§8, Locked Decision #5):** Deterministic zero-based interval-ladder review scheduling (`FORGOT` &rarr; 0, `DIFFICULT` &rarr; `max(0, step - 1)`, `REMEMBERED` &rarr; `min(lastStep, step + 1)`, `EASY` &rarr; `min(lastStep, step + 2)`). Due queue sorting (`daysOverdue` descending, then previously-reviewed before never-reviewed, then `step` ascending, then `entryId` ascending). Daily review limit net of reviews already completed today based on `reviewed_local_date`. First review of a never-reviewed entry records `previous_step = 0`, `previous_interval_days = 0`, `previous_next_review_date = null`.
3. **Bookmarks (§7.4):** Persistent bookmarking of knowledge entries without altering `updated_at`, `fingerprint`, `stable_id`, `content_hash`, or `imported_hash`.
4. **Explicit Knowledge Relationships (§9, §4.5):** Idempotent undirected normalized relationship linking (`from_id < to_id`) between knowledge entries.

---

## 2. Exact SQLite DDL for Schema Version 2

Room-generated `app/schemas/.../2.json` is the schema authority; no `DEFAULT` clauses in migration or entities:

```sql
-- 1. daily_insight_selection
-- Canonical single row per local date; local_date is PK.
CREATE TABLE IF NOT EXISTS `daily_insight_selection` (
    `local_date`   TEXT NOT NULL,
    `entry_id`     INTEGER,
    `selected_at`  INTEGER NOT NULL,
    `was_shuffled` INTEGER NOT NULL,
    PRIMARY KEY(`local_date`),
    FOREIGN KEY(`entry_id`) REFERENCES `knowledge_entry`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS `idx_dis_entry_id` ON `daily_insight_selection` (`entry_id`);

-- 2. daily_insight_history
-- Append-only audit log recording the entry that became selected plus reason.
CREATE TABLE IF NOT EXISTS `daily_insight_history` (
    `id`          INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `local_date`  TEXT NOT NULL,
    `entry_id`    INTEGER,
    `reason`      TEXT NOT NULL,
    `selected_at` INTEGER NOT NULL,
    FOREIGN KEY(`entry_id`) REFERENCES `knowledge_entry`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS `idx_dih_local_date` ON `daily_insight_history` (`local_date`);
CREATE INDEX IF NOT EXISTS `idx_dih_entry_id`   ON `daily_insight_history` (`entry_id`);

-- 3. review_schedule
-- Current spaced repetition state; entry_id is PK.
CREATE TABLE IF NOT EXISTS `review_schedule` (
    `entry_id`         INTEGER NOT NULL,
    `step`             INTEGER NOT NULL,
    `next_review_date` TEXT NOT NULL,
    `last_reviewed_at` INTEGER NOT NULL,
    `review_count`     INTEGER NOT NULL,
    PRIMARY KEY(`entry_id`),
    FOREIGN KEY(`entry_id`) REFERENCES `knowledge_entry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `idx_rs_next_review_date` ON `review_schedule` (`next_review_date`);

-- 4. review_record
-- Immutable log of each completed review.
CREATE TABLE IF NOT EXISTS `review_record` (
    `id`                        INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `entry_id`                  INTEGER NOT NULL,
    `reviewed_at`               INTEGER NOT NULL,
    `reviewed_local_date`       TEXT NOT NULL,
    `rating`                    TEXT NOT NULL,
    `previous_step`             INTEGER NOT NULL,
    `previous_interval_days`    INTEGER NOT NULL,
    `previous_next_review_date` TEXT,
    `new_step`                  INTEGER NOT NULL,
    `new_interval_days`         INTEGER NOT NULL,
    `next_review_date`          TEXT NOT NULL,
    PRIMARY KEY(`id`),
    FOREIGN KEY(`entry_id`) REFERENCES `knowledge_entry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `idx_rr_entry_id` ON `review_record` (`entry_id`);
CREATE INDEX IF NOT EXISTS `idx_rr_reviewed_local_date` ON `review_record` (`reviewed_local_date`);
```

---

## 3. Room Migration MIGRATION_1_2

```java
public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override
    public void migrate(@NonNull SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_insight_selection` ("
                + "`local_date` TEXT NOT NULL, "
                + "`entry_id` INTEGER, "
                + "`selected_at` INTEGER NOT NULL, "
                + "`was_shuffled` INTEGER NOT NULL, "
                + "PRIMARY KEY(`local_date`), "
                + "FOREIGN KEY(`entry_id`) REFERENCES `knowledge_entry`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL"
                + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_dis_entry_id` ON `daily_insight_selection` (`entry_id`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `daily_insight_history` ("
                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`local_date` TEXT NOT NULL, "
                + "`entry_id` INTEGER, "
                + "`reason` TEXT NOT NULL, "
                + "`selected_at` INTEGER NOT NULL, "
                + "FOREIGN KEY(`entry_id`) REFERENCES `knowledge_entry`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL"
                + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_dih_local_date` ON `daily_insight_history` (`local_date`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_dih_entry_id` ON `daily_insight_history` (`entry_id`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `review_schedule` ("
                + "`entry_id` INTEGER NOT NULL, "
                + "`step` INTEGER NOT NULL, "
                + "`next_review_date` TEXT NOT NULL, "
                + "`last_reviewed_at` INTEGER NOT NULL, "
                + "`review_count` INTEGER NOT NULL, "
                + "PRIMARY KEY(`entry_id`), "
                + "FOREIGN KEY(`entry_id`) REFERENCES `knowledge_entry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
                + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_rs_next_review_date` ON `review_schedule` (`next_review_date`)");

        db.execSQL("CREATE TABLE IF NOT EXISTS `review_record` ("
                + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                + "`entry_id` INTEGER NOT NULL, "
                + "`reviewed_at` INTEGER NOT NULL, "
                + "`reviewed_local_date` TEXT NOT NULL, "
                + "`rating` TEXT NOT NULL, "
                + "`previous_step` INTEGER NOT NULL, "
                + "`previous_interval_days` INTEGER NOT NULL, "
                + "`previous_next_review_date` TEXT, "
                + "`new_step` INTEGER NOT NULL, "
                + "`new_interval_days` INTEGER NOT NULL, "
                + "`next_review_date` TEXT NOT NULL, "
                + "PRIMARY KEY(`id`), "
                + "FOREIGN KEY(`entry_id`) REFERENCES `knowledge_entry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
                + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_rr_entry_id` ON `review_record` (`entry_id`)");
        db.execSQL("CREATE INDEX IF NOT EXISTS `idx_rr_reviewed_local_date` ON `review_record` (`reviewed_local_date`)");
    }
};
```

---

## 4. Migration Test Approach

- Run under Robolectric on JVM with pinned `androidx.test:core:1.6.1` and `androidx.test.ext:junit:1.2.1`.
- `MigrationTest` executes `PRAGMA foreign_keys = ON;` and asserts foreign keys are enabled before testing `SET NULL` and `CASCADE`.
- Creates real v1 DB from `app/schemas/.../1.json`, seeds data, applies `MIGRATION_1_2`, and validates schema against `app/schemas/.../2.json`.
- Asserts all v1 data preserved and foreign key rules function correctly.

---

## 5. Test Manifest

1. **ArchitectureRulesTest**: 1 test.
2. **DailyInsightEngineTest**: 14 tests.
3. **ReviewSchedulerTest**: 18 tests.
4. **BookmarkServiceTest**: 3 tests.
5. **RelationshipServiceTest**: 6 tests.
6. **MigrationTest**: 3 tests.
7. **RoomDailyInsightDaoTest**: 4 tests (including race condition insertOrIgnore test).
8. **RoomReviewDaoTest**: 4 tests (including reviewed_local_date count and zone test).
9. **RoomBookmarkIntegrityTest**: 1 test.

**Total Test Classes:** 9  
**Total Test Methods:** ~54 tests
