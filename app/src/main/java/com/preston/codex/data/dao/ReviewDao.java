package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.preston.codex.data.entity.ReviewRecordEntity;
import com.preston.codex.data.entity.ReviewScheduleEntity;

import java.util.List;

@Dao
public interface ReviewDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsertSchedule(ReviewScheduleEntity entity);

    @Query("SELECT * FROM review_schedule WHERE entry_id = :entryId LIMIT 1")
    ReviewScheduleEntity findScheduleByEntryId(long entryId);

    @Query("SELECT * FROM review_schedule WHERE next_review_date <= :asOfDate")
    List<ReviewScheduleEntity> findSchedulesDueOnOrBefore(String asOfDate);

    @Insert
    long insertRecord(ReviewRecordEntity entity);

    @Query("SELECT COUNT(*) FROM review_record WHERE reviewed_local_date = :localDate")
    int countRecordsForDate(String localDate);

    @Query("SELECT * FROM review_record WHERE entry_id = :entryId ORDER BY id ASC")
    List<ReviewRecordEntity> findRecordsByEntryId(long entryId);

    @Query("SELECT COUNT(*) FROM review_schedule")
    int countSchedules();

    @Query("SELECT COUNT(*) FROM review_record")
    int countRecords();
}
