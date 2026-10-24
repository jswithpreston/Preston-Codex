package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.preston.codex.data.entity.KnowledgeEntryTagEntity;
import com.preston.codex.data.entity.KnowledgeTagEntity;

import java.util.List;

@Dao
public interface KnowledgeTagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(KnowledgeTagEntity tag);

    @Query("SELECT * FROM knowledge_tag WHERE name = :name LIMIT 1")
    KnowledgeTagEntity findByName(String name);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertEntryTag(KnowledgeEntryTagEntity junction);

    @Query("SELECT t.name FROM knowledge_tag t INNER JOIN knowledge_entry_tag et ON t.id = et.tag_id WHERE et.entry_id = :entryId")
    List<String> getTagsForEntry(long entryId);

    @Query("SELECT COUNT(*) FROM knowledge_tag")
    int countTags();
}
