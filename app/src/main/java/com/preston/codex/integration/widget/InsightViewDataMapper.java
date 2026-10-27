package com.preston.codex.integration.widget;

import com.preston.codex.domain.scheduling.InsightDetails;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Pure Java mapper converting domain {@link InsightDetails} into {@link WidgetInsightViewData}.
 * Zero android.*, data.entity.*, or data.dao.* imports.
 */
public final class InsightViewDataMapper {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.US);

    public WidgetInsightViewData map(Optional<InsightDetails> detailsOpt, LocalDate localDate) {
        Objects.requireNonNull(localDate, "localDate must not be null");
        String formattedDate = localDate.format(DATE_FORMATTER);

        if (detailsOpt == null || !detailsOpt.isPresent()) {
            return WidgetInsightViewData.createEmpty(formattedDate);
        }

        InsightDetails details = detailsOpt.get();
        String quote = formatQuote(details.getQuoteText());
        String authorLine = formatAuthorLine(details.getAuthor(), details.getSourceTitle());
        String insight = details.getInsightText().orElse("");

        return new WidgetInsightViewData(
                details.getEntryId(),
                quote,
                authorLine,
                insight,
                formattedDate,
                details.isBookmarked(),
                false
        );
    }

    private static String formatQuote(String rawQuote) {
        if (rawQuote == null || rawQuote.isEmpty()) {
            return "";
        }
        String trimmed = rawQuote.trim();
        if (trimmed.startsWith("“") && trimmed.endsWith("”")) {
            return trimmed;
        }
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return "“" + trimmed + "”";
    }

    private static String formatAuthorLine(String author, String sourceTitle) {
        boolean hasAuthor = author != null && !author.trim().isEmpty();
        boolean hasTitle = sourceTitle != null && !sourceTitle.trim().isEmpty();

        if (hasAuthor && hasTitle) {
            return "— " + author.trim() + ", " + sourceTitle.trim();
        } else if (hasAuthor) {
            return "— " + author.trim();
        } else if (hasTitle) {
            return "— " + sourceTitle.trim();
        } else {
            return "";
        }
    }
}
