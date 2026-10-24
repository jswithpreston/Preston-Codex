package com.preston.codex.importer.model;

import java.util.Objects;

/**
 * Parsed metadata for a knowledge source (e.g. book).
 */
public final class ParsedSource {
    private final String title;
    private final String author;
    private final String sourceType;
    private final String publisher;
    private final Integer year;
    private final String isbn;

    public ParsedSource(String title, String author, String sourceType, String publisher, Integer year, String isbn) {
        this.title = title == null ? "" : title.trim();
        this.author = author == null ? "" : author.trim();
        this.sourceType = (sourceType == null || sourceType.trim().isEmpty()) ? "BOOK" : sourceType.trim();
        this.publisher = publisher;
        this.year = year;
        this.isbn = isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getPublisher() {
        return publisher;
    }

    public Integer getYear() {
        return year;
    }

    public String getIsbn() {
        return isbn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParsedSource)) return false;
        ParsedSource that = (ParsedSource) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(author, that.author) &&
                Objects.equals(sourceType, that.sourceType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, author, sourceType);
    }
}
