package com.preston.codex.domain.knowledge;

import com.preston.codex.domain.TransactionRunner;

import java.util.List;
import java.util.Objects;

/**
 * Service for toggling and reading persistent bookmarks.
 * Ensures that bookmark status changes do not modify entry timestamps or content hashes.
 */
public final class BookmarkService {

    private final KnowledgeDomainRepository repository;
    private final TransactionRunner transactionRunner;

    public BookmarkService(KnowledgeDomainRepository repository, TransactionRunner transactionRunner) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.transactionRunner = Objects.requireNonNull(transactionRunner, "transactionRunner must not be null");
    }

    public void setBookmark(long entryId, boolean isBookmarked) {
        transactionRunner.runInTransaction(() -> {
            if (!repository.entryExists(entryId)) {
                throw new IllegalArgumentException("Entry " + entryId + " does not exist");
            }
            repository.setBookmark(entryId, isBookmarked);
        });
    }

    public boolean isBookmarked(long entryId) {
        return transactionRunner.runInTransaction(() -> repository.isBookmarked(entryId));
    }

    public List<Long> getBookmarkedEntryIds() {
        return transactionRunner.runInTransaction(repository::getBookmarkedEntryIds);
    }
}
