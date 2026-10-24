package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "knowledge_relationship",
        foreignKeys = {
                @ForeignKey(
                        entity = KnowledgeEntryEntity.class,
                        parentColumns = "id",
                        childColumns = "from_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = KnowledgeEntryEntity.class,
                        parentColumns = "id",
                        childColumns = "to_id",
                        onDelete = ForeignKey.CASCADE
                )
        },
        indices = {
                @Index(value = {"from_id", "to_id", "rel_type"}, unique = true),
                @Index("to_id")
        }
)
public class KnowledgeRelationshipEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "from_id")
    public long fromId;

    @ColumnInfo(name = "to_id")
    public long toId;

    @NonNull
    @ColumnInfo(name = "rel_type")
    public String relType = "RELATED";

    @ColumnInfo(name = "created_at")
    public long createdAt;

    public KnowledgeRelationshipEntity() {
    }

    @androidx.room.Ignore
    public KnowledgeRelationshipEntity(long fromId, long toId, @NonNull String relType, long createdAt) {
        if (fromId >= toId) {
            throw new IllegalArgumentException("Normalized relationship requires from_id < to_id (" + fromId + " vs " + toId + ")");
        }
        this.fromId = fromId;
        this.toId = toId;
        this.relType = relType;
        this.createdAt = createdAt;
    }
}
