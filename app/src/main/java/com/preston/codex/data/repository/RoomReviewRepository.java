package com.preston.codex.data.repository;

import com.preston.codex.data.dao.KnowledgeEntryDao;
import com.preston.codex.data.dao.ReviewDao;
import com.preston.codex.data.entity.ReviewRecordEntity;
import com.preston.codex.data.entity.ReviewScheduleEntity;
import com.preston.codex.domain.recall.ReviewRating;
import com.preston.codex.domain.recall.ReviewRecord;
import com.preston.codex.domain.recall.ReviewRepository;
import com.preston.codex.domain.recall.ReviewSchedule;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class RoomReviewRepository implements ReviewRepository {

    private final ReviewDao reviewDao;
    private final KnowledgeEntryDao knowledgeEntryDao;

    public RoomReviewRepository(ReviewDao reviewDao, KnowledgeEntryDao knowledgeEntryDao) {
        this.reviewDao = Objects.requireNonNull(reviewDao, "reviewDao must not be null");
        this.knowledgeEntryDao = Objects.requireNonNull(knowledgeEntryDao, "knowledgeEntryDao must not be null");
    }

    @Override
    public Optional<ReviewSchedule> getScheduleForEntry(long entryId) {
        ReviewScheduleEntity entity = reviewDao.findScheduleByEntryId(entryId);
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(new ReviewSchedule(
                entity.entryId,
                entity.step,
                LocalDate.parse(entity.nextReviewDate),
                Instant.ofEpochMilli(entity.lastReviewedAt),
                entity.reviewCount
        ));
    }

    @Override
    public void saveSchedule(ReviewSchedule schedule) {
        ReviewScheduleEntity entity = new ReviewScheduleEntity();
        entity.entryId = schedule.getEntryId();
        entity.step = schedule.getStep();
        entity.nextReviewDate = schedule.getNextReviewDate().toString();
        entity.lastReviewedAt = schedule.getLastReviewedAt().toEpochMilli();
        entity.reviewCount = schedule.getReviewCount();
        reviewDao.upsertSchedule(entity);
    }

    @Override
    public void recordReview(ReviewRecord record) {
        ReviewRecordEntity entity = new ReviewRecordEntity();
        entity.entryId = record.getEntryId();
        entity.reviewedAt = record.getReviewedAt().toEpochMilli();
        entity.reviewedLocalDate = record.getReviewedLocalDate().toString();
        entity.rating = record.getRating().name();
        entity.previousStep = record.getPreviousStep();
        entity.previousIntervalDays = record.getPreviousIntervalDays();
        entity.previousNextReviewDate = record.getRawPreviousNextReviewDate() != null ? record.getRawPreviousNextReviewDate().toString() : null;
        entity.newStep = record.getNewStep();
        entity.newIntervalDays = record.getNewIntervalDays();
        entity.nextReviewDate = record.getNextReviewDate().toString();
        reviewDao.insertRecord(entity);
    }

    @Override
    public List<ReviewSchedule> getSchedulesDueOnOrBefore(LocalDate asOfDate) {
        List<ReviewScheduleEntity> entities = reviewDao.findSchedulesDueOnOrBefore(asOfDate.toString());
        List<ReviewSchedule> result = new ArrayList<>(entities.size());
        for (ReviewScheduleEntity e : entities) {
            result.add(new ReviewSchedule(
                    e.entryId,
                    e.step,
                    LocalDate.parse(e.nextReviewDate),
                    Instant.ofEpochMilli(e.lastReviewedAt),
                    e.reviewCount
            ));
        }
        return result;
    }

    @Override
    public int countReviewsForDate(LocalDate date) {
        return reviewDao.countRecordsForDate(date.toString());
    }

    @Override
    public List<Long> getEligibleEntryIds() {
        return knowledgeEntryDao.findEligibleEntryIds();
    }

    @Override
    public boolean isEntryEligible(long entryId) {
        return knowledgeEntryDao.isEligibleById(entryId);
    }

    @Override
    public List<ReviewRecord> getHistoryForEntry(long entryId) {
        List<ReviewRecordEntity> entities = reviewDao.findRecordsByEntryId(entryId);
        List<ReviewRecord> result = new ArrayList<>(entities.size());
        for (ReviewRecordEntity e : entities) {
            LocalDate prevNextDate = e.previousNextReviewDate != null ? LocalDate.parse(e.previousNextReviewDate) : null;
            result.add(new ReviewRecord(
                    e.id,
                    e.entryId,
                    Instant.ofEpochMilli(e.reviewedAt),
                    LocalDate.parse(e.reviewedLocalDate),
                    ReviewRating.valueOf(e.rating),
                    e.previousStep,
                    e.previousIntervalDays,
                    prevNextDate,
                    e.newStep,
                    e.newIntervalDays,
                    LocalDate.parse(e.nextReviewDate)
            ));
        }
        return result;
    }
}
