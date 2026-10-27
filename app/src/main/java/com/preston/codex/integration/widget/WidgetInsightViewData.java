package com.preston.codex.integration.widget;

import java.util.Objects;

/**
 * Immutable view data model for binding daily insight content into widget RemoteViews.
 * Contains no android.* dependencies.
 */
public final class WidgetInsightViewData {

    private final long entryId;
    private final String quoteText;
    private final String authorLine;
    private final String insightText;
    private final String formattedDate;
    private final boolean isBookmarked;
    private final boolean isEmpty;

    public WidgetInsightViewData(
            long entryId,
            String quoteText,
            String authorLine,
            String insightText,
            String formattedDate,
            boolean isBookmarked,
            boolean isEmpty
    ) {
        this.entryId = entryId;
        this.quoteText = Objects.requireNonNull(quoteText, "quoteText must not be null");
        this.authorLine = Objects.requireNonNull(authorLine, "authorLine must not be null");
        this.insightText = Objects.requireNonNull(insightText, "insightText must not be null");
        this.formattedDate = Objects.requireNonNull(formattedDate, "formattedDate must not be null");
        this.isBookmarked = isBookmarked;
        this.isEmpty = isEmpty;
    }

    public static WidgetInsightViewData createEmpty(String formattedDate) {
        return new WidgetInsightViewData(
                -1L,
                "",
                "",
                "",
                formattedDate != null ? formattedDate : "",
                false,
                true
        );
    }

    public long getEntryId() {
        return entryId;
    }

    public String getQuoteText() {
        return quoteText;
    }

    public String getAuthorLine() {
        return authorLine;
    }

    public String getInsightText() {
        return insightText;
    }

    public String getFormattedDate() {
        return formattedDate;
    }

    public boolean isBookmarked() {
        return isBookmarked;
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WidgetInsightViewData that = (WidgetInsightViewData) o;
        return entryId == that.entryId &&
                isBookmarked == that.isBookmarked &&
                isEmpty == that.isEmpty &&
                Objects.equals(quoteText, that.quoteText) &&
                Objects.equals(authorLine, that.authorLine) &&
                Objects.equals(insightText, that.insightText) &&
                Objects.equals(formattedDate, that.formattedDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryId, quoteText, authorLine, insightText, formattedDate, isBookmarked, isEmpty);
    }

    @Override
    public String toString() {
        return "WidgetInsightViewData{" +
                "entryId=" + entryId +
                ", isBookmarked=" + isBookmarked +
                ", isEmpty=" + isEmpty +
                '}';
    }
}
