package com.preston.codex.integration.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.preston.codex.domain.scheduling.InsightDetails;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Optional;

public class InsightViewDataMapperTest {

    private final InsightViewDataMapper mapper = new InsightViewDataMapper();
    private final LocalDate testDate = LocalDate.of(2026, 10, 26);

    @Test
    public void map_fullInsightDetails_formatsCorrectly() {
        InsightDetails details = new InsightDetails(
                42L,
                "Clarity about what matters provides clarity about what does not.",
                "Cal Newport",
                "Deep Work",
                "Focus is a filtering mechanism.",
                true
        );

        WidgetInsightViewData viewData = mapper.map(Optional.of(details), testDate);

        assertEquals(42L, viewData.getEntryId());
        assertEquals("“Clarity about what matters provides clarity about what does not.”", viewData.getQuoteText());
        assertEquals("— Cal Newport, Deep Work", viewData.getAuthorLine());
        assertEquals("Focus is a filtering mechanism.", viewData.getInsightText());
        assertEquals("Mon, Oct 26, 2026", viewData.getFormattedDate());
        assertTrue(viewData.isBookmarked());
        assertFalse(viewData.isEmpty());
    }

    @Test
    public void map_onlyAuthor_formatsAuthorLineWithoutComma() {
        InsightDetails details = new InsightDetails(
                10L,
                "Simplicity is prerequisite for reliability.",
                "Edsger Dijkstra",
                "",
                null,
                false
        );

        WidgetInsightViewData viewData = mapper.map(Optional.of(details), testDate);

        assertEquals("— Edsger Dijkstra", viewData.getAuthorLine());
        assertEquals("", viewData.getInsightText());
        assertFalse(viewData.isBookmarked());
        assertFalse(viewData.isEmpty());
    }

    @Test
    public void map_onlySourceTitle_formatsSourceWithoutAuthor() {
        InsightDetails details = new InsightDetails(
                11L,
                "An excerpt from an anonymous manual.",
                "",
                "The Ancient Codex",
                "Study daily.",
                false
        );

        WidgetInsightViewData viewData = mapper.map(Optional.of(details), testDate);

        assertEquals("— The Ancient Codex", viewData.getAuthorLine());
        assertEquals("Study daily.", viewData.getInsightText());
    }

    @Test
    public void map_emptyDetails_returnsEmptyViewData() {
        WidgetInsightViewData viewData = mapper.map(Optional.empty(), testDate);

        assertTrue(viewData.isEmpty());
        assertEquals(-1L, viewData.getEntryId());
        assertEquals("Mon, Oct 26, 2026", viewData.getFormattedDate());
    }
}
