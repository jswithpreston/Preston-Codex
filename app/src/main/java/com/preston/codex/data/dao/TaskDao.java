package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.preston.codex.data.entity.TaskEntity;

import java.util.List;

@Dao
public interface TaskDao {

    @Insert
    long insert(TaskEntity entity);

    @Update
    void update(TaskEntity entity);

    @Query("SELECT * FROM task WHERE id = :id LIMIT 1")
    TaskEntity findById(long id);

    @Query("DELETE FROM task WHERE id = :id")
    int deleteById(long id);

    @Query("SELECT * FROM task WHERE task_kind = 'ONE_OFF' AND scheduled_date = :date ORDER BY id ASC")
    List<TaskEntity> findOneOffTasksByScheduledDate(String date);

    @Query("SELECT * FROM task WHERE task_kind = 'ONE_OFF' ORDER BY id ASC")
    List<TaskEntity> findAllOneOffTasks();

    @Query("SELECT * FROM task WHERE task_kind = 'ONE_OFF' AND (status = 'OPEN' OR status = 'IN_PROGRESS') AND scheduled_date < :today ORDER BY id ASC")
    List<TaskEntity> findOverdueOneOffTasks(String today);

    @Query("SELECT * FROM task WHERE task_kind = 'SERIES' ORDER BY id ASC")
    List<TaskEntity> findAllSeriesTasks();

    @Query("SELECT COUNT(*) FROM task")
    int count();
}
