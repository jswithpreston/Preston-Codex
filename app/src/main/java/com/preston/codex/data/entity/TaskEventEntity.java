package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "task_event",
        foreignKeys = {
                @ForeignKey(
                        entity = TaskEntity.class,
                        parentColumns = "id",
                        childColumns = "task_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = TaskOccurrenceEntity.class,
                        parentColumns = "id",
                        childColumns = "occurrence_id",
                        onDelete = ForeignKey.SET_NULL
                )
        },
        indices = {
                @Index(value = "task_id"),
                @Index(value = "occurrence_id")
        }
)
public class TaskEventEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "task_id")
    public long taskId;

    @ColumnInfo(name = "occurrence_id")
    public Long occurrenceId;

    @NonNull
    @ColumnInfo(name = "event_type")
    public String eventType;

    public String details;

    public long timestamp;

    public TaskEventEntity() {
    }
}
