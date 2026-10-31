package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.preston.codex.data.entity.ReminderStateEntity;

import java.util.List;

@Dao
public interface ReminderStateDao {

    @Insert
    long insert(ReminderStateEntity entity);

    @Update
    void update(ReminderStateEntity entity);

    @Query("DELETE FROM reminder_state WHERE owner_type = :ownerType AND owner_id = :ownerId")
    int deleteByOwner(String ownerType, long ownerId);

    @Query("SELECT * FROM reminder_state WHERE idempotency_key = :key LIMIT 1")
    ReminderStateEntity findByIdempotencyKey(String key);

    @Query("SELECT * FROM reminder_state WHERE owner_type = :ownerType AND owner_id = :ownerId AND status = 'PENDING'")
    List<ReminderStateEntity> findPendingByOwner(String ownerType, long ownerId);

    @Query("SELECT * FROM reminder_state WHERE status = 'PENDING' ORDER BY trigger_at ASC")
    List<ReminderStateEntity> findPending();

    @Query("UPDATE reminder_state SET status = 'CANCELLED' WHERE owner_type = :ownerType AND owner_id = :ownerId AND status = 'PENDING'")
    int cancelPendingByOwner(String ownerType, long ownerId);
}
