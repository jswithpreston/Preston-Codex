package com.preston.codex.domain.knowledge;

import java.util.List;

/**
 * Domain repository interface for entry existence, bookmarks, and relationships.
 * Pure Java, zero android.* imports.
 */
public interface KnowledgeDomainRepository {

    /**
     * Checks if an entry with the given ID exists.
     */
    boolean entryExists(long entryId);

    /**
     * Sets or removes the bookmark status for an entry without modifying updated_at or hash fields.
     */
    void setBookmark(long entryId, boolean isBookmarked);

    /**
     * Checks if an entry is bookmarked.
     */
    boolean isBookmarked(long entryId);

    /**
     * Retrieves all entry IDs that are currently bookmarked.
     */
    List<Long> getBookmarkedEntryIds();

    /**
     * Adds an undirected relationship. Idempotent (ignores duplicates).
     */
    void addRelationship(KnowledgeRelationship relationship);

    /**
     * Removes an undirected relationship.
     */
    void removeRelationship(long fromId, long toId, String relType);

    /**
     * Finds all relationships involving the given entry ID (either as fromId or toId).
     */
    List<KnowledgeRelationship> getRelationshipsForEntry(long entryId);
}
