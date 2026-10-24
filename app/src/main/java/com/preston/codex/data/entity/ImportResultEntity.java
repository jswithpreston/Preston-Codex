package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "import_result",
        foreignKeys = {
                @ForeignKey(
                        entity = ImportBatchEntity.class,
                        parentColumns = "id",
                        childColumns = "batch_id",
                        onDelete = ForeignKey.CASCADE
                ),
                @ForeignKey(
                        entity = KnowledgeEntryEntity.class,
                        parentColumns = "id",
                        childColumns = "existing_entry_id",
                        onDelete = ForeignKey.SET_NULL
                ),
                @ForeignKey(
                        entity = KnowledgeEntryEntity.class,
                        parentColumns = "id",
                        childColumns = "inserted_entry_id",
                        onDelete = ForeignKey.SET_NULL
                )
        },
        indices = {
                @Index(value = {"batch_id", "entry_index"}, unique = true),
                @Index("existing_entry_id"),
                @Index("inserted_entry_id")
        }
)
public class ImportResultEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "batch_id")
    public long batchId;

    @ColumnInfo(name = "entry_index")
    public int entryIndex;

    @ColumnInfo(name = "stable_id")
    public String stableId;

    public String fingerprint;

    @NonNull
    public String classification;

    @ColumnInfo(name = "existing_entry_id")
    public Long existingEntryId;

    @ColumnInfo(name = "inserted_entry_id")
    public Long insertedEntryId;

    @ColumnInfo(name = "incoming_payload")
    public String incomingPayload;

    @ColumnInfo(name = "incoming_hash")
    public String incomingHash;

    @NonNull
    @ColumnInfo(name = "resolution_status")
    public String resolutionStatus = "PENDING";

    @ColumnInfo(name = "error_message")
    public String errorMessage;

    public ImportResultEntity() {
    }
}
