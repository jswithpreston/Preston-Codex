package com.preston.codex.importer.model;

/**
 * Report reflecting actual committed database counts after import transaction completes.
 */
public final class ImportReport {
    private final long batchId;
    private final String outcome;
    private final int entriesParsed;
    private final int entriesNew;
    private final int entriesUnchanged;
    private final int entriesConflict;
    private final int entriesDuplicate;
    private final int entriesInvalid;
    private final int entriesSkipped;
    private final int idsAttached;

    public ImportReport(long batchId,
                        String outcome,
                        int entriesParsed,
                        int entriesNew,
                        int entriesUnchanged,
                        int entriesConflict,
                        int entriesDuplicate,
                        int entriesInvalid,
                        int entriesSkipped,
                        int idsAttached) {
        this.batchId = batchId;
        this.outcome = outcome;
        this.entriesParsed = entriesParsed;
        this.entriesNew = entriesNew;
        this.entriesUnchanged = entriesUnchanged;
        this.entriesConflict = entriesConflict;
        this.entriesDuplicate = entriesDuplicate;
        this.entriesInvalid = entriesInvalid;
        this.entriesSkipped = entriesSkipped;
        this.idsAttached = idsAttached;
    }

    public long getBatchId() { return batchId; }
    public String getOutcome() { return outcome; }
    public int getEntriesParsed() { return entriesParsed; }
    public int getEntriesNew() { return entriesNew; }
    public int getEntriesUnchanged() { return entriesUnchanged; }
    public int getEntriesConflict() { return entriesConflict; }
    public int getEntriesDuplicate() { return entriesDuplicate; }
    public int getEntriesInvalid() { return entriesInvalid; }
    public int getEntriesSkipped() { return entriesSkipped; }
    public int getIdsAttached() { return idsAttached; }
}
