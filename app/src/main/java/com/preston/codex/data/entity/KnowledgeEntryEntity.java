package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "knowledge_entry",
        foreignKeys = {
                @ForeignKey(
                        entity = KnowledgeSourceEntity.class,
                        parentColumns = "id",
                        childColumns = "source_id",
                        onDelete = ForeignKey.RESTRICT
                ),
                @ForeignKey(
                        entity = ImportBatchEntity.class,
                        parentColumns = "id",
                        childColumns = "import_batch_id",
                        onDelete = ForeignKey.SET_NULL
                )
        },
        indices = {
                @Index(value = "fingerprint", unique = true),
                @Index(value = "stable_id", unique = true),
                @Index("source_id"),
                @Index("import_batch_id"),
                @Index("is_archived"),
                @Index("is_trashed"),
                @Index("is_bookmarked")
        }
)
public class KnowledgeEntryEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "source_id")
    public long sourceId;

    @ColumnInfo(name = "stable_id")
    public String stableId;

    @NonNull
    public String fingerprint;

    @NonNull
    @ColumnInfo(name = "content_hash")
    public String contentHash;

    @NonNull
    @ColumnInfo(name = "imported_hash")
    public String importedHash;

    @NonNull
    @ColumnInfo(name = "quote_text")
    public String quoteText;

    @ColumnInfo(name = "insight_text")
    public String insightText;

    @ColumnInfo(name = "source_ref")
    public String sourceRef;

    public String application;

    @NonNull
    @ColumnInfo(name = "entry_type")
    public String entryType = "QUOTE";

    @ColumnInfo(name = "review_mode")
    public String reviewMode;

    @ColumnInfo(name = "extra_fields")
    public String extraFields;

    @ColumnInfo(name = "raw_markdown")
    public String rawMarkdown;

    @ColumnInfo(name = "is_bookmarked")
    public boolean isBookmarked;

    @ColumnInfo(name = "is_archived")
    public boolean isArchived;

    @ColumnInfo(name = "is_trashed")
    public boolean isTrashed;

    @ColumnInfo(name = "trashed_at")
    public Long trashedAt;

    @ColumnInfo(name = "import_batch_id")
    public Long importBatchId;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public KnowledgeEntryEntity() {
    }
}
