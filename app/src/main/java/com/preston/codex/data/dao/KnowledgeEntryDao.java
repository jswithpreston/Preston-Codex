package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.preston.codex.data.entity.KnowledgeEntryEntity;

import java.util.List;

@Dao
public interface KnowledgeEntryDao {

    @Insert
    long insert(KnowledgeEntryEntity entry);

    @Update
    void update(KnowledgeEntryEntity entry);

    @Query("UPDATE knowledge_entry SET stable_id = :stableId, updated_at = :updatedAt WHERE id = :id")
    void updateStableId(long id, String stableId, long updatedAt);

    @Query("SELECT * FROM knowledge_entry WHERE id = :id LIMIT 1")
    KnowledgeEntryEntity findById(long id);

    @Query("SELECT * FROM knowledge_entry WHERE stable_id = :stableId LIMIT 1")
    KnowledgeEntryEntity findByStableId(String stableId);

    @Query("SELECT * FROM knowledge_entry WHERE fingerprint = :fingerprint LIMIT 1")
    KnowledgeEntryEntity findByFingerprint(String fingerprint);

    @Query("SELECT * FROM knowledge_entry")
    List<KnowledgeEntryEntity> getAllEntries();

    @Query("SELECT COUNT(*) FROM knowledge_entry")
    int count();
}
