package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.preston.codex.data.entity.RecurrenceRuleEntity;

import java.util.List;

@Dao
public interface RecurrenceRuleDao {

    @Insert
    long insert(RecurrenceRuleEntity entity);

    @Update
    void update(RecurrenceRuleEntity entity);

    @Query("SELECT * FROM recurrence_rule WHERE id = :id LIMIT 1")
    RecurrenceRuleEntity findById(long id);

    @Query("SELECT * FROM recurrence_rule WHERE task_id = :taskId LIMIT 1")
    RecurrenceRuleEntity findByTaskId(long taskId);

    @Query("SELECT * FROM recurrence_rule ORDER BY id ASC")
    List<RecurrenceRuleEntity> findAll();
}
