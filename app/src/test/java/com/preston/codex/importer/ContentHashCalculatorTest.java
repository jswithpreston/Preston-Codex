package com.preston.codex.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ContentHashCalculatorTest {

    private ContentHashCalculator calculator;

    @Before
    public void setUp() {
        calculator = new ContentHashCalculator(new EntryNormalizer());
    }

    @Test
    public void CH01_contentHash_deterministic() {
        String h1 = calculator.calculate("Quote", "Insight", "Page 1", "App", "principle", Arrays.asList("a", "b"));
        String h2 = calculator.calculate("Quote", "Insight", "Page 1", "App", "principle", Arrays.asList("a", "b"));
        assertEquals(h1, h2);
    }

    @Test
    public void CH02_contentHash_detectsQuoteChange() {
        String h1 = calculator.calculate("Quote 1", "Insight", "Page 1", "App", "principle", Collections.emptyList());
        String h2 = calculator.calculate("Quote 2", "Insight", "Page 1", "App", "principle", Collections.emptyList());
        assertNotEquals(h1, h2);
    }

    @Test
    public void CH03_contentHash_detectsInsightChange() {
        String h1 = calculator.calculate("Quote", "Insight 1", "Page 1", "App", "principle", Collections.emptyList());
        String h2 = calculator.calculate("Quote", "Insight 2", "Page 1", "App", "principle", Collections.emptyList());
        assertNotEquals(h1, h2);
    }

    @Test
    public void CH04_contentHash_detectsTagChange_orderIndependent() {
        String h1 = calculator.calculate("Quote", "Insight", "Page 1", "App", "principle", Arrays.asList("focus", "productivity"));
        String h2 = calculator.calculate("Quote", "Insight", "Page 1", "App", "principle", Arrays.asList("productivity", "focus"));
        String h3 = calculator.calculate("Quote", "Insight", "Page 1", "App", "principle", Collections.singletonList("focus"));
        assertEquals(h1, h2);
        assertNotEquals(h1, h3);
    }

    @Test
    public void CH05_contentHash_detectsApplicationOrTypeChange() {
        String h1 = calculator.calculate("Quote", "Insight", "Page 1", "App 1", "principle", Collections.emptyList());
        String h2 = calculator.calculate("Quote", "Insight", "Page 1", "App 2", "principle", Collections.emptyList());
        String h3 = calculator.calculate("Quote", "Insight", "Page 1", "App 1", "quote", Collections.emptyList());
        assertNotEquals(h1, h2);
        assertNotEquals(h1, h3);
    }
}
