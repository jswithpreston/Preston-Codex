package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "task_occurrence",
        foreignKeys = {
                @ForeignKey(
                        entity = TaskEntity.class,
                        parentColumns = "id",
                        childColumns = "task_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = RecurrenceRuleEntity.class,
                        parentColumns = "id",
                        childColumns = "rule_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = {"task_id", "occurrence_date"}, unique = true),
                @Index(value = "task_id"),
                @Index(value = "rule_id"),
                @Index(value = "effective_date")
        }
)
public class TaskOccurrenceEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "task_id")
    public long taskId;

    @ColumnInfo(name = "rule_id")
    public long ruleId;

    @NonNull
    @ColumnInfo(name = "occurrence_date")
    public String occurrenceDate;

    @NonNull
    @ColumnInfo(name = "effective_date")
    public String effectiveDate;

    @NonNull
    public String status;

    @ColumnInfo(name = "title_override")
    public String titleOverride;

    @ColumnInfo(name = "time_override")
    public String timeOverride;

    @ColumnInfo(name = "note_override")
    public String noteOverride;

    public String reflection;

    @ColumnInfo(name = "auto_reschedule_count")
    public int autoRescheduleCount;

    @ColumnInfo(name = "completed_at")
    public Long completedAt;

    @ColumnInfo(name = "cancelled_at")
    public Long cancelledAt;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public TaskOccurrenceEntity() {
    }
}
