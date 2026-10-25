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
import com.preston.codex.data.dao.ReviewDao;
import com.preston.codex.data.entity.DailyInsightHistoryEntity;
import com.preston.codex.data.entity.DailyInsightSelectionEntity;
import com.preston.codex.data.entity.ImportBatchEntity;
import com.preston.codex.data.entity.ImportResultEntity;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeEntryTagEntity;
import com.preston.codex.data.entity.KnowledgeRelationshipEntity;
import com.preston.codex.data.entity.KnowledgeSourceEntity;
import com.preston.codex.data.entity.KnowledgeTagEntity;
import com.preston.codex.data.entity.ReviewRecordEntity;
import com.preston.codex.data.entity.ReviewScheduleEntity;

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
                ReviewRecordEntity.class
        },
        version = 2,
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
                    ).addMigrations(MIGRATION_1_2).addCallback(CALLBACK).build();
                }
            }
        }
        return INSTANCE;
    }
}
