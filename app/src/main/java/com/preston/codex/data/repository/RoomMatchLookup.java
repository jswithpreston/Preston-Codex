package com.preston.codex.data.repository;

import com.preston.codex.data.dao.KnowledgeEntryDao;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.importer.MatchLookup;
import com.preston.codex.importer.model.ExistingEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter providing database lookups to the pure-Java EntryMatcher.
 */
public class RoomMatchLookup implements MatchLookup {

    private final KnowledgeEntryDao entryDao;

    public RoomMatchLookup(KnowledgeEntryDao entryDao) {
        this.entryDao = entryDao;
    }

    @Override
    public ExistingEntry findByStableId(String stableId) {
        if (stableId == null) return null;
        return toExistingEntry(entryDao.findByStableId(stableId));
    }

    @Override
    public ExistingEntry findByFingerprint(String fingerprint) {
        if (fingerprint == null) return null;
        return toExistingEntry(entryDao.findByFingerprint(fingerprint));
    }

    @Override
    public List<ExistingEntry> findAllCandidates() {
        List<KnowledgeEntryEntity> entities = entryDao.getAllEntries();
        List<ExistingEntry> result = new ArrayList<>(entities.size());
        for (KnowledgeEntryEntity entity : entities) {
            result.add(toExistingEntry(entity));
        }
        return result;
    }

    private ExistingEntry toExistingEntry(KnowledgeEntryEntity e) {
        if (e == null) return null;
        return new ExistingEntry(
                e.id,
                e.sourceId,
                e.stableId,
                e.fingerprint,
                e.contentHash,
                e.importedHash,
                e.quoteText,
                e.isArchived,
                e.isTrashed
        );
    }
}
