package com.preston.codex.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.preston.codex.importer.model.ParsedEntry;
import com.preston.codex.importer.model.ParsedFile;
import com.preston.codex.importer.model.ParsedSource;

import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class MarkdownParserTest {

    private MarkdownParser parser;

    @Before
    public void setUp() {
        parser = new MarkdownParser();
    }

    private String loadFixture(String fixtureName) {
        String path = "/fixtures/" + fixtureName;
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalArgumentException("Fixture not found: " + path);
        }
        try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    @Test
    public void P01_parseValid_twoEntries_returnsCorrectCount() {
        String content = loadFixture("valid_basic.md");
        ParsedFile result = parser.parse("valid_basic.md", content);

        assertNotNull(result.getSource());
        assertEquals("Deep Work", result.getSource().getTitle());
        assertEquals("Cal Newport", result.getSource().getAuthor());
        assertEquals(2, result.getEntries().size());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void P02_parseValid_allOptionalFields_populated() {
        String content = loadFixture("valid_full.md");
        ParsedFile result = parser.parse("valid_full.md", content);

        assertEquals(2, result.getEntries().size());
        ParsedEntry first = result.getEntries().get(0);
        assertEquals("deep-work-01", first.getStableId());
        assertEquals("Page 42", first.getSourceRef());
        assertEquals("Review weekly priorities.", first.getApplication());
        assertEquals("principle", first.getEntryType());
        assertEquals("spaced", first.getReviewMode());
        assertTrue(first.getTags().contains("focus"));
        assertTrue(first.getTags().contains("productivity"));
        assertEquals(1, first.getRelated().size());
        assertEquals("deep-work-02", first.getRelated().get(0));
    }

    @Test
    public void P03_parseMissingOptional_noError() {
        String content = loadFixture("valid_missing_optional.md");
        ParsedFile result = parser.parse("valid_missing_optional.md", content);

        assertEquals(0, result.getErrors().size());
        assertEquals(1, result.getEntries().size());
        ParsedEntry entry = result.getEntries().get(0);
        assertNull(entry.getStableId());
        assertNull(entry.getSourceRef());
        assertNull(entry.getApplication());
        assertNull(entry.getReviewMode());
    }

    @Test
    public void P04_parseMissingQuote_producesInvalidEntry() {
        String content = loadFixture("invalid_no_quote.md");
        ParsedFile result = parser.parse("invalid_no_quote.md", content);

        assertEquals(0, result.getEntries().size());
        assertEquals(1, result.getErrors().size());
        assertEquals("invalid_no_quote.md", result.getErrors().get(0).getFilename());
        assertEquals(0, result.getErrors().get(0).getEntryIndex());
        assertTrue(result.getErrors().get(0).getMessage().toLowerCase().contains("quote"));
    }

    @Test
    public void P05_parseMultipleEntries_correctIndexing() {
        String content = loadFixture("valid_basic.md");
        ParsedFile result = parser.parse("valid_basic.md", content);

        assertEquals(2, result.getEntries().size());
        assertEquals(0, result.getEntries().get(0).getEntryIndex());
        assertEquals(1, result.getEntries().get(1).getEntryIndex());
        assertTrue(result.getEntries().get(0).getQuoteText().contains("Clarity about what matters"));
        assertTrue(result.getEntries().get(1).getQuoteText().contains("If you don't produce"));
    }

    @Test
    public void P06_parseEmptyFile_returnsEmpty() {
        String content = loadFixture("invalid_empty_file.md");
        ParsedFile result = parser.parse("invalid_empty_file.md", content);

        assertNull(result.getSource());
        assertEquals(0, result.getEntries().size());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void P07_parseNoBookHeader_producesError() {
        String content = loadFixture("invalid_no_book_header.md");
        ParsedFile result = parser.parse("invalid_no_book_header.md", content);

        assertNull(result.getSource());
        assertFalse(result.getErrors().isEmpty());
        assertTrue(result.getErrors().get(0).getMessage().toLowerCase().contains("book"));
    }

    @Test
    public void P08_parseUnicode_contentPreserved() {
        String content = loadFixture("valid_unicode.md");
        ParsedFile result = parser.parse("valid_unicode.md", content);

        assertEquals("思考，快与慢", result.getSource().getTitle());
        assertEquals(1, result.getEntries().size());
        ParsedEntry entry = result.getEntries().get(0);
        assertTrue(entry.getQuoteText().contains("系统1运行是无意识且快速的——不怎么费力，或者完全不费力。"));
        assertTrue(entry.getTags().contains("心理学"));
        assertTrue(entry.getTags().contains("pensée"));
    }

    @Test
    public void P09_parseEscapedQuotes_contentPreserved() {
        String content = loadFixture("valid_escaped_quotes.md");
        ParsedFile result = parser.parse("valid_escaped_quotes.md", content);

        assertEquals(1, result.getEntries().size());
        ParsedEntry entry = result.getEntries().get(0);
        assertTrue(entry.getQuoteText().contains("\"To succeed today, you must 'produce' value.\""));
    }

    @Test
    public void P10_parseCrlf_sameResultAsLf() {
        String lfContent = loadFixture("valid_basic.md");
        String crlfContent = loadFixture("valid_crlf.md");

        ParsedFile lfResult = parser.parse("valid_basic.md", lfContent);
        ParsedFile crlfResult = parser.parse("valid_crlf.md", crlfContent);

        assertEquals(lfResult.getEntries().size(), crlfResult.getEntries().size());
        assertEquals(lfResult.getSource().getTitle(), crlfResult.getSource().getTitle());
        for (int i = 0; i < lfResult.getEntries().size(); i++) {
            assertEquals(lfResult.getEntries().get(i).getQuoteText().trim(),
                    crlfResult.getEntries().get(i).getQuoteText().trim());
            assertEquals(lfResult.getEntries().get(i).getInsightText().trim(),
                    crlfResult.getEntries().get(i).getInsightText().trim());
        }
    }

    @Test
    public void P11_parseUnknownFields_inExtraFieldsMap() {
        String content = loadFixture("valid_unknown_fields.md");
        ParsedFile result = parser.parse("valid_unknown_fields.md", content);

        assertEquals(1, result.getEntries().size());
        ParsedEntry entry = result.getEntries().get(0);
        assertEquals("5", entry.getExtraFields().get("Rating"));
        assertEquals("high", entry.getExtraFields().get("Priority"));
    }

    @Test
    public void P12_parseMalformedSeparator_notSplit() {
        String content = loadFixture("invalid_malformed_separator.md");
        ParsedFile result = parser.parse("invalid_malformed_separator.md", content);

        // '===' does not match '---' separator, so it is treated as a single block.
        // It has duplicate **Quote:** fields, which triggers invalid entry error!
        assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void P13_parseMixed_validExtracted_invalidReported() {
        String content = loadFixture("invalid_mixed.md");
        ParsedFile result = parser.parse("invalid_mixed.md", content);

        assertEquals(1, result.getEntries().size());
        assertEquals(1, result.getErrors().size());
        assertEquals(1, result.getErrors().get(0).getEntryIndex());
    }

    @Test
    public void P14_parseLargeFile_50Entries_noError() {
        String content = loadFixture("valid_large.md");
        ParsedFile result = parser.parse("valid_large.md", content);

        assertEquals(50, result.getEntries().size());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    public void P15_parseExtended_allSupportedFieldsParsed() {
        String content = loadFixture("valid_full.md");
        ParsedFile result = parser.parse("valid_full.md", content);

        ParsedEntry entry = result.getEntries().get(0);
        assertEquals("deep-work-01", entry.getStableId());
        assertEquals("Page 42", entry.getSourceRef());
        assertEquals("Review weekly priorities.", entry.getApplication());
        assertEquals("principle", entry.getEntryType());
        assertEquals("spaced", entry.getReviewMode());
        assertEquals(2, entry.getTags().size());
        assertEquals(1, entry.getRelated().size());
    }

    @Test
    public void P16_parseMultilineField_continuationLinesBelongToPreviousField() {
        String content = loadFixture("valid_multiline_field.md");
        ParsedFile result = parser.parse("valid_multiline_field.md", content);

        assertEquals(1, result.getEntries().size());
        ParsedEntry entry = result.getEntries().get(0);
        assertTrue(entry.getQuoteText().contains("First line of quote."));
        assertTrue(entry.getQuoteText().contains("Second line continues the quote."));
        assertTrue(entry.getQuoteText().contains("Third line still part of the quote."));
        assertTrue(entry.getInsightText().contains("Multiline insight paragraph 1."));
        assertTrue(entry.getInsightText().contains("Still part of the insight paragraph 2."));
    }

    @Test
    public void P17_parseDuplicateField_producesInvalidEntry() {
        String content = loadFixture("invalid_duplicate_field.md");
        ParsedFile result = parser.parse("invalid_duplicate_field.md", content);

        assertEquals(0, result.getEntries().size());
        assertEquals(1, result.getErrors().size());
        assertTrue(result.getErrors().get(0).getMessage().toLowerCase().contains("duplicate"));
    }

    @Test
    public void P18_parseUnknownType_appliesDefaultAndWarns() {
        String content = loadFixture("valid_unknown_type.md");
        ParsedFile result = parser.parse("valid_unknown_type.md", content);

        assertEquals(1, result.getEntries().size());
        ParsedEntry entry = result.getEntries().get(0);
        assertEquals("QUOTE", entry.getEntryType());
        assertEquals("mysterious_type", entry.getExtraFields().get("_raw_type"));
        assertFalse(entry.getWarnings().isEmpty());
    }

    @Test
    public void P19_parseUnknownReview_appliesDefaultAndWarns() {
        String content = loadFixture("valid_unknown_review.md");
        ParsedFile result = parser.parse("valid_unknown_review.md", content);

        assertEquals(1, result.getEntries().size());
        ParsedEntry entry = result.getEntries().get(0);
        assertNull(entry.getReviewMode());
        assertEquals("alien_interval", entry.getExtraFields().get("_raw_review"));
        assertFalse(entry.getWarnings().isEmpty());
    }

    @Test
    public void P20_parseNoAuthor_setsAuthorEmptyString() {
        String content = loadFixture("valid_no_author.md");
        ParsedFile result = parser.parse("valid_no_author.md", content);

        assertNotNull(result.getSource());
        assertEquals("Ancient Proverb", result.getSource().getTitle());
        assertEquals("", result.getSource().getAuthor());
    }

    @Test
    public void P21_parseWrappingQuotes_rawTextPreservedVerbatim() {
        String content = loadFixture("valid_wrapping_quotes.md");
        ParsedFile result = parser.parse("valid_wrapping_quotes.md", content);

        assertEquals(1, result.getEntries().size());
        ParsedEntry entry = result.getEntries().get(0);
        assertEquals("\"Clarity about what matters provides clarity about what does not.\"", entry.getQuoteText());
    }

    @Test
    public void P22_parseFileWithSurroundingWhitespace_handledCleanly() {
        String content = "   \n\n# Book: Clean Code\n# Author: Uncle Bob\n\n**Quote:**\nCode is clean.\n\n\n";
        ParsedFile result = parser.parse("test.md", content);

        assertNotNull(result.getSource());
        assertEquals("Clean Code", result.getSource().getTitle());
        assertEquals(1, result.getEntries().size());
    }
}
