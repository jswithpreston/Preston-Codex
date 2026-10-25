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

    @Query("UPDATE knowledge_entry SET is_bookmarked = :isBookmarked WHERE id = :id")
    void updateBookmark(long id, boolean isBookmarked);

    @Query("SELECT id FROM knowledge_entry WHERE is_bookmarked = 1 AND is_archived = 0 AND is_trashed = 0")
    List<Long> findBookmarkedEntryIds();

    @Query("SELECT id FROM knowledge_entry WHERE is_archived = 0 AND is_trashed = 0 ORDER BY id ASC")
    List<Long> findEligibleEntryIds();

    @Query("SELECT COUNT(*) > 0 FROM knowledge_entry WHERE id = :id")
    boolean existsById(long id);

    @Query("SELECT COUNT(*) > 0 FROM knowledge_entry WHERE id = :id AND is_archived = 0 AND is_trashed = 0")
    boolean isEligibleById(long id);

    @Query("DELETE FROM knowledge_entry WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT COUNT(*) FROM knowledge_entry")
    int count();
}
