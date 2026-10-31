package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "reminder_state",
        indices = {
                @Index(value = "idempotency_key", unique = true),
                @Index(value = {"owner_type", "owner_id"}),
                @Index(value = "status"),
                @Index(value = "trigger_at")
        }
)
public class ReminderStateEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "reminder_type")
    public String reminderType;

    @NonNull
    @ColumnInfo(name = "owner_type")
    public String ownerType;

    @ColumnInfo(name = "owner_id")
    public long ownerId;

    @ColumnInfo(name = "trigger_at")
    public long triggerAt;

    @NonNull
    @ColumnInfo(name = "trigger_local_date")
    public String triggerLocalDate;

    @ColumnInfo(name = "trigger_local_time")
    public String triggerLocalTime;

    @NonNull
    public String channel;

    @NonNull
    public String status;

    @ColumnInfo(name = "fired_at")
    public Long firedAt;

    @NonNull
    @ColumnInfo(name = "idempotency_key")
    public String idempotencyKey;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    public ReminderStateEntity() {
    }
}
