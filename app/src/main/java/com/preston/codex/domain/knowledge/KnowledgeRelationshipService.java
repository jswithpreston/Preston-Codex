package com.preston.codex.domain.knowledge;

import com.preston.codex.domain.TransactionRunner;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Service for managing explicit undirected relationships between knowledge entries.
 */
public final class KnowledgeRelationshipService {

    public static final String DEFAULT_REL_TYPE = "RELATED";

    private final KnowledgeDomainRepository repository;
    private final Clock clock;
    private final TransactionRunner transactionRunner;

    public KnowledgeRelationshipService(
            KnowledgeDomainRepository repository,
            Clock clock,
            TransactionRunner transactionRunner
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
    }

    /**
     * Links two entries with an undirected relationship.
     * Idempotent: linking twice does not throw and maintains exactly one record.
     * Arguments can be provided in any order (normalizes fromId &lt; toId).
     */
    public void linkEntries(long entryA, long entryB, String relType) {
        if (entryA == entryB) {
            throw new IllegalArgumentException("Cannot link entry " + entryA + " to itself");
        }
        String type = (relType != null && !relType.trim().isEmpty()) ? relType.trim() : DEFAULT_REL_TYPE;

        transactionRunner.runInTransaction(() -> {
            if (!repository.entryExists(entryA)) {
                throw new IllegalArgumentException("Entry " + entryA + " does not exist");
            }
            if (!repository.entryExists(entryB)) {
                throw new IllegalArgumentException("Entry " + entryB + " does not exist");
            }

            long from = Math.min(entryA, entryB);
            long to = Math.max(entryA, entryB);
            Instant now = clock.instant();

            KnowledgeRelationship rel = new KnowledgeRelationship(from, to, type, now);
            repository.addRelationship(rel);
        });
    }

    public void linkEntries(long entryA, long entryB) {
        linkEntries(entryA, entryB, DEFAULT_REL_TYPE);
    }

    /**
     * Removes an undirected relationship between two entries.
     */
    public void unlinkEntries(long entryA, long entryB, String relType) {
        if (entryA == entryB) return;
        String type = (relType != null && !relType.trim().isEmpty()) ? relType.trim() : DEFAULT_REL_TYPE;
        long from = Math.min(entryA, entryB);
        long to = Math.max(entryA, entryB);

        transactionRunner.runInTransaction(() -> {
            repository.removeRelationship(from, to, type);
        });
    }

    public void unlinkEntries(long entryA, long entryB) {
        unlinkEntries(entryA, entryB, DEFAULT_REL_TYPE);
    }

    /**
     * Finds all entry IDs related to the given entry ID across all relationship directions.
     */
    public List<Long> getRelatedEntryIds(long entryId) {
        return transactionRunner.runInTransaction(() -> {
            List<KnowledgeRelationship> rels = repository.getRelationshipsForEntry(entryId);
            List<Long> related = new ArrayList<>(rels.size());
            for (KnowledgeRelationship r : rels) {
                if (r.getFromId() == entryId) {
                    related.add(r.getToId());
                } else if (r.getToId() == entryId) {
                    related.add(r.getFromId());
                }
            }
            return related;
        });
    }
}
