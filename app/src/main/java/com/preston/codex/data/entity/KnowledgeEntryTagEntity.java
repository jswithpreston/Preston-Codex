package com.preston.codex.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

@Entity(
        tableName = "knowledge_entry_tag",
        primaryKeys = {"entry_id", "tag_id"},
        foreignKeys = {
                @ForeignKey(
                        entity = KnowledgeEntryEntity.class,
                        parentColumns = "id",
                        childColumns = "entry_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = KnowledgeTagEntity.class,
                        parentColumns = "id",
                        childColumns = "tag_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index("tag_id")
        }
)
public class KnowledgeEntryTagEntity {

    @ColumnInfo(name = "entry_id")
    public long entryId;

    @ColumnInfo(name = "tag_id")
    public long tagId;

    public KnowledgeEntryTagEntity() {
    }

    @androidx.room.Ignore
    public KnowledgeEntryTagEntity(long entryId, long tagId) {
        this.entryId = entryId;
        this.tagId = tagId;
    }
}
