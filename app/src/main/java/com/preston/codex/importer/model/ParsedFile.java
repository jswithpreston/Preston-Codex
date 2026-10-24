package com.preston.codex.importer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representation of an entire parsed Markdown file.
 */
public final class ParsedFile {
    private final String filename;
    private final ParsedSource source;
    private final List<ParsedEntry> entries;
    private final List<ParseError> errors;

    public ParsedFile(String filename, ParsedSource source, List<ParsedEntry> entries, List<ParseError> errors) {
        this.filename = filename == null ? "" : filename;
        this.source = source;
        this.entries = entries == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(entries));
        this.errors = errors == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public String getFilename() {
        return filename;
    }

    public ParsedSource getSource() {
        return source;
    }

    public List<ParsedEntry> getEntries() {
        return entries;
    }

    public List<ParseError> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
