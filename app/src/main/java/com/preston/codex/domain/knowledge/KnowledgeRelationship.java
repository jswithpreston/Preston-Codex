package com.preston.codex.domain.knowledge;

import java.time.Instant;
import java.util.Objects;

/**
 * Domain entity representing an undirected relationship between two knowledge entries.
 * Enforces the invariant fromId &lt; toId.
 */
public final class KnowledgeRelationship {

    private final long fromId;
    private final long toId;
    private final String relType;
    private final Instant createdAt;

    public KnowledgeRelationship(long fromId, long toId, String relType, Instant createdAt) {
        if (fromId >= toId) {
            throw new IllegalArgumentException("Invariant violation: fromId (" + fromId + ") must be < toId (" + toId + ")");
        }
        this.fromId = fromId;
        this.toId = toId;
        this.relType = Objects.requireNonNull(relType, "relType must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public long getFromId() {
        return fromId;
    }

    public long getToId() {
        return toId;
    }

    public String getRelType() {
        return relType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KnowledgeRelationship that = (KnowledgeRelationship) o;
        return fromId == that.fromId &&
                toId == that.toId &&
                Objects.equals(relType, that.relType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromId, toId, relType);
    }

    @Override
    public String toString() {
        return "KnowledgeRelationship{" +
                "fromId=" + fromId +
                ", toId=" + toId +
                ", relType='" + relType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
