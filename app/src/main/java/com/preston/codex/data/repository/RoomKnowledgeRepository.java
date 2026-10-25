package com.preston.codex.data.repository;

import com.preston.codex.data.dao.KnowledgeEntryDao;
import com.preston.codex.data.dao.KnowledgeRelationshipDao;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeRelationshipEntity;
import com.preston.codex.domain.knowledge.KnowledgeDomainRepository;
import com.preston.codex.domain.knowledge.KnowledgeRelationship;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RoomKnowledgeRepository implements KnowledgeDomainRepository {

    private final KnowledgeEntryDao knowledgeEntryDao;
    private final KnowledgeRelationshipDao knowledgeRelationshipDao;

    public RoomKnowledgeRepository(KnowledgeEntryDao knowledgeEntryDao, KnowledgeRelationshipDao knowledgeRelationshipDao) {
        this.knowledgeEntryDao = Objects.requireNonNull(knowledgeEntryDao, "knowledgeEntryDao must not be null");
        this.knowledgeRelationshipDao = Objects.requireNonNull(knowledgeRelationshipDao, "knowledgeRelationshipDao must not be null");
    }

    @Override
    public boolean entryExists(long entryId) {
        return knowledgeEntryDao.existsById(entryId);
    }

    @Override
    public void setBookmark(long entryId, boolean isBookmarked) {
        knowledgeEntryDao.updateBookmark(entryId, isBookmarked);
    }

    @Override
    public boolean isBookmarked(long entryId) {
        KnowledgeEntryEntity entry = knowledgeEntryDao.findById(entryId);
        return entry != null && entry.isBookmarked;
    }

    @Override
    public List<Long> getBookmarkedEntryIds() {
        return knowledgeEntryDao.findBookmarkedEntryIds();
    }

    @Override
    public void addRelationship(KnowledgeRelationship relationship) {
        KnowledgeRelationshipEntity entity = new KnowledgeRelationshipEntity();
        entity.fromId = relationship.getFromId();
        entity.toId = relationship.getToId();
        entity.relType = relationship.getRelType();
        entity.createdAt = relationship.getCreatedAt().toEpochMilli();
        knowledgeRelationshipDao.insert(entity);
    }

    @Override
    public void removeRelationship(long fromId, long toId, String relType) {
        knowledgeRelationshipDao.deleteRelationship(fromId, toId, relType);
    }

    @Override
    public List<KnowledgeRelationship> getRelationshipsForEntry(long entryId) {
        List<KnowledgeRelationshipEntity> entities = knowledgeRelationshipDao.getRelationshipsForEntry(entryId);
        List<KnowledgeRelationship> result = new ArrayList<>(entities.size());
        for (KnowledgeRelationshipEntity e : entities) {
            result.add(new KnowledgeRelationship(
                    e.fromId,
                    e.toId,
                    e.relType,
                    Instant.ofEpochMilli(e.createdAt)
            ));
        }
        return result;
    }
}
