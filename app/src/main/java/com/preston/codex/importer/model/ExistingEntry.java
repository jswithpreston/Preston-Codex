package com.preston.codex.importer.model;

/**
 * Lightweight projection of an existing entry in the database for matching purposes.
 */
public final class ExistingEntry {
    private final long id;
    private final Long sourceId;
    private final String stableId;
    private final String fingerprint;
    private final String contentHash;
    private final String importedHash;
    private final String quoteText;
    private final boolean isArchived;
    private final boolean isTrashed;

    public ExistingEntry(long id,
                         Long sourceId,
                         String stableId,
                         String fingerprint,
                         String contentHash,
                         String importedHash,
                         String quoteText,
                         boolean isArchived,
                         boolean isTrashed) {
        this.id = id;
        this.sourceId = sourceId;
        this.stableId = stableId;
        this.fingerprint = fingerprint;
        this.contentHash = contentHash;
        this.importedHash = importedHash;
        this.quoteText = quoteText;
        this.isArchived = isArchived;
        this.isTrashed = isTrashed;
    }

    public long getId() {
        return id;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public String getStableId() {
        return stableId;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getImportedHash() {
        return importedHash;
    }

    public String getQuoteText() {
        return quoteText;
    }

    public boolean isArchived() {
        return isArchived;
    }

    public boolean isTrashed() {
        return isTrashed;
    }
}
