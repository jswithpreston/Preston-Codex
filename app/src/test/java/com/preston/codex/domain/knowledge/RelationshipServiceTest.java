package com.preston.codex.domain.knowledge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.preston.codex.domain.TransactionRunner;

import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class RelationshipServiceTest {

    private InMemoryKnowledgeDomainRepository repository;
    private Clock clock;
    private DirectTransactionRunner transactionRunner;
    private KnowledgeRelationshipService service;

    @Before
    public void setUp() {
        repository = new InMemoryKnowledgeDomainRepository();
        clock = Clock.fixed(Instant.parse("2026-10-25T12:00:00Z"), ZoneOffset.UTC);
        transactionRunner = new DirectTransactionRunner();
        service = new KnowledgeRelationshipService(repository, clock, transactionRunner);
    }

    @Test
    public void linkEntries_normalizesFromLessThanTo() {
        repository.addEntry(10L);
        repository.addEntry(5L);

        // Link with higher ID first
        service.linkEntries(10L, 5L);

        List<KnowledgeRelationship> rels = repository.getRelationshipsForEntry(5L);
        assertEquals(1, rels.size());
        assertEquals(5L, rels.get(0).getFromId());
        assertEquals(10L, rels.get(0).getToId());
    }

    @Test
    public void linkEntries_reversedOrderProducesSamePair() {
        repository.addEntry(1L);
        repository.addEntry(2L);

        service.linkEntries(1L, 2L);
        service.linkEntries(2L, 1L);

        assertEquals("Reversed order must produce the exact same single relationship", 1, repository.relationships.size());
    }

    @Test
    public void linkEntries_idempotentLinkingTwiceLeavesOneRow() {
        repository.addEntry(1L);
        repository.addEntry(2L);

        service.linkEntries(1L, 2L);
        service.linkEntries(1L, 2L);

        assertEquals("Linking twice must not throw and must maintain exactly 1 row", 1, repository.relationships.size());
    }

    @Test
    public void linkEntries_selfLinkThrowsIllegalArgumentException() {
        repository.addEntry(1L);

        try {
            service.linkEntries(1L, 1L);
            fail("Self-linking must throw IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("itself"));
        }
    }

    @Test
    public void unlinkEntries_removesRelationship() {
        repository.addEntry(1L);
        repository.addEntry(2L);

        service.linkEntries(1L, 2L);
        assertFalse(repository.relationships.isEmpty());

        service.unlinkEntries(2L, 1L);
        assertTrue(repository.relationships.isEmpty());
    }

    @Test
    public void getRelatedEntryIds_bidirectionalLookup() {
        repository.addEntry(1L);
        repository.addEntry(2L);
        repository.addEntry(3L);

        service.linkEntries(1L, 2L);
        service.linkEntries(1L, 3L);

        List<Long> relatedTo1 = service.getRelatedEntryIds(1L);
        assertEquals(2, relatedTo1.size());
        assertTrue(relatedTo1.contains(2L));
        assertTrue(relatedTo1.contains(3L));

        List<Long> relatedTo2 = service.getRelatedEntryIds(2L);
        assertEquals(1, relatedTo2.size());
        assertTrue(relatedTo2.contains(1L));
    }

    // --- In-Memory Test Fixtures ---

    static class DirectTransactionRunner implements TransactionRunner {
        @Override
        public <T> T runInTransaction(Supplier<T> action) {
            return action.get();
        }

        @Override
        public void runInTransaction(Runnable action) {
            action.run();
        }
    }

    static class InMemoryKnowledgeDomainRepository implements KnowledgeDomainRepository {
        final Set<Long> entries = new HashSet<>();
        final Set<Long> bookmarks = new HashSet<>();
        final List<KnowledgeRelationship> relationships = new ArrayList<>();

        void addEntry(long id) { entries.add(id); }

        @Override
        public boolean entryExists(long entryId) {
            return entries.contains(entryId);
        }

        @Override
        public void setBookmark(long entryId, boolean isBookmarked) {
            if (isBookmarked) bookmarks.add(entryId);
            else bookmarks.remove(entryId);
        }

        @Override
        public boolean isBookmarked(long entryId) {
            return bookmarks.contains(entryId);
        }

        @Override
        public List<Long> getBookmarkedEntryIds() {
            return new ArrayList<>(bookmarks);
        }

        @Override
        public void addRelationship(KnowledgeRelationship relationship) {
            for (KnowledgeRelationship r : relationships) {
                if (r.getFromId() == relationship.getFromId() &&
                        r.getToId() == relationship.getToId() &&
                        r.getRelType().equals(relationship.getRelType())) {
                    return; // duplicate ignored
                }
            }
            relationships.add(relationship);
        }

        @Override
        public void removeRelationship(long fromId, long toId, String relType) {
            relationships.removeIf(r -> r.getFromId() == fromId && r.getToId() == toId && r.getRelType().equals(relType));
        }

        @Override
        public List<KnowledgeRelationship> getRelationshipsForEntry(long entryId) {
            List<KnowledgeRelationship> result = new ArrayList<>();
            for (KnowledgeRelationship r : relationships) {
                if (r.getFromId() == entryId || r.getToId() == entryId) {
                    result.add(r);
                }
            }
            return result;
        }
    }
}
