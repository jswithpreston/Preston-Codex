package com.preston.codex.data.repository;

import com.preston.codex.data.dao.DailyInsightDao;
import com.preston.codex.data.dao.KnowledgeEntryDao;
import com.preston.codex.data.dao.KnowledgeSourceDao;
import com.preston.codex.data.entity.DailyInsightSelectionEntity;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeSourceEntity;
import com.preston.codex.domain.scheduling.InsightDetails;
import com.preston.codex.domain.scheduling.InsightDetailsRepository;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Room-backed implementation of {@link InsightDetailsRepository}.
 * Assembles domain {@link InsightDetails} from Room DAOs.
 */
public final class RoomInsightDetailsRepository implements InsightDetailsRepository {

    private final DailyInsightDao dailyInsightDao;
    private final KnowledgeEntryDao knowledgeEntryDao;
    private final KnowledgeSourceDao knowledgeSourceDao;

    public RoomInsightDetailsRepository(
            DailyInsightDao dailyInsightDao,
            KnowledgeEntryDao knowledgeEntryDao,
            KnowledgeSourceDao knowledgeSourceDao
    ) {
        this.dailyInsightDao = Objects.requireNonNull(dailyInsightDao, "dailyInsightDao must not be null");
        this.knowledgeEntryDao = Objects.requireNonNull(knowledgeEntryDao, "knowledgeEntryDao must not be null");
        this.knowledgeSourceDao = Objects.requireNonNull(knowledgeSourceDao, "knowledgeSourceDao must not be null");
    }

    @Override
    public Optional<InsightDetails> getDailyInsightDetails(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        DailyInsightSelectionEntity selection = dailyInsightDao.findSelectionByDate(date.toString());
        if (selection == null || selection.entryId == null) {
            return Optional.empty();
        }
        return getInsightDetailsById(selection.entryId);
    }

    @Override
    public Optional<InsightDetails> getInsightDetailsById(long entryId) {
        KnowledgeEntryEntity entry = knowledgeEntryDao.findById(entryId);
        if (entry == null || entry.isArchived || entry.isTrashed) {
            return Optional.empty();
        }

        KnowledgeSourceEntity source = knowledgeSourceDao.findById(entry.sourceId);
        String author = (source != null) ? source.author : "";
        String sourceTitle = (source != null) ? source.title : "";

        return Optional.of(new InsightDetails(
                entry.id,
                entry.quoteText,
                author,
                sourceTitle,
                entry.insightText,
                entry.isBookmarked
        ));
    }
}
