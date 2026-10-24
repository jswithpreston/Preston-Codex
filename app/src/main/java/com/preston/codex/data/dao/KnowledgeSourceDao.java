package com.preston.codex.data.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.preston.codex.data.entity.KnowledgeSourceEntity;

import java.util.List;

@Dao
public interface KnowledgeSourceDao {

    @Insert
    long insert(KnowledgeSourceEntity source);

    @Update
    void update(KnowledgeSourceEntity source);

    @Query("SELECT * FROM knowledge_source WHERE title_key = :titleKey AND author_key = :authorKey LIMIT 1")
    KnowledgeSourceEntity findByKeys(String titleKey, String authorKey);

    @Query("SELECT * FROM knowledge_source WHERE title_key = :titleKey")
    List<KnowledgeSourceEntity> findByTitleKey(String titleKey);

    @Query("SELECT * FROM knowledge_source WHERE id = :id LIMIT 1")
    KnowledgeSourceEntity findById(long id);

    @Query("SELECT COUNT(*) FROM knowledge_source")
    int count();
}
