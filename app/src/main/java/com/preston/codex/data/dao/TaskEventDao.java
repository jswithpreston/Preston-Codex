package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.preston.codex.data.entity.TaskEventEntity;

import java.util.List;

@Dao
public interface TaskEventDao {

    @Insert
    long insert(TaskEventEntity entity);

    @Query("SELECT * FROM task_event WHERE task_id = :taskId ORDER BY timestamp ASC")
    List<TaskEventEntity> findByTaskId(long taskId);

    @Query("SELECT * FROM task_event WHERE occurrence_id = :occurrenceId ORDER BY timestamp ASC")
    List<TaskEventEntity> findByOccurrenceId(long occurrenceId);

    @Query("SELECT COUNT(*) FROM task_event WHERE task_id = :taskId")
    int countByTaskId(long taskId);
}
