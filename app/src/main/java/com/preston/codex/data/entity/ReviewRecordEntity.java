package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "review_record",
        foreignKeys = @ForeignKey(
                entity = KnowledgeEntryEntity.class,
                parentColumns = "id",
                childColumns = "entry_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = "entry_id", name = "idx_rr_entry_id"),
                @Index(value = "reviewed_local_date", name = "idx_rr_reviewed_local_date")
        }
)
public class ReviewRecordEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "entry_id")
    public long entryId;

    @ColumnInfo(name = "reviewed_at")
    public long reviewedAt;

    @NonNull
    @ColumnInfo(name = "reviewed_local_date")
    public String reviewedLocalDate;

    @NonNull
    public String rating;

    @ColumnInfo(name = "previous_step")
    public int previousStep;

    @ColumnInfo(name = "previous_interval_days")
    public int previousIntervalDays;

    @ColumnInfo(name = "previous_next_review_date")
    public String previousNextReviewDate;

    @ColumnInfo(name = "new_step")
    public int newStep;

    @ColumnInfo(name = "new_interval_days")
    public int newIntervalDays;

    @NonNull
    @ColumnInfo(name = "next_review_date")
    public String nextReviewDate;
}
