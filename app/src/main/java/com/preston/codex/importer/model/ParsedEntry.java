package com.preston.codex.importer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An individual entry parsed from a Markdown block.
 */
public final class ParsedEntry {
    private final int entryIndex;
    private final String stableId;
    private final String quoteText;
    private final String insightText;
    private final String sourceRef;
    private final String application;
    private final String entryType;
    private final String reviewMode;
    private final List<String> tags;
    private final List<String> related;
    private final Map<String, String> extraFields;
    private final String rawMarkdown;
    private final List<String> warnings;

    public ParsedEntry(int entryIndex,
                       String stableId,
                       String quoteText,
                       String insightText,
                       String sourceRef,
                       String application,
                       String entryType,
                       String reviewMode,
                       List<String> tags,
                       List<String> related,
                       Map<String, String> extraFields,
                       String rawMarkdown,
                       List<String> warnings) {
        this.entryIndex = entryIndex;
        this.stableId = stableId;
        this.quoteText = quoteText == null ? "" : quoteText;
        this.insightText = insightText;
        this.sourceRef = sourceRef;
        this.application = application;
        this.entryType = (entryType == null || entryType.trim().isEmpty()) ? "QUOTE" : entryType.trim();
        this.reviewMode = reviewMode;
        this.tags = tags == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(tags));
        this.related = related == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(related));
        this.extraFields = extraFields == null ? Collections.emptyMap() : Collections.unmodifiableMap(new LinkedHashMap<>(extraFields));
        this.rawMarkdown = rawMarkdown == null ? "" : rawMarkdown;
        this.warnings = warnings == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    public int getEntryIndex() {
        return entryIndex;
    }

    public String getStableId() {
        return stableId;
    }

    public String getQuoteText() {
        return quoteText;
    }

    public String getInsightText() {
        return insightText;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public String getApplication() {
        return application;
    }

    public String getEntryType() {
        return entryType;
    }

    public String getReviewMode() {
        return reviewMode;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getRelated() {
        return related;
    }

    public Map<String, String> getExtraFields() {
        return extraFields;
    }

    public String getRawMarkdown() {
        return rawMarkdown;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
