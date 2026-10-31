package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.preston.codex.data.entity.TaskTagEntity;

import java.util.List;

@Dao
public interface TaskTagDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TaskTagEntity> tags);

    @Query("DELETE FROM task_tag WHERE task_id = :taskId")
    int deleteByTaskId(long taskId);

    @Query("SELECT tag FROM task_tag WHERE task_id = :taskId ORDER BY tag ASC")
    List<String> findTagsByTaskId(long taskId);
}
