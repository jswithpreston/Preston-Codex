package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "review_schedule",
        foreignKeys = @ForeignKey(
                entity = KnowledgeEntryEntity.class,
                parentColumns = "id",
                childColumns = "entry_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = @Index(value = "next_review_date", name = "idx_rs_next_review_date")
)
public class ReviewScheduleEntity {

    @PrimaryKey
    @ColumnInfo(name = "entry_id")
    public long entryId;

    public int step;

    @NonNull
    @ColumnInfo(name = "next_review_date")
    public String nextReviewDate;

    @ColumnInfo(name = "last_reviewed_at")
    public long lastReviewedAt;

    @ColumnInfo(name = "review_count")
    public int reviewCount;
}
