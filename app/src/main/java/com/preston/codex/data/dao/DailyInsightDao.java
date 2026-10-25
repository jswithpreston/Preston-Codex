package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.preston.codex.data.entity.DailyInsightHistoryEntity;
import com.preston.codex.data.entity.DailyInsightSelectionEntity;

import java.util.List;

@Dao
public interface DailyInsightDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertSelection(DailyInsightSelectionEntity entity);

    @Update
    void updateSelection(DailyInsightSelectionEntity entity);

    @Query("SELECT * FROM daily_insight_selection WHERE local_date = :localDate LIMIT 1")
    DailyInsightSelectionEntity findSelectionByDate(String localDate);

    @Query("SELECT * FROM daily_insight_selection WHERE local_date < :beforeDate ORDER BY local_date DESC LIMIT 1")
    DailyInsightSelectionEntity findMostRecentPriorSelection(String beforeDate);

    @Insert
    long insertHistory(DailyInsightHistoryEntity entity);

    @Query("SELECT * FROM daily_insight_history WHERE local_date = :localDate ORDER BY id ASC")
    List<DailyInsightHistoryEntity> getHistoryByDate(String localDate);

    @Query("SELECT COUNT(*) FROM daily_insight_selection")
    int countSelections();

    @Query("SELECT COUNT(*) FROM daily_insight_history")
    int countHistory();
}
