package com.preston.codex.integration.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WidgetInsightViewDataTest {

    @Test
    public void populatedData_gettersAndEquality() {
        WidgetInsightViewData data1 = new WidgetInsightViewData(
                12L,
                "Focus is a skill.",
                "— Cal Newport, Deep Work",
                "Deliberate practice is key.",
                "Mon, Oct 26, 2026",
                true,
                false
        );

        WidgetInsightViewData data2 = new WidgetInsightViewData(
                12L,
                "Focus is a skill.",
                "— Cal Newport, Deep Work",
                "Deliberate practice is key.",
                "Mon, Oct 26, 2026",
                true,
                false
        );

        assertEquals(12L, data1.getEntryId());
        assertEquals("Focus is a skill.", data1.getQuoteText());
        assertEquals("— Cal Newport, Deep Work", data1.getAuthorLine());
        assertEquals("Deliberate practice is key.", data1.getInsightText());
        assertEquals("Mon, Oct 26, 2026", data1.getFormattedDate());
        assertTrue(data1.isBookmarked());
        assertFalse(data1.isEmpty());
        assertEquals(data1, data2);
        assertEquals(data1.hashCode(), data2.hashCode());
    }

    @Test
    public void emptyState_createsConsistentEmptyViewData() {
        WidgetInsightViewData empty = WidgetInsightViewData.createEmpty("Mon, Oct 26, 2026");

        assertEquals(-1L, empty.getEntryId());
        assertTrue(empty.isEmpty());
        assertFalse(empty.isBookmarked());
        assertEquals("", empty.getQuoteText());
        assertEquals("", empty.getAuthorLine());
        assertEquals("", empty.getInsightText());
        assertEquals("Mon, Oct 26, 2026", empty.getFormattedDate());
    }

    @Test
    public void inequality_differingFields() {
        WidgetInsightViewData d1 = new WidgetInsightViewData(1L, "Q", "A", "I", "D", false, false);
        WidgetInsightViewData d2 = new WidgetInsightViewData(2L, "Q", "A", "I", "D", false, false);
        WidgetInsightViewData d3 = new WidgetInsightViewData(1L, "Q", "A", "I", "D", true, false);

        assertNotEquals(d1, d2);
        assertNotEquals(d1, d3);
    }
}
