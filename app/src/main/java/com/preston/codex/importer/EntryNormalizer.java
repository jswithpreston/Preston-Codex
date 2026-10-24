package com.preston.codex.importer;

import java.util.Locale;

/**
 * Text normalization utilities for knowledge sources and entries.
 * Implements locked decision #1: trim, collapse whitespace, unify line endings
 * and quote characters; do NOT strip words or meaningful punctuation.
 */
public class EntryNormalizer {

    public String normalize(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replace("\r\n", "\n").replace("\r", "\n");
        cleaned = unifyCurlyQuotes(cleaned);

        String[] lines = cleaned.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String line : lines) {
            String collapsed = line.replaceAll("[ \\t]+", " ").trim();
            if (!first) {
                sb.append("\n");
            }
            sb.append(collapsed);
            first = false;
        }
        return sb.toString().trim();
    }

    public String toKey(String text) {
        return normalize(text).toLowerCase(Locale.ROOT);
    }

    public String unifyCurlyQuotes(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u201C', '"')
                   .replace('\u201D', '"')
                   .replace('\u2018', '\'')
                   .replace('\u2019', '\'');
    }

    public String stripWrappingQuotes(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = unifyCurlyQuotes(text).trim();
        if (trimmed.length() >= 2) {
            if ((trimmed.startsWith("\"") && trimmed.endsWith("\""))
                    || (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
                return trimmed.substring(1, trimmed.length() - 1).trim();
            }
        }
        return trimmed;
    }
}
