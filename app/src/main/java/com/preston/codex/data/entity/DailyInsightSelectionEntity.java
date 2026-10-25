package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "daily_insight_selection",
        foreignKeys = @ForeignKey(
                entity = KnowledgeEntryEntity.class,
                parentColumns = "id",
                childColumns = "entry_id",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = @Index(value = "entry_id", name = "idx_dis_entry_id")
)
public class DailyInsightSelectionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "local_date")
    public String localDate;

    @ColumnInfo(name = "entry_id")
    public Long entryId;

    @ColumnInfo(name = "selected_at")
    public long selectedAt;

    @ColumnInfo(name = "was_shuffled")
    public boolean wasShuffled;
}
