package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.preston.codex.data.entity.TaskOccurrenceEntity;

import java.util.List;

@Dao
public interface TaskOccurrenceDao {

    @Insert
    long insert(TaskOccurrenceEntity entity);

    @Insert
    void insertAll(List<TaskOccurrenceEntity> entities);

    @Update
    void update(TaskOccurrenceEntity entity);

    @Query("SELECT * FROM task_occurrence WHERE id = :id LIMIT 1")
    TaskOccurrenceEntity findById(long id);

    @Query("SELECT * FROM task_occurrence WHERE task_id = :taskId AND occurrence_date = :occurrenceDate LIMIT 1")
    TaskOccurrenceEntity findByTaskIdAndOccurrenceDate(long taskId, String occurrenceDate);

    @Query("SELECT * FROM task_occurrence WHERE rule_id = :ruleId ORDER BY occurrence_date ASC")
    List<TaskOccurrenceEntity> findByRuleId(long ruleId);

    @Query("SELECT * FROM task_occurrence WHERE task_id = :taskId ORDER BY occurrence_date ASC")
    List<TaskOccurrenceEntity> findByTaskId(long taskId);

    @Query("SELECT * FROM task_occurrence WHERE effective_date = :effectiveDate ORDER BY id ASC")
    List<TaskOccurrenceEntity> findByEffectiveDate(String effectiveDate);

    @Query("SELECT * FROM task_occurrence WHERE effective_date < :today AND (status = 'OPEN' OR status = 'IN_PROGRESS') ORDER BY effective_date ASC")
    List<TaskOccurrenceEntity> findMissed(String today);

    @Query("DELETE FROM task_occurrence WHERE rule_id = :ruleId AND occurrence_date >= :fromDate AND status = 'OPEN' "
            + "AND id NOT IN (SELECT occurrence_id FROM task_event WHERE occurrence_id IS NOT NULL) "
            + "AND id NOT IN (SELECT occurrence_id FROM reschedule_history WHERE occurrence_id IS NOT NULL)")
    int deleteUntouchedFuture(long ruleId, String fromDate);

    @Query("SELECT COUNT(*) FROM task_occurrence WHERE rule_id = :ruleId")
    int countByRuleId(long ruleId);
}
