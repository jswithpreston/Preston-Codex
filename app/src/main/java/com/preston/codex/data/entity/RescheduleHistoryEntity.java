package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "reschedule_history",
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
public class RescheduleHistoryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "task_id")
    public long taskId;

    @ColumnInfo(name = "occurrence_id")
    public Long occurrenceId;

    @ColumnInfo(name = "from_date")
    public String fromDate;

    @NonNull
    @ColumnInfo(name = "to_date")
    public String toDate;

    @NonNull
    public String reason;

    public long timestamp;

    public RescheduleHistoryEntity() {
    }
}
