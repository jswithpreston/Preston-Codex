package com.preston.codex.data.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "import_batch")
public class ImportBatchEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String filename;

    @NonNull
    @ColumnInfo(name = "content_hash")
    public String contentHash;

    @ColumnInfo(name = "started_at")
    public long startedAt;

    @ColumnInfo(name = "finished_at")
    public Long finishedAt;

    @NonNull
    public String outcome = "IN_PROGRESS"; // IN_PROGRESS, COMPLETED, FAILED

    @ColumnInfo(name = "entries_parsed")
    public int entriesParsed;

    @ColumnInfo(name = "entries_new")
    public int entriesNew;

    @ColumnInfo(name = "entries_unchanged")
    public int entriesUnchanged;

    @ColumnInfo(name = "entries_conflict")
    public int entriesConflict;

    @ColumnInfo(name = "entries_duplicate")
    public int entriesDuplicate;

    @ColumnInfo(name = "entries_invalid")
    public int entriesInvalid;

    @ColumnInfo(name = "entries_skipped")
    public int entriesSkipped;

    @ColumnInfo(name = "ids_attached")
    public int idsAttached;

    @ColumnInfo(name = "error_message")
    public String errorMessage;

    public ImportBatchEntity() {
    }
}
