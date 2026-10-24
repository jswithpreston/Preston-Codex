package com.preston.codex.importer.model;

/**
 * Result of matching an incoming entry against batch state and database state.
 */
public final class MatchResult {
    private final ImportClassification classification;
    private final Long existingEntryId;
    private final String note;
    private final String errorMessage;

    public MatchResult(ImportClassification classification, Long existingEntryId, String note, String errorMessage) {
        this.classification = classification;
        this.existingEntryId = existingEntryId;
        this.note = note;
        this.errorMessage = errorMessage;
    }

    public static MatchResult of(ImportClassification classification) {
        return new MatchResult(classification, null, null, null);
    }

    public static MatchResult of(ImportClassification classification, Long existingEntryId) {
        return new MatchResult(classification, existingEntryId, null, null);
    }

    public static MatchResult of(ImportClassification classification, Long existingEntryId, String note) {
        return new MatchResult(classification, existingEntryId, note, null);
    }

    public static MatchResult invalid(String errorMessage) {
        return new MatchResult(ImportClassification.INVALID, null, null, errorMessage);
    }

    public ImportClassification getClassification() {
        return classification;
    }

    public Long getExistingEntryId() {
        return existingEntryId;
    }

    public String getNote() {
        return note;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
