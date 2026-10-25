package com.preston.codex.domain.knowledge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.preston.codex.domain.TransactionRunner;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class BookmarkServiceTest {

    private InMemoryKnowledgeDomainRepository repository;
    private DirectTransactionRunner transactionRunner;
    private BookmarkService service;

    @Before
    public void setUp() {
        repository = new InMemoryKnowledgeDomainRepository();
        transactionRunner = new DirectTransactionRunner();
        service = new BookmarkService(repository, transactionRunner);
    }

    @Test
    public void setBookmark_addsAndRemovesBookmark() {
        repository.addEntry(1L);

        assertFalse(service.isBookmarked(1L));

        service.setBookmark(1L, true);
        assertTrue(service.isBookmarked(1L));

        service.setBookmark(1L, false);
        assertFalse(service.isBookmarked(1L));
    }

    @Test
    public void getBookmarks_returnsOnlyBookmarkedEntries() {
        repository.addEntry(1L);
        repository.addEntry(2L);
        repository.addEntry(3L);

        service.setBookmark(1L, true);
        service.setBookmark(3L, true);

        List<Long> bookmarked = service.getBookmarkedEntryIds();
        assertEquals(2, bookmarked.size());
        assertTrue(bookmarked.contains(1L));
        assertTrue(bookmarked.contains(3L));
        assertFalse(bookmarked.contains(2L));
    }

    @Test
    public void setBookmark_nonExistentEntry_throwsIllegalArgumentException() {
        try {
            service.setBookmark(999L, true);
            fail("Must throw when bookmarking non-existent entry");
        } catch (IllegalArgumentException expected) {
            assertTrue(expected.getMessage().contains("does not exist"));
        }
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
        public void addRelationship(KnowledgeRelationship relationship) {}

        @Override
        public void removeRelationship(long fromId, long toId, String relType) {}

        @Override
        public List<KnowledgeRelationship> getRelationshipsForEntry(long entryId) {
            return new ArrayList<>();
        }
    }
}
