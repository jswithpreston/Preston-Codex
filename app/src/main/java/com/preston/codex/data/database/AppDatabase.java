package com.preston.codex.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.preston.codex.data.dao.DailyInsightDao;
import com.preston.codex.data.dao.ImportBatchDao;
import com.preston.codex.data.dao.ImportResultDao;
import com.preston.codex.data.dao.KnowledgeEntryDao;
import com.preston.codex.data.dao.KnowledgeRelationshipDao;
import com.preston.codex.data.dao.KnowledgeSourceDao;
import com.preston.codex.data.dao.KnowledgeTagDao;
import com.preston.codex.data.dao.RecurrenceRuleDao;
import com.preston.codex.data.dao.ReminderStateDao;
import com.preston.codex.data.dao.RescheduleHistoryDao;
import com.preston.codex.data.dao.ReviewDao;
import com.preston.codex.data.dao.TaskDao;
import com.preston.codex.data.dao.TaskEventDao;
import com.preston.codex.data.dao.TaskOccurrenceDao;
import com.preston.codex.data.dao.TaskTagDao;
import com.preston.codex.data.entity.DailyInsightHistoryEntity;
import com.preston.codex.data.entity.DailyInsightSelectionEntity;
import com.preston.codex.data.entity.ImportBatchEntity;
import com.preston.codex.data.entity.ImportResultEntity;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeEntryTagEntity;
import com.preston.codex.data.entity.KnowledgeRelationshipEntity;
import com.preston.codex.data.entity.KnowledgeSourceEntity;
import com.preston.codex.data.entity.KnowledgeTagEntity;
import com.preston.codex.data.entity.RecurrenceRuleEntity;
import com.preston.codex.data.entity.ReminderStateEntity;
import com.preston.codex.data.entity.RescheduleHistoryEntity;
import com.preston.codex.data.entity.ReviewRecordEntity;
import com.preston.codex.data.entity.ReviewScheduleEntity;
import com.preston.codex.data.entity.TaskEntity;
import com.preston.codex.data.entity.TaskEventEntity;
import com.preston.codex.data.entity.TaskOccurrenceEntity;
import com.preston.codex.data.entity.TaskTagEntity;

@Database(
        entities = {
                KnowledgeSourceEntity.class,
                KnowledgeEntryEntity.class,
                KnowledgeTagEntity.class,
                KnowledgeEntryTagEntity.class,
                KnowledgeRelationshipEntity.class,
                ImportBatchEntity.class,
                ImportResultEntity.class,
                DailyInsightSelectionEntity.class,
                DailyInsightHistoryEntity.class,
                ReviewScheduleEntity.class,
                ReviewRecordEntity.class,
                TaskEntity.class,
                TaskTagEntity.class,
                RecurrenceRuleEntity.class,
                TaskOccurrenceEntity.class,
                TaskEventEntity.class,
                RescheduleHistoryEntity.class,
                ReminderStateEntity.class
        },
        version = 3,
        exportSchema = true
)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "preston_codex.db";

    public abstract KnowledgeSourceDao knowledgeSourceDao();
    public abstract KnowledgeEntryDao knowledgeEntryDao();
    public abstract KnowledgeTagDao knowledgeTagDao();
    public abstract KnowledgeRelationshipDao knowledgeRelationshipDao();
    public abstract ImportBatchDao importBatchDao();
    public abstract ImportResultDao importResultDao();
    public abstract DailyInsightDao dailyInsightDao();
    public abstract ReviewDao reviewDao();
    public abstract TaskDao taskDao();
    public abstract TaskTagDao taskTagDao();
    public abstract RecurrenceRuleDao recurrenceRuleDao();
    public abstract TaskOccurrenceDao taskOccurrenceDao();
    public abstract TaskEventDao taskEventDao();
    public abstract RescheduleHistoryDao rescheduleHistoryDao();
    public abstract ReminderStateDao reminderStateDao();

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
                    + "FOREIGN KEY(`entry_id`) REFERENCES `knowledge_entry`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
                    + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_rr_entry_id` ON `review_record` (`entry_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `idx_rr_reviewed_local_date` ON `review_record` (`reviewed_local_date`)");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            // 1. task table
            db.execSQL("CREATE TABLE IF NOT EXISTS `task` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`task_kind` TEXT NOT NULL, "
                    + "`title` TEXT NOT NULL, "
                    + "`description` TEXT, "
                    + "`status` TEXT NOT NULL, "
                    + "`previous_task_id` INTEGER, "
                    + "`knowledge_entry_id` INTEGER, "
                    + "`reflection` TEXT, "
                    + "`is_flexible` INTEGER NOT NULL, "
                    + "`priority` TEXT NOT NULL, "
                    + "`deadline` TEXT, "
                    + "`estimated_duration_minutes` INTEGER, "
                    + "`scheduled_date` TEXT, "
                    + "`scheduled_time` TEXT, "
                    + "`auto_reschedule_count` INTEGER NOT NULL, "
                    + "`completed_at` INTEGER, "
                    + "`cancelled_at` INTEGER, "
                    + "`created_at` INTEGER NOT NULL, "
                    + "`updated_at` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`previous_task_id`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL, "
                    + "FOREIGN KEY(`knowledge_entry_id`) REFERENCES `knowledge_entry`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL"
                    + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_previous_task_id` ON `task` (`previous_task_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_knowledge_entry_id` ON `task` (`knowledge_entry_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_scheduled_date` ON `task` (`scheduled_date`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_deadline` ON `task` (`deadline`)");

            // 2. task_tag table
            db.execSQL("CREATE TABLE IF NOT EXISTS `task_tag` ("
                    + "`task_id` INTEGER NOT NULL, "
                    + "`tag` TEXT NOT NULL, "
                    + "PRIMARY KEY(`task_id`, `tag`), "
                    + "FOREIGN KEY(`task_id`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
                    + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_tag_task_id` ON `task_tag` (`task_id`)");

            // 3. recurrence_rule table
            db.execSQL("CREATE TABLE IF NOT EXISTS `recurrence_rule` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`task_id` INTEGER NOT NULL, "
                    + "`type` TEXT NOT NULL, "
                    + "`interval` INTEGER NOT NULL, "
                    + "`weekdays` TEXT, "
                    + "`start_date` TEXT NOT NULL, "
                    + "`end_date` TEXT, "
                    + "`occurrence_count` INTEGER, "
                    + "`anchor_day_of_month` INTEGER, "
                    + "`missed_policy` TEXT NOT NULL, "
                    + "`created_at` INTEGER NOT NULL, "
                    + "`updated_at` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`task_id`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
                    + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recurrence_rule_task_id` ON `recurrence_rule` (`task_id`)");

            // 4. task_occurrence table
            db.execSQL("CREATE TABLE IF NOT EXISTS `task_occurrence` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`task_id` INTEGER NOT NULL, "
                    + "`rule_id` INTEGER NOT NULL, "
                    + "`occurrence_date` TEXT NOT NULL, "
                    + "`effective_date` TEXT NOT NULL, "
                    + "`status` TEXT NOT NULL, "
                    + "`title_override` TEXT, "
                    + "`time_override` TEXT, "
                    + "`note_override` TEXT, "
                    + "`reflection` TEXT, "
                    + "`auto_reschedule_count` INTEGER NOT NULL, "
                    + "`completed_at` INTEGER, "
                    + "`cancelled_at` INTEGER, "
                    + "`created_at` INTEGER NOT NULL, "
                    + "`updated_at` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`task_id`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, "
                    + "FOREIGN KEY(`rule_id`) REFERENCES `recurrence_rule`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE"
                    + ")");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_task_occurrence_task_id_occurrence_date` ON `task_occurrence` (`task_id`, `occurrence_date`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_occurrence_task_id` ON `task_occurrence` (`task_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_occurrence_rule_id` ON `task_occurrence` (`rule_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_occurrence_effective_date` ON `task_occurrence` (`effective_date`)");

            // 5. task_event table
            db.execSQL("CREATE TABLE IF NOT EXISTS `task_event` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`task_id` INTEGER NOT NULL, "
                    + "`occurrence_id` INTEGER, "
                    + "`event_type` TEXT NOT NULL, "
                    + "`details` TEXT, "
                    + "`timestamp` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`task_id`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, "
                    + "FOREIGN KEY(`occurrence_id`) REFERENCES `task_occurrence`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL"
                    + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_event_task_id` ON `task_event` (`task_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_event_occurrence_id` ON `task_event` (`occurrence_id`)");

            // 6. reschedule_history table
            db.execSQL("CREATE TABLE IF NOT EXISTS `reschedule_history` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`task_id` INTEGER NOT NULL, "
                    + "`occurrence_id` INTEGER, "
                    + "`from_date` TEXT, "
                    + "`to_date` TEXT NOT NULL, "
                    + "`reason` TEXT NOT NULL, "
                    + "`timestamp` INTEGER NOT NULL, "
                    + "FOREIGN KEY(`task_id`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE, "
                    + "FOREIGN KEY(`occurrence_id`) REFERENCES `task_occurrence`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL"
                    + ")");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reschedule_history_task_id` ON `reschedule_history` (`task_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reschedule_history_occurrence_id` ON `reschedule_history` (`occurrence_id`)");

            // 7. reminder_state table
            db.execSQL("CREATE TABLE IF NOT EXISTS `reminder_state` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`reminder_type` TEXT NOT NULL, "
                    + "`owner_type` TEXT NOT NULL, "
                    + "`owner_id` INTEGER NOT NULL, "
                    + "`trigger_at` INTEGER NOT NULL, "
                    + "`trigger_local_date` TEXT NOT NULL, "
                    + "`trigger_local_time` TEXT, "
                    + "`channel` TEXT NOT NULL, "
                    + "`status` TEXT NOT NULL, "
                    + "`fired_at` INTEGER, "
                    + "`idempotency_key` TEXT NOT NULL, "
                    + "`created_at` INTEGER NOT NULL"
                    + ")");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reminder_state_idempotency_key` ON `reminder_state` (`idempotency_key`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_state_owner_type_owner_id` ON `reminder_state` (`owner_type`, `owner_id`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_state_status` ON `reminder_state` (`status`)");
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_state_trigger_at` ON `reminder_state` (`trigger_at`)");
        }
    };

    public static final Callback CALLBACK = new Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    };

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Locked decision #10: NEVER call fallbackToDestructiveMigration()
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).addCallback(CALLBACK).build();
                }
            }
        }
        return INSTANCE;
    }
}
