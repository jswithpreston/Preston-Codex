package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "recurrence_rule",
        foreignKeys = {
                @ForeignKey(
                        entity = TaskEntity.class,
                        parentColumns = "id",
                        childColumns = "task_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = "task_id")
        }
)
public class RecurrenceRuleEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "task_id")
    public long taskId;

    @NonNull
    public String type;

    public int interval;

    public String weekdays;

    @NonNull
    @ColumnInfo(name = "start_date")
    public String startDate;

    @ColumnInfo(name = "end_date")
    public String endDate;

    @ColumnInfo(name = "occurrence_count")
    public Integer occurrenceCount;

    @ColumnInfo(name = "anchor_day_of_month")
    public Integer anchorDayOfMonth;

    @NonNull
    @ColumnInfo(name = "missed_policy")
    public String missedPolicy;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public RecurrenceRuleEntity() {
    }
}
