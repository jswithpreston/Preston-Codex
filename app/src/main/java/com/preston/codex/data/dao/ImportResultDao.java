package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.preston.codex.data.entity.ImportResultEntity;

import java.util.List;

@Dao
public interface ImportResultDao {

    @Insert
    long insert(ImportResultEntity result);

    @Query("SELECT * FROM import_result WHERE existing_entry_id = :existingEntryId AND incoming_hash = :incomingHash AND resolution_status = 'PENDING' LIMIT 1")
    ImportResultEntity findPendingByExistingEntryAndHash(long existingEntryId, String incomingHash);

    @Query("SELECT * FROM import_result WHERE batch_id = :batchId ORDER BY entry_index ASC")
    List<ImportResultEntity> getByBatchId(long batchId);

    @Query("SELECT COUNT(*) FROM import_result WHERE resolution_status = 'PENDING'")
    int countPending();
}
