package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "task_tag",
        primaryKeys = {"task_id", "tag"},
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
public class TaskTagEntity {

    @ColumnInfo(name = "task_id")
    public long taskId;

    @NonNull
    public String tag;

    public TaskTagEntity(long taskId, @NonNull String tag) {
        this.taskId = taskId;
        this.tag = tag;
    }
}
