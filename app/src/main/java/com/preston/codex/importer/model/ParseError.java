package com.preston.codex.importer.model;

import java.util.Objects;

/**
 * Details of a parsing or validation error in an imported file.
 */
public final class ParseError {
    private final String filename;
    private final int entryIndex;
    private final String message;

    public ParseError(String filename, int entryIndex, String message) {
        this.filename = filename == null ? "" : filename;
        this.entryIndex = entryIndex;
        this.message = message == null ? "" : message;
    }

    public String getFilename() {
        return filename;
    }

    public int getEntryIndex() {
        return entryIndex;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "[" + filename + " entry " + entryIndex + "] " + message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParseError)) return false;
        ParseError that = (ParseError) o;
        return entryIndex == that.entryIndex &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filename, entryIndex, message);
    }
}
