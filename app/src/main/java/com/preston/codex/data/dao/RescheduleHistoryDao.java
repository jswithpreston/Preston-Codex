package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.preston.codex.data.entity.RescheduleHistoryEntity;

import java.util.List;

@Dao
public interface RescheduleHistoryDao {

    @Insert
    long insert(RescheduleHistoryEntity entity);

    @Query("SELECT * FROM reschedule_history WHERE task_id = :taskId ORDER BY timestamp ASC")
    List<RescheduleHistoryEntity> findByTaskId(long taskId);

    @Query("SELECT * FROM reschedule_history WHERE occurrence_id = :occurrenceId ORDER BY timestamp ASC")
    List<RescheduleHistoryEntity> findByOccurrenceId(long occurrenceId);

    @Query("SELECT COUNT(*) FROM reschedule_history WHERE task_id = :taskId")
    int countByTaskId(long taskId);
}
