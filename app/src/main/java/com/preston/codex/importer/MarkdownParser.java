package com.preston.codex.importer;

import com.preston.codex.importer.model.ParseError;
import com.preston.codex.importer.model.ParsedEntry;
import com.preston.codex.importer.model.ParsedFile;
import com.preston.codex.importer.model.ParsedSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java Markdown parser for importing personal knowledge files.
 * Zero android.* or org.json dependencies.
 */
public class MarkdownParser {

    private static final Pattern SEPARATOR_PATTERN = Pattern.compile("^\\s*-{3,}\\s*$");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^#\\s*([A-Za-z0-9_-]+):\\s*(.*)$");
    private static final Pattern FIELD_PATTERN = Pattern.compile("^\\*\\*([A-Za-z0-9_ -]+):\\*\\*\\s*(.*)$");

    private static final Set<String> KNOWN_TYPES = new HashSet<>(Arrays.asList(
            "quote", "principle", "fact", "mental_model", "question"
    ));

    private static final Set<String> KNOWN_REVIEWS = new HashSet<>(Arrays.asList(
            "standard", "spaced", "none"
    ));

    public ParsedFile parse(String filename, String content) {
        if (content == null || content.trim().isEmpty()) {
            return new ParsedFile(filename, null, Collections.emptyList(), Collections.emptyList());
        }

        String normalized = content.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n", -1);

        String bookTitle = null;
        String author = "";
        String sourceType = "BOOK";
        String publisher = null;
        Integer year = null;
        String isbn = null;

        List<ParseError> errors = new ArrayList<>();
        List<ParsedEntry> entries = new ArrayList<>();

        List<String> entryBlockLines = new ArrayList<>();
        List<List<String>> rawBlocks = new ArrayList<>();

        boolean readingHeader = true;

        for (String line : lines) {
            String trimmed = line.trim();
            if (readingHeader) {
                if (trimmed.startsWith("#")) {
                    Matcher m = HEADER_PATTERN.matcher(trimmed);
                    if (m.matches()) {
                        String key = m.group(1).trim().toLowerCase(Locale.ROOT);
                        String val = m.group(2).trim();
                        if ("book".equals(key) || "title".equals(key)) {
                            bookTitle = val;
                        } else if ("author".equals(key)) {
                            author = val;
                        } else if ("type".equals(key)) {
                            sourceType = val;
                        } else if ("publisher".equals(key)) {
                            publisher = val;
                        } else if ("year".equals(key)) {
                            try {
                                year = Integer.parseInt(val);
                            } catch (NumberFormatException ignored) {
                            }
                        } else if ("isbn".equals(key)) {
                            isbn = val;
                        }
                    }
                    continue;
                } else if (trimmed.isEmpty()) {
                    continue;
                } else {
                    readingHeader = false;
                }
            }

            if (SEPARATOR_PATTERN.matcher(line).matches()) {
                if (!entryBlockLines.isEmpty()) {
                    rawBlocks.add(new ArrayList<>(entryBlockLines));
                    entryBlockLines.clear();
                }
            } else {
                entryBlockLines.add(line);
            }
        }

        if (!entryBlockLines.isEmpty()) {
            rawBlocks.add(new ArrayList<>(entryBlockLines));
        }

        if (bookTitle == null || bookTitle.trim().isEmpty()) {
            errors.add(new ParseError(filename, 0, "Missing required source header (# Book: or # Title:)"));
            return new ParsedFile(filename, null, Collections.emptyList(), errors);
        }

        ParsedSource source = new ParsedSource(bookTitle, author, sourceType, publisher, year, isbn);

        int entryIndex = 0;
        for (List<String> blockLines : rawBlocks) {
            // Check if block has any content
            boolean hasContent = false;
            for (String l : blockLines) {
                if (!l.trim().isEmpty()) {
                    hasContent = true;
                    break;
                }
            }
            if (!hasContent) {
                continue;
            }

            ParsedEntry entry = parseEntryBlock(filename, entryIndex, blockLines, errors);
            if (entry != null) {
                entries.add(entry);
            }
            entryIndex++;
        }

        return new ParsedFile(filename, source, entries, errors);
    }

    private ParsedEntry parseEntryBlock(String filename, int entryIndex, List<String> lines, List<ParseError> errors) {
        String rawMarkdown = String.join("\n", lines).trim();
        Map<String, StringBuilder> fieldBuilders = new LinkedHashMap<>();
        Set<String> seenFields = new HashSet<>();
        String currentField = null;

        for (String line : lines) {
            Matcher m = FIELD_PATTERN.matcher(line.trim());
            if (m.matches()) {
                String fieldName = m.group(1).trim();
                String fieldKey = fieldName.toLowerCase(Locale.ROOT);
                if (seenFields.contains(fieldKey)) {
                    errors.add(new ParseError(filename, entryIndex, "Duplicate field: **" + fieldName + ":**"));
                    return null;
                }
                seenFields.add(fieldKey);
                currentField = fieldName;
                StringBuilder sb = new StringBuilder(m.group(2));
                fieldBuilders.put(fieldName, sb);
            } else {
                if (currentField != null) {
                    fieldBuilders.get(currentField).append("\n").append(line);
                }
            }
        }

        // Validate presence of Quote
        String rawQuote = null;
        for (Map.Entry<String, StringBuilder> e : fieldBuilders.entrySet()) {
            if ("quote".equalsIgnoreCase(e.getKey())) {
                rawQuote = e.getValue().toString().trim();
                break;
            }
        }

        if (rawQuote == null || rawQuote.isEmpty()) {
            errors.add(new ParseError(filename, entryIndex, "Missing required **Quote:** field"));
            return null;
        }

        String stableId = null;
        String insight = null;
        String sourceRef = null;
        String application = null;
        String entryType = "QUOTE";
        String reviewMode = null;
        List<String> tags = new ArrayList<>();
        List<String> related = new ArrayList<>();
        Map<String, String> extraFields = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();

        for (Map.Entry<String, StringBuilder> e : fieldBuilders.entrySet()) {
            String key = e.getKey();
            String lowerKey = key.toLowerCase(Locale.ROOT);
            String val = e.getValue().toString().trim();

            switch (lowerKey) {
                case "quote":
                    // Handled
                    break;
                case "id":
                    stableId = val.isEmpty() ? null : val;
                    break;
                case "insight":
                    insight = val.isEmpty() ? null : val;
                    break;
                case "source":
                    sourceRef = val.isEmpty() ? null : val;
                    break;
                case "application":
                    application = val.isEmpty() ? null : val;
                    break;
                case "type":
                    if (val.isEmpty()) {
                        entryType = "QUOTE";
                    } else if (KNOWN_TYPES.contains(val.toLowerCase(Locale.ROOT))) {
                        entryType = val.toLowerCase(Locale.ROOT);
                    } else {
                        warnings.add("Unrecognized Type: '" + val + "'. Defaulted to 'QUOTE'.");
                        extraFields.put("_raw_type", val);
                        entryType = "QUOTE";
                    }
                    break;
                case "review":
                    if (val.isEmpty()) {
                        reviewMode = null;
                    } else if (KNOWN_REVIEWS.contains(val.toLowerCase(Locale.ROOT))) {
                        reviewMode = val.toLowerCase(Locale.ROOT);
                    } else {
                        warnings.add("Unrecognized Review mode: '" + val + "'. Ignored.");
                        extraFields.put("_raw_review", val);
                        reviewMode = null;
                    }
                    break;
                case "tags":
                    if (!val.isEmpty()) {
                        for (String part : val.split(",")) {
                            String trimmedTag = part.trim();
                            if (!trimmedTag.isEmpty()) {
                                tags.add(trimmedTag);
                            }
                        }
                    }
                    break;
                case "related":
                    if (!val.isEmpty()) {
                        for (String part : val.split(",")) {
                            String trimmedRel = part.trim();
                            if (!trimmedRel.isEmpty()) {
                                related.add(trimmedRel);
                            }
                        }
                    }
                    break;
                default:
                    extraFields.put(key, val);
                    break;
            }
        }

        return new ParsedEntry(
                entryIndex,
                stableId,
                rawQuote,
                insight,
                sourceRef,
                application,
                entryType,
                reviewMode,
                tags,
                related,
                extraFields,
                rawMarkdown,
                warnings
        );
    }
}
