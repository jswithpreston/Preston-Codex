package com.preston.codex.domain.scheduling;

import java.util.Objects;
import java.util.Optional;

/**
 * Pure domain read model representing displayable details of an insight
 * along with its source attribution.
 * Pure Java, zero android.* imports.
 */
public final class InsightDetails {

    private final long entryId;
    private final String quoteText;
    private final String author;
    private final String sourceTitle;
    private final String insightText;
    private final boolean isBookmarked;

    public InsightDetails(
            long entryId,
            String quoteText,
            String author,
            String sourceTitle,
            String insightText,
            boolean isBookmarked
    ) {
        this.entryId = entryId;
        this.quoteText = Objects.requireNonNull(quoteText, "quoteText must not be null");
        this.author = author != null ? author : "";
        this.sourceTitle = sourceTitle != null ? sourceTitle : "";
        this.insightText = insightText;
        this.isBookmarked = isBookmarked;
    }

    public long getEntryId() {
        return entryId;
    }

    public String getQuoteText() {
        return quoteText;
    }

    public String getAuthor() {
        return author;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public Optional<String> getInsightText() {
        return Optional.ofNullable(insightText);
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InsightDetails that = (InsightDetails) o;
        return entryId == that.entryId &&
                isBookmarked == that.isBookmarked &&
                Objects.equals(quoteText, that.quoteText) &&
                Objects.equals(author, that.author) &&
                Objects.equals(sourceTitle, that.sourceTitle) &&
                Objects.equals(insightText, that.insightText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryId, quoteText, author, sourceTitle, insightText, isBookmarked);
    }

    @Override
    public String toString() {
        return "InsightDetails{" +
                "entryId=" + entryId +
                ", isBookmarked=" + isBookmarked +
                '}';
    }
}
