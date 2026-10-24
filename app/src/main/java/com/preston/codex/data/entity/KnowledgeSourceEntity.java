package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "knowledge_source",
        indices = {
                @Index(value = {"title_key", "author_key"}, unique = true),
                @Index("author_key")
        }
)
public class KnowledgeSourceEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String title;

    @NonNull
    public String author = "";

    @NonNull
    @ColumnInfo(name = "title_key")
    public String titleKey;

    @NonNull
    @ColumnInfo(name = "author_key")
    public String authorKey = "";

    @NonNull
    @ColumnInfo(name = "source_type")
    public String sourceType = "BOOK";

    public String publisher;
    public Integer year;
    public String isbn;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public KnowledgeSourceEntity() {
    }

    @androidx.room.Ignore
    public KnowledgeSourceEntity(@NonNull String title,
                                 @NonNull String author,
                                 @NonNull String titleKey,
                                 @NonNull String authorKey,
                                 @NonNull String sourceType,
                                 String publisher,
                                 Integer year,
                                 String isbn,
                                 long createdAt,
                                 long updatedAt) {
        this.title = title;
        this.author = author;
        this.titleKey = titleKey;
        this.authorKey = authorKey;
        this.sourceType = sourceType;
        this.publisher = publisher;
        this.year = year;
        this.isbn = isbn;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
