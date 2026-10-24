package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "knowledge_tag",
        indices = {
                @Index(value = "name", unique = true)
        }
)
public class KnowledgeTagEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String name;

    public KnowledgeTagEntity() {
    }

    @androidx.room.Ignore
    public KnowledgeTagEntity(@NonNull String name) {
        this.name = name;
    }
}
