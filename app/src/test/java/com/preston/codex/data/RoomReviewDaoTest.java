package com.preston.codex.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.preston.codex.data.dao.KnowledgeEntryDao;
import com.preston.codex.data.dao.KnowledgeSourceDao;
import com.preston.codex.data.dao.ReviewDao;
import com.preston.codex.data.database.AppDatabase;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeSourceEntity;
import com.preston.codex.data.entity.ReviewRecordEntity;
import com.preston.codex.data.entity.ReviewScheduleEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class RoomReviewDaoTest {

    private AppDatabase db;
    private ReviewDao reviewDao;
    private KnowledgeEntryDao entryDao;
    private KnowledgeSourceDao sourceDao;
    private long entryId;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        reviewDao = db.reviewDao();
        entryDao = db.knowledgeEntryDao();
        sourceDao = db.knowledgeSourceDao();

        KnowledgeSourceEntity source = new KnowledgeSourceEntity();
        source.title = "Source";
        source.titleKey = "source";
        source.createdAt = 1000L;
        source.updatedAt = 1000L;
        long sourceId = sourceDao.insert(source);

        KnowledgeEntryEntity entry = new KnowledgeEntryEntity();
        entry.sourceId = sourceId;
        entry.fingerprint = "fp-review";
        entry.contentHash = "ch-review";
        entry.importedHash = "ih-review";
        entry.quoteText = "To review or not to review";
        entry.createdAt = 1000L;
        entry.updatedAt = 1000L;
        entryId = entryDao.insert(entry);
    }

    @After
    public void tearDown() {
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void upsertSchedule_insertsAndUpdatesOnConflict() {
        ReviewScheduleEntity schedule = new ReviewScheduleEntity();
        schedule.entryId = entryId;
        schedule.step = 0;
        schedule.nextReviewDate = "2026-10-26";
        schedule.lastReviewedAt = 1000L;
        schedule.reviewCount = 1;
        reviewDao.upsertSchedule(schedule);

        ReviewScheduleEntity read = reviewDao.findScheduleByEntryId(entryId);
        assertNotNull(read);
        assertEquals(0, read.step);
        assertEquals("2026-10-26", read.nextReviewDate);

        // Update schedule (advances to step 1)
        schedule.step = 1;
        schedule.nextReviewDate = "2026-10-28";
        schedule.reviewCount = 2;
        reviewDao.upsertSchedule(schedule);

        ReviewScheduleEntity updated = reviewDao.findScheduleByEntryId(entryId);
        assertEquals(1, updated.step);
        assertEquals("2026-10-28", updated.nextReviewDate);
        assertEquals(2, updated.reviewCount);
        assertEquals(1, reviewDao.countSchedules());
    }

    @Test
    public void countRecordsForDate_countsOnlySpecifiedDate() {
        ReviewRecordEntity r1 = new ReviewRecordEntity();
        r1.entryId = entryId;
        r1.reviewedAt = 1000L;
        r1.reviewedLocalDate = "2026-10-25";
        r1.rating = "REMEMBERED";
        r1.previousStep = 0;
        r1.previousIntervalDays = 1;
        r1.newStep = 1;
        r1.newIntervalDays = 3;
        r1.nextReviewDate = "2026-10-28";
        reviewDao.insertRecord(r1);

        ReviewRecordEntity r2 = new ReviewRecordEntity();
        r2.entryId = entryId;
        r2.reviewedAt = 2000L;
        r2.reviewedLocalDate = "2026-10-25";
        r2.rating = "EASY";
        r2.previousStep = 1;
        r2.previousIntervalDays = 3;
        r2.newStep = 3;
        r2.newIntervalDays = 14;
        r2.nextReviewDate = "2026-11-08";
        reviewDao.insertRecord(r2);

        ReviewRecordEntity r3 = new ReviewRecordEntity();
        r3.entryId = entryId;
        r3.reviewedAt = 3000L;
        r3.reviewedLocalDate = "2026-10-26";
        r3.rating = "FORGOT";
        r3.previousStep = 3;
        r3.previousIntervalDays = 14;
        r3.newStep = 0;
        r3.newIntervalDays = 1;
        r3.nextReviewDate = "2026-10-27";
        reviewDao.insertRecord(r3);

        assertEquals(2, reviewDao.countRecordsForDate("2026-10-25"));
        assertEquals(1, reviewDao.countRecordsForDate("2026-10-26"));
        assertEquals(0, reviewDao.countRecordsForDate("2026-10-27"));
    }

    @Test
    public void findSchedulesDueOnOrBefore_filtersCorrectly() {
        ReviewScheduleEntity dueYesterday = new ReviewScheduleEntity();
        dueYesterday.entryId = entryId;
        dueYesterday.step = 1;
        dueYesterday.nextReviewDate = "2026-10-24";
        dueYesterday.lastReviewedAt = 1000L;
        dueYesterday.reviewCount = 1;
        reviewDao.upsertSchedule(dueYesterday);

        List<ReviewScheduleEntity> due = reviewDao.findSchedulesDueOnOrBefore("2026-10-25");
        assertEquals(1, due.size());
        assertEquals(entryId, due.get(0).entryId);

        List<ReviewScheduleEntity> dueEarlier = reviewDao.findSchedulesDueOnOrBefore("2026-10-23");
        assertTrue(dueEarlier.isEmpty());
    }
}
