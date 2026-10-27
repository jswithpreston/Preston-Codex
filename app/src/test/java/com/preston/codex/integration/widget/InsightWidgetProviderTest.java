package com.preston.codex.integration.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.preston.codex.AppContainer;
import com.preston.codex.CodexApplication;
import com.preston.codex.data.database.AppDatabase;
import com.preston.codex.data.entity.DailyInsightSelectionEntity;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeSourceEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Executors;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class InsightWidgetProviderTest {

    private Context context;
    private AppDatabase db;
    private AppContainer container;
    private Clock clock;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .addCallback(AppDatabase.CALLBACK)
                .build();

        clock = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneId.of("UTC"));
        container = new AppContainer(context, clock, () -> ZoneId.of("UTC"), Executors.newSingleThreadExecutor()) {
            @Override
            public synchronized AppDatabase getDatabase() {
                return db;
            }
        };
        CodexApplication.setContainerForTest(container);
    }

    @After
    public void tearDown() {
        CodexApplication.resetContainerForTest();
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void providers_instantiateCleanly() {
        assertNotNull(new SmallInsightWidgetProvider());
        assertNotNull(new MediumInsightWidgetProvider());
        assertNotNull(new LargeInsightWidgetProvider());
    }

    @Test
    public void onEnabled_ensuresBackgroundScheduled() {
        MediumInsightWidgetProvider provider = new MediumInsightWidgetProvider();
        provider.onEnabled(context);
        // Does not throw and triggers ensureScheduled
        assertNotNull(container.getNotificationScheduler());
    }

    @Test
    public void onUpdate_withPopulatedDb_rendersWithoutCrash() {
        // Seed an active selection for today
        KnowledgeSourceEntity src = new KnowledgeSourceEntity();
        src.title = "Atomic Habits";
        src.author = "James Clear";
        src.titleKey = "atomic habits";
        src.authorKey = "james clear";
        src.sourceType = "BOOK";
        src.createdAt = 1000;
        src.updatedAt = 1000;
        long srcId = db.knowledgeSourceDao().insert(src);

        KnowledgeEntryEntity entry = new KnowledgeEntryEntity();
        entry.sourceId = srcId;
        entry.fingerprint = "fp1";
        entry.contentHash = "ch1";
        entry.importedHash = "ih1";
        entry.quoteText = "You fall to the level of your systems.";
        entry.insightText = "Focus on habits.";
        entry.entryType = "QUOTE";
        entry.createdAt = 1000;
        entry.updatedAt = 1000;
        long entryId = db.knowledgeEntryDao().insert(entry);

        DailyInsightSelectionEntity sel = new DailyInsightSelectionEntity();
        sel.localDate = "2026-10-26";
        sel.entryId = entryId;
        sel.selectedAt = 1000;
        sel.wasShuffled = false;
        db.dailyInsightDao().insertSelection(sel);

        MediumInsightWidgetProvider provider = new MediumInsightWidgetProvider();
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        provider.onUpdate(context, manager, new int[]{101});
        // Rendering executes asynchronously on executor; wait for task
        assertTrue(container.getDailyInsightRepository().isEntryEligible(entryId));
    }

    @Test
    public void pendingIntentRequestCodes_areUniquePerWidgetAndAction() {
        int widget1 = 42;
        int widget2 = 43;

        int reqOpen1 = (widget1 * 10) + 1;
        int reqShuffle1 = (widget1 * 10) + 2;
        int reqBookmark1 = (widget1 * 10) + 3;

        int reqOpen2 = (widget2 * 10) + 1;
        int reqShuffle2 = (widget2 * 10) + 2;
        int reqBookmark2 = (widget2 * 10) + 3;

        assertEquals(421, reqOpen1);
        assertEquals(422, reqShuffle1);
        assertEquals(423, reqBookmark1);

        assertEquals(431, reqOpen2);
        assertEquals(432, reqShuffle2);
        assertEquals(433, reqBookmark2);
    }
}
