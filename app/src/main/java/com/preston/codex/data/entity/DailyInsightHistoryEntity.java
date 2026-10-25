package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "daily_insight_history",
        foreignKeys = @ForeignKey(
                entity = KnowledgeEntryEntity.class,
                parentColumns = "id",
                childColumns = "entry_id",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = {
                @Index(value = "local_date", name = "idx_dih_local_date"),
                @Index(value = "entry_id", name = "idx_dih_entry_id")
        }
)
public class DailyInsightHistoryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "local_date")
    public String localDate;

    @ColumnInfo(name = "entry_id")
    public Long entryId;

    @NonNull
    public String reason;

    @ColumnInfo(name = "selected_at")
    public long selectedAt;
}
