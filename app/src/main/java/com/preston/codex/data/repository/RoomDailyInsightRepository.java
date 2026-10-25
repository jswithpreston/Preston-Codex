package com.preston.codex.data.repository;

import com.preston.codex.data.dao.DailyInsightDao;
import com.preston.codex.data.dao.KnowledgeEntryDao;
import com.preston.codex.data.entity.DailyInsightHistoryEntity;
import com.preston.codex.data.entity.DailyInsightSelectionEntity;
import com.preston.codex.domain.scheduling.DailyInsightHistory;
import com.preston.codex.domain.scheduling.DailyInsightHistoryReason;
import com.preston.codex.domain.scheduling.DailyInsightRepository;
import com.preston.codex.domain.scheduling.DailyInsightSelection;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RoomDailyInsightRepository implements DailyInsightRepository {

    private final DailyInsightDao dailyInsightDao;
    private final KnowledgeEntryDao knowledgeEntryDao;

    public RoomDailyInsightRepository(DailyInsightDao dailyInsightDao, KnowledgeEntryDao knowledgeEntryDao) {
        this.dailyInsightDao = Objects.requireNonNull(dailyInsightDao, "dailyInsightDao must not be null");
        this.knowledgeEntryDao = Objects.requireNonNull(knowledgeEntryDao, "knowledgeEntryDao must not be null");
    }

    @Override
    public Optional<DailyInsightSelection> getSelectionForDate(LocalDate date) {
        DailyInsightSelectionEntity entity = dailyInsightDao.findSelectionByDate(date.toString());
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public boolean insertOrIgnoreSelection(DailyInsightSelection selection) {
        DailyInsightSelectionEntity entity = toEntity(selection);
        long rowId = dailyInsightDao.insertSelection(entity);
        return rowId != -1;
    }

    @Override
    public void updateSelection(DailyInsightSelection selection) {
        dailyInsightDao.updateSelection(toEntity(selection));
    }

    @Override
    public void recordHistory(DailyInsightHistory history) {
        DailyInsightHistoryEntity entity = new DailyInsightHistoryEntity();
        entity.localDate = history.getLocalDate().toString();
        entity.entryId = history.getRawEntryId();
        entity.reason = history.getReason().name();
        entity.selectedAt = history.getSelectedAt().toEpochMilli();
        dailyInsightDao.insertHistory(entity);
    }

    @Override
    public List<DailyInsightHistory> getHistoryForDate(LocalDate date) {
        List<DailyInsightHistoryEntity> entities = dailyInsightDao.getHistoryByDate(date.toString());
        List<DailyInsightHistory> result = new ArrayList<>(entities.size());
        for (DailyInsightHistoryEntity e : entities) {
            result.add(new DailyInsightHistory(
                    e.id,
                    LocalDate.parse(e.localDate),
                    e.entryId,
                    DailyInsightHistoryReason.valueOf(e.reason),
                    Instant.ofEpochMilli(e.selectedAt)
            ));
        }
        return result;
    }

    @Override
    public Optional<DailyInsightSelection> getMostRecentPriorSelection(LocalDate beforeDate) {
        DailyInsightSelectionEntity entity = dailyInsightDao.findMostRecentPriorSelection(beforeDate.toString());
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(toDomain(entity));
    }

    @Override
    public List<Long> getEligibleEntryIds() {
        return knowledgeEntryDao.findEligibleEntryIds();
    }

    @Override
    public boolean isEntryEligible(long entryId) {
        return knowledgeEntryDao.isEligibleById(entryId);
    }

    private static DailyInsightSelection toDomain(DailyInsightSelectionEntity entity) {
        return new DailyInsightSelection(
                LocalDate.parse(entity.localDate),
                entity.entryId,
                Instant.ofEpochMilli(entity.selectedAt),
                entity.wasShuffled
        );
    }

    private static DailyInsightSelectionEntity toEntity(DailyInsightSelection domain) {
        DailyInsightSelectionEntity entity = new DailyInsightSelectionEntity();
        entity.localDate = domain.getLocalDate().toString();
        entity.entryId = domain.getRawEntryId();
        entity.selectedAt = domain.getSelectedAt().toEpochMilli();
        entity.wasShuffled = domain.isWasShuffled();
        return entity;
    }
}
