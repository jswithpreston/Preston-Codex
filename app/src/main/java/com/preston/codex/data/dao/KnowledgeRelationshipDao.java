package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.preston.codex.data.entity.KnowledgeRelationshipEntity;

import java.util.List;

@Dao
public interface KnowledgeRelationshipDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(KnowledgeRelationshipEntity relationship);

    @Query("SELECT * FROM knowledge_relationship WHERE from_id = :fromId AND to_id = :toId AND rel_type = :relType LIMIT 1")
    KnowledgeRelationshipEntity findRelationship(long fromId, long toId, String relType);

    @Query("SELECT * FROM knowledge_relationship WHERE from_id = :entryId OR to_id = :entryId")
    List<KnowledgeRelationshipEntity> getRelationshipsForEntry(long entryId);

    @Query("DELETE FROM knowledge_relationship WHERE from_id = :fromId AND to_id = :toId AND rel_type = :relType")
    void deleteRelationship(long fromId, long toId, String relType);

    @Query("SELECT COUNT(*) FROM knowledge_relationship")
    int count();
}
