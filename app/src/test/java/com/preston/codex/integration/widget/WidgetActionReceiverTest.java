package com.preston.codex.integration.widget;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class WidgetActionReceiverTest {

    private Context context;
    private AppDatabase db;
    private AppContainer container;
    private ExecutorService executor;
    private long entry1Id;
    private long entry2Id;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .addCallback(AppDatabase.CALLBACK)
                .build();

        KnowledgeSourceEntity src = new KnowledgeSourceEntity();
        src.title = "Source";
        src.author = "Author";
        src.titleKey = "source";
        src.authorKey = "author";
        src.sourceType = "BOOK";
        src.createdAt = 1000;
        src.updatedAt = 1000;
        long srcId = db.knowledgeSourceDao().insert(src);

        KnowledgeEntryEntity e1 = new KnowledgeEntryEntity();
        e1.sourceId = srcId;
        e1.fingerprint = "fp1";
        e1.contentHash = "ch1";
        e1.importedHash = "ih1";
        e1.quoteText = "Quote 1";
        e1.entryType = "QUOTE";
        e1.createdAt = 1000;
        e1.updatedAt = 1000;
        entry1Id = db.knowledgeEntryDao().insert(e1);

        KnowledgeEntryEntity e2 = new KnowledgeEntryEntity();
        e2.sourceId = srcId;
        e2.fingerprint = "fp2";
        e2.contentHash = "ch2";
        e2.importedHash = "ih2";
        e2.quoteText = "Quote 2";
        e2.entryType = "QUOTE";
        e2.createdAt = 1000;
        e2.updatedAt = 1000;
        entry2Id = db.knowledgeEntryDao().insert(e2);

        DailyInsightSelectionEntity sel = new DailyInsightSelectionEntity();
        sel.localDate = "2026-10-26";
        sel.entryId = entry1Id;
        sel.selectedAt = 1000;
        sel.wasShuffled = false;
        db.dailyInsightDao().insertSelection(sel);

        executor = Executors.newSingleThreadExecutor();
        Clock clock = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneId.of("UTC"));
        container = new AppContainer(context, clock, () -> ZoneId.of("UTC"), executor) {
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
        executor.shutdown();
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void shuffleAction_updatesCanonicalSelection() throws Exception {
        Intent intent = new Intent(WidgetRenderer.ACTION_SHUFFLE);
        WidgetActionReceiver receiver = new WidgetActionReceiver();

        receiver.onReceive(context, intent);
        executor.submit(() -> {}).get(2, TimeUnit.SECONDS);

        DailyInsightSelectionEntity sel = db.dailyInsightDao().findSelectionByDate("2026-10-26");
        assertTrue("Selection should be marked wasShuffled", sel.wasShuffled);
        assertTrue("Selection should have selected entry2", sel.entryId == entry2Id);
    }

    @Test
    public void bookmarkAction_setsDesiredBookmarkState() throws Exception {
        assertFalse(db.knowledgeEntryDao().findById(entry1Id).isBookmarked);

        Intent intent = new Intent(WidgetRenderer.ACTION_BOOKMARK);
        intent.putExtra(WidgetRenderer.EXTRA_ENTRY_ID, entry1Id);
        intent.putExtra(WidgetRenderer.EXTRA_DESIRED_BOOKMARK_STATE, true);

        WidgetActionReceiver receiver = new WidgetActionReceiver();
        receiver.onReceive(context, intent);
        executor.submit(() -> {}).get(2, TimeUnit.SECONDS);

        assertTrue(db.knowledgeEntryDao().findById(entry1Id).isBookmarked);

        // Desired state false removes bookmark
        Intent intent2 = new Intent(WidgetRenderer.ACTION_BOOKMARK);
        intent2.putExtra(WidgetRenderer.EXTRA_ENTRY_ID, entry1Id);
        intent2.putExtra(WidgetRenderer.EXTRA_DESIRED_BOOKMARK_STATE, false);
        receiver.onReceive(context, intent2);
        executor.submit(() -> {}).get(2, TimeUnit.SECONDS);

        assertFalse(db.knowledgeEntryDao().findById(entry1Id).isBookmarked);
    }

    @Test
    public void staleEntryBookmark_trashedEntry_isNoOp() throws Exception {
        // Trash entry1
        db.getOpenHelper().getWritableDatabase().execSQL(
                "UPDATE knowledge_entry SET is_trashed = 1 WHERE id = ?", new Object[]{entry1Id}
        );

        Intent intent = new Intent(WidgetRenderer.ACTION_BOOKMARK);
        intent.putExtra(WidgetRenderer.EXTRA_ENTRY_ID, entry1Id);
        intent.putExtra(WidgetRenderer.EXTRA_DESIRED_BOOKMARK_STATE, true);

        WidgetActionReceiver receiver = new WidgetActionReceiver();
        receiver.onReceive(context, intent);
        executor.submit(() -> {}).get(2, TimeUnit.SECONDS);

        // Trashed entry bookmark must remain unchanged (false)
        assertFalse(db.knowledgeEntryDao().findById(entry1Id).isBookmarked);
    }
}
