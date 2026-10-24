package com.preston.codex.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.preston.codex.data.dao.ImportBatchDao;
import com.preston.codex.data.dao.ImportResultDao;
import com.preston.codex.data.dao.KnowledgeEntryDao;
import com.preston.codex.data.dao.KnowledgeRelationshipDao;
import com.preston.codex.data.dao.KnowledgeSourceDao;
import com.preston.codex.data.dao.KnowledgeTagDao;
import com.preston.codex.data.entity.ImportBatchEntity;
import com.preston.codex.data.entity.ImportResultEntity;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeEntryTagEntity;
import com.preston.codex.data.entity.KnowledgeRelationshipEntity;
import com.preston.codex.data.entity.KnowledgeSourceEntity;
import com.preston.codex.data.entity.KnowledgeTagEntity;

@Database(
        entities = {
                KnowledgeSourceEntity.class,
                KnowledgeEntryEntity.class,
                KnowledgeTagEntity.class,
                KnowledgeEntryTagEntity.class,
                KnowledgeRelationshipEntity.class,
                ImportBatchEntity.class,
                ImportResultEntity.class
        },
        version = 1,
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
                    ).addCallback(CALLBACK).build();
                }
            }
        }
        return INSTANCE;
    }
}
