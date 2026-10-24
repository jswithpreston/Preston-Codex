package com.preston.codex.importer;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class EntryNormalizerTest {

    private EntryNormalizer normalizer;

    @Before
    public void setUp() {
        normalizer = new EntryNormalizer();
    }

    @Test
    public void N01_normalize_trimsWhitespace() {
        assertEquals("hello", normalizer.normalize("   hello   "));
    }

    @Test
    public void N02_normalize_collapsesInternalWhitespace() {
        assertEquals("a b c", normalizer.normalize("a    b\t\tc"));
    }

    @Test
    public void N03_normalize_unifiesLineEndings() {
        assertEquals("line 1\nline 2\nline 3", normalizer.normalize("line 1\r\nline 2\rline 3"));
    }

    @Test
    public void N04_normalize_unifiesCurlyQuotes() {
        String input = "\u201CHello\u201D, she said, \u2018it\u2019s fine\u2019.";
        String expected = "\"Hello\", she said, 'it's fine'.";
        assertEquals(expected, normalizer.normalize(input));
    }

    @Test
    public void N05_normalize_preservesMeaningfulPunctuation() {
        String input = "Don't stop; keep going -- it's 100% worth it!";
        assertEquals("Don't stop; keep going -- it's 100% worth it!", normalizer.normalize(input));
    }

    @Test
    public void N06_normalize_preservesWords_noStemming() {
        String input = "Running runners ran swiftly.";
        assertEquals("Running runners ran swiftly.", normalizer.normalize(input));
    }

    @Test
    public void N07_normalizeSource_collapsesTitleSpacing() {
        assertEquals("Deep Work", normalizer.normalize("Deep   Work"));
    }

    @Test
    public void N08_stripWrappingQuotes_stripsSinglePairOnly() {
        assertEquals("Hello \"world\"", normalizer.stripWrappingQuotes("\"Hello \"world\"\""));
        assertEquals("Hello world", normalizer.stripWrappingQuotes("\u201CHello world\u201D"));
        assertEquals("Hello world", normalizer.stripWrappingQuotes("'Hello world'"));
        // Does not strip unmatched quotes
        assertEquals("\"Hello world", normalizer.stripWrappingQuotes("\"Hello world"));
    }

    @Test
    public void N09_normalize_emptyOrNullString_returnsEmpty() {
        assertEquals("", normalizer.normalize(""));
        assertEquals("", normalizer.normalize(null));
        assertEquals("", normalizer.normalize("    \n\t  "));
    }
}
