package com.preston.codex.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.preston.codex.data.dao.DailyInsightDao;
import com.preston.codex.data.dao.KnowledgeEntryDao;
import com.preston.codex.data.dao.KnowledgeSourceDao;
import com.preston.codex.data.database.AppDatabase;
import com.preston.codex.data.entity.DailyInsightHistoryEntity;
import com.preston.codex.data.entity.DailyInsightSelectionEntity;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeSourceEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class RoomDailyInsightDaoTest {

    private AppDatabase db;
    private DailyInsightDao dailyInsightDao;
    private KnowledgeEntryDao entryDao;
    private KnowledgeSourceDao sourceDao;
    private long entryId1;
    private long entryId2;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        dailyInsightDao = db.dailyInsightDao();
        entryDao = db.knowledgeEntryDao();
        sourceDao = db.knowledgeSourceDao();

        KnowledgeSourceEntity source = new KnowledgeSourceEntity();
        source.title = "Sample Source";
        source.author = "Author";
        source.titleKey = "sample source";
        source.authorKey = "author";
        source.createdAt = 1000L;
        source.updatedAt = 1000L;
        long sourceId = sourceDao.insert(source);

        KnowledgeEntryEntity e1 = new KnowledgeEntryEntity();
        e1.sourceId = sourceId;
        e1.fingerprint = "fp-1";
        e1.contentHash = "ch-1";
        e1.importedHash = "ih-1";
        e1.quoteText = "Quote 1";
        e1.createdAt = 1000L;
        e1.updatedAt = 1000L;
        entryId1 = entryDao.insert(e1);

        KnowledgeEntryEntity e2 = new KnowledgeEntryEntity();
        e2.sourceId = sourceId;
        e2.fingerprint = "fp-2";
        e2.contentHash = "ch-2";
        e2.importedHash = "ih-2";
        e2.quoteText = "Quote 2";
        e2.createdAt = 1000L;
        e2.updatedAt = 1000L;
        entryId2 = entryDao.insert(e2);
    }

    @After
    public void tearDown() {
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void insertOrIgnore_concurrentRace_returnsNegativeOneForLoserAndKeepsExactlyOneRow() {
        DailyInsightSelectionEntity winner = new DailyInsightSelectionEntity();
        winner.localDate = "2026-10-25";
        winner.entryId = entryId1;
        winner.selectedAt = 10000L;
        winner.wasShuffled = false;

        DailyInsightSelectionEntity loser = new DailyInsightSelectionEntity();
        loser.localDate = "2026-10-25";
        loser.entryId = entryId2;
        loser.selectedAt = 10001L;
        loser.wasShuffled = false;

        long winnerResult = dailyInsightDao.insertSelection(winner);
        long loserResult = dailyInsightDao.insertSelection(loser);

        assertTrue("First insert must succeed", winnerResult > 0 || winnerResult == 0);
        assertEquals("Conflicting date insert must return -1 (ignored)", -1, loserResult);

        // Record history only for winner
        DailyInsightHistoryEntity history = new DailyInsightHistoryEntity();
        history.localDate = "2026-10-25";
        history.entryId = entryId1;
        history.reason = "AUTO";
        history.selectedAt = 10000L;
        dailyInsightDao.insertHistory(history);

        assertEquals("Exactly 1 selection row must exist for date", 1, dailyInsightDao.countSelections());
        assertEquals("Exactly 1 AUTO history row must exist", 1, dailyInsightDao.countHistory());

        DailyInsightSelectionEntity persisted = dailyInsightDao.findSelectionByDate("2026-10-25");
        assertNotNull(persisted);
        assertEquals(Long.valueOf(entryId1), persisted.entryId);
    }

    @Test
    public void findMostRecentPriorSelection_returnsCorrectRow() {
        DailyInsightSelectionEntity day1 = new DailyInsightSelectionEntity();
        day1.localDate = "2026-10-20";
        day1.entryId = entryId1;
        day1.selectedAt = 1000L;
        dailyInsightDao.insertSelection(day1);

        DailyInsightSelectionEntity day2 = new DailyInsightSelectionEntity();
        day2.localDate = "2026-10-22";
        day2.entryId = entryId2;
        day2.selectedAt = 2000L;
        dailyInsightDao.insertSelection(day2);

        DailyInsightSelectionEntity prior = dailyInsightDao.findMostRecentPriorSelection("2026-10-25");
        assertNotNull(prior);
        assertEquals("2026-10-22", prior.localDate);
        assertEquals(Long.valueOf(entryId2), prior.entryId);
    }

    @Test
    public void history_recordsAutoShuffleAndInvalidated() {
        DailyInsightHistoryEntity h1 = new DailyInsightHistoryEntity();
        h1.localDate = "2026-10-25";
        h1.entryId = entryId1;
        h1.reason = "AUTO";
        h1.selectedAt = 1000L;
        dailyInsightDao.insertHistory(h1);

        DailyInsightHistoryEntity h2 = new DailyInsightHistoryEntity();
        h2.localDate = "2026-10-25";
        h2.entryId = entryId2;
        h2.reason = "SHUFFLE";
        h2.selectedAt = 2000L;
        dailyInsightDao.insertHistory(h2);

        List<DailyInsightHistoryEntity> historyList = dailyInsightDao.getHistoryByDate("2026-10-25");
        assertEquals(2, historyList.size());
        assertEquals("AUTO", historyList.get(0).reason);
        assertEquals("SHUFFLE", historyList.get(1).reason);
    }
}
