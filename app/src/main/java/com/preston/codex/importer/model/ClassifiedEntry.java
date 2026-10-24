package com.preston.codex.importer.model;

/**
 * A parsed entry decorated with its calculated hashes and classification match result.
 */
public final class ClassifiedEntry {
    private final ParsedEntry parsedEntry;
    private final MatchResult matchResult;
    private final String fingerprint;
    private final String contentHash;

    public ClassifiedEntry(ParsedEntry parsedEntry, MatchResult matchResult, String fingerprint, String contentHash) {
        this.parsedEntry = parsedEntry;
        this.matchResult = matchResult;
        this.fingerprint = fingerprint;
        this.contentHash = contentHash;
    }

    public ParsedEntry getParsedEntry() {
        return parsedEntry;
    }

    public MatchResult getMatchResult() {
        return matchResult;
    }

    public ImportClassification getClassification() {
        return matchResult.getClassification();
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public String getContentHash() {
        return contentHash;
    }
}
