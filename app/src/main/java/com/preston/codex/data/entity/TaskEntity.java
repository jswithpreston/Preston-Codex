package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "task",
        foreignKeys = {
                @ForeignKey(
                        entity = TaskEntity.class,
                        parentColumns = "id",
                        childColumns = "previous_task_id",
                        onDelete = ForeignKey.SET_NULL
                ),
                @ForeignKey(
                        entity = KnowledgeEntryEntity.class,
                        parentColumns = "id",
                        childColumns = "knowledge_entry_id",
                        onDelete = ForeignKey.SET_NULL
                )
        },
        indices = {
                @Index(value = "previous_task_id"),
                @Index(value = "knowledge_entry_id"),
                @Index(value = "scheduled_date"),
                @Index(value = "deadline")
        }
)
public class TaskEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "task_kind")
    public String taskKind;

    @NonNull
    public String title;

    public String description;

    @NonNull
    public String status;

    @ColumnInfo(name = "previous_task_id")
    public Long previousTaskId;

    @ColumnInfo(name = "knowledge_entry_id")
    public Long knowledgeEntryId;

    public String reflection;

    @ColumnInfo(name = "is_flexible")
    public boolean isFlexible;

    @NonNull
    public String priority;

    public String deadline;

    @ColumnInfo(name = "estimated_duration_minutes")
    public Integer estimatedDurationMinutes;

    @ColumnInfo(name = "scheduled_date")
    public String scheduledDate;

    @ColumnInfo(name = "scheduled_time")
    public String scheduledTime;

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

    public TaskEntity() {
    }
}
