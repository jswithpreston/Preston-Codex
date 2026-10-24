package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.preston.codex.data.entity.ImportBatchEntity;

import java.util.List;

@Dao
public interface ImportBatchDao {

    @Insert
    long insert(ImportBatchEntity batch);

    @Update
    void update(ImportBatchEntity batch);

    @Query("SELECT * FROM import_batch WHERE id = :id LIMIT 1")
    ImportBatchEntity findById(long id);

    @Query("SELECT * FROM import_batch WHERE filename = :filename ORDER BY id DESC LIMIT 1")
    ImportBatchEntity findLatestByFilename(String filename);

    @Query("SELECT * FROM import_batch ORDER BY id DESC")
    List<ImportBatchEntity> getAllBatches();
}
