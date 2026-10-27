package com.preston.codex.debug;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.widget.Button;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.preston.codex.AppContainer;
import com.preston.codex.CodexApplication;
import com.preston.codex.R;
import com.preston.codex.data.database.AppDatabase;
import com.preston.codex.data.entity.DailyInsightSelectionEntity;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeSourceEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DebugDevPanelTest {

    private Context context;
    private AppDatabase db;
    private AppContainer container;
    private ExecutorService executor;
    private ActivityController<DebugDevPanelActivity> controller;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .addCallback(AppDatabase.CALLBACK)
                .build();

        executor = Executors.newSingleThreadExecutor();
        Clock clock = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneId.of("UTC"));
        container = new AppContainer(context, clock, () -> ZoneId.of("UTC"), executor) {
            @Override
            public synchronized AppDatabase getDatabase() {
                return db;
            }
        };
        CodexApplication.setContainerForTest(container);

        controller = Robolectric.buildActivity(DebugDevPanelActivity.class).setup();
    }

    @After
    public void tearDown() {
        if (controller != null) {
            controller.pause().stop().destroy();
        }
        CodexApplication.resetContainerForTest();
        executor.shutdown();
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void devPanel_activityLaunches() {
        DebugDevPanelActivity activity = controller.get();
        assertNotNull(activity);
        assertNotNull(activity.findViewById(R.id.btn_import_sample));
    }

    @Test
    public void devPanel_importSampleLibrary_insertsEntries() throws Exception {
        DebugDevPanelActivity activity = controller.get();
        Button importBtn = activity.findViewById(R.id.btn_import_sample);
        importBtn.performClick();

        executor.submit(() -> {}).get(5, TimeUnit.SECONDS);

        int count = db.knowledgeEntryDao().count();
        assertTrue("Import must insert sample entries into database (count=" + count + ")", count >= 2);
    }

    @Test
    public void devPanel_archiveAndTrashCurrentEntry() throws Exception {
        KnowledgeSourceEntity src = new KnowledgeSourceEntity();
        src.title = "Source";
        src.author = "Author";
        src.titleKey = "source";
        src.authorKey = "author";
        src.sourceType = "BOOK";
        src.createdAt = 1000;
        src.updatedAt = 1000;
        long srcId = db.knowledgeSourceDao().insert(src);

        KnowledgeEntryEntity entry = new KnowledgeEntryEntity();
        entry.sourceId = srcId;
        entry.fingerprint = "fp1";
        entry.contentHash = "ch1";
        entry.importedHash = "ih1";
        entry.quoteText = "Quote";
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

        DebugDevPanelActivity activity = controller.get();

        // Archive
        activity.findViewById(R.id.btn_archive_current).performClick();
        executor.submit(() -> {}).get(2, TimeUnit.SECONDS);
        assertTrue(db.knowledgeEntryDao().findById(entryId).isArchived);

        // Reset and Trash
        db.getOpenHelper().getWritableDatabase().execSQL("UPDATE knowledge_entry SET is_archived = 0 WHERE id = ?", new Object[]{entryId});
        activity.findViewById(R.id.btn_trash_current).performClick();
        executor.submit(() -> {}).get(2, TimeUnit.SECONDS);
        assertTrue(db.knowledgeEntryDao().findById(entryId).isTrashed);
    }
}
