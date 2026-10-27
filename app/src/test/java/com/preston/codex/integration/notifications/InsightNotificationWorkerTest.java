package com.preston.codex.integration.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.service.notification.StatusBarNotification;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestWorkerBuilder;
import androidx.work.testing.WorkManagerTestInitHelper;

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
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowInstrumentation;

import java.lang.reflect.Method;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.Executors;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class InsightNotificationWorkerTest {

    private Context context;
    private AppDatabase db;
    private AppContainer container;
    private Clock clock;
    private NotificationManager notificationManager;
    private long entryId;

    @Before
    public void setUp() throws Exception {
        context = ApplicationProvider.getApplicationContext();
        Configuration config = new Configuration.Builder().build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);

        notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Notifications are enabled by default in Robolectric; grant the runtime permission so
            // ContextCompat.checkSelfPermission (used by PermissionHelper) returns GRANTED.
            Shadows.shadowOf(notificationManager).setNotificationsEnabled(true);
            ShadowInstrumentation shadowInstr = Shadows.shadowOf(ShadowInstrumentation.getInstrumentation());
            try {
                java.lang.reflect.Method grantPerm = ShadowInstrumentation.class.getDeclaredMethod(
                        "grantPermissions", String[].class);
                grantPerm.setAccessible(true);
                grantPerm.invoke(shadowInstr, (Object) new String[]{
                        android.Manifest.permission.POST_NOTIFICATIONS});
            } catch (Exception e) {
                throw new RuntimeException("Failed to grant notification permission", e);
            }
        }

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .addCallback(AppDatabase.CALLBACK)
                .build();

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
        entry.quoteText = "You do not rise to the level of your goals.";
        entry.insightText = "Focus on systems.";
        entry.entryType = "QUOTE";
        entry.createdAt = 1000;
        entry.updatedAt = 1000;
        entryId = db.knowledgeEntryDao().insert(entry);

        DailyInsightSelectionEntity sel = new DailyInsightSelectionEntity();
        sel.localDate = "2026-10-26";
        sel.entryId = entryId;
        sel.selectedAt = 1000;
        sel.wasShuffled = false;
        db.dailyInsightDao().insertSelection(sel);

        clock = Clock.fixed(Instant.parse("2026-10-26T08:05:00Z"), ZoneId.of("UTC"));
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
    public void freshInstall_withoutMainActivity_postsSuccessfully() throws Exception {
        Data inputData = new Data.Builder()
                .putString(NotificationScheduler.KEY_SCHEDULED_DATE, "2026-10-26")
                .build();

        InsightNotificationWorker worker = TestWorkerBuilder.from(context, InsightNotificationWorker.class)
                .setInputData(inputData)
                .build();

        ListenableWorker.Result result = worker.doWork();
        assertEquals(ListenableWorker.Result.success(), result);

        StatusBarNotification[] active = notificationManager.getActiveNotifications();
        assertTrue("Must post notification on fresh install", active.length > 0);

        StatusBarNotification sbn = active[0];
        assertEquals(NotificationChannelInitializer.NOTIFICATION_ID_INSIGHT, sbn.getId());
        Notification notification = sbn.getNotification();

        // Check lockscreen public version privacy
        assertNotNull(notification.publicVersion);
        assertEquals("Insight of the Day", notification.publicVersion.extras.getString(Notification.EXTRA_TITLE));
        assertEquals("Open Preston Codex to view today's insight", notification.publicVersion.extras.getString(Notification.EXTRA_TEXT));

        // Private version has quote text
        assertEquals("You do not rise to the level of your goals.", notification.extras.getString(Notification.EXTRA_TEXT));
    }

    @Test
    public void worker_runTwiceOnSameDate_postsOnlyOneNotification() throws Exception {
        Data inputData = new Data.Builder()
                .putString(NotificationScheduler.KEY_SCHEDULED_DATE, "2026-10-26")
                .build();

        // Run 1
        InsightNotificationWorker worker1 = TestWorkerBuilder.from(context, InsightNotificationWorker.class)
                .setInputData(inputData)
                .build();
        worker1.doWork();

        assertEquals("2026-10-26", container.getNotificationPreferences().getLastNotifiedDate(NotificationChannelInitializer.CHANNEL_ID_INSIGHT));
        notificationManager.cancelAll();

        // Run 2 on same date
        InsightNotificationWorker worker2 = TestWorkerBuilder.from(context, InsightNotificationWorker.class)
                .setInputData(inputData)
                .build();
        worker2.doWork();

        StatusBarNotification[] active = notificationManager.getActiveNotifications();
        assertEquals("Second run on same date must skip posting", 0, active.length);
    }

    @Test
    public void lateWorker_scheduledDateAlreadyPassed_skipsStalePosting() throws Exception {
        // Scheduled for yesterday (2026-10-25), but running today (2026-10-26)
        Data inputData = new Data.Builder()
                .putString(NotificationScheduler.KEY_SCHEDULED_DATE, "2026-10-25")
                .build();

        InsightNotificationWorker worker = TestWorkerBuilder.from(context, InsightNotificationWorker.class)
                .setInputData(inputData)
                .build();

        ListenableWorker.Result result = worker.doWork();
        assertEquals(ListenableWorker.Result.success(), result);

        StatusBarNotification[] active = notificationManager.getActiveNotifications();
        assertEquals("Late worker with past scheduled date must skip posting", 0, active.length);
    }

    @Test
    public void disabledChannel_skipsPosting() throws Exception {
        container.getNotificationPreferences().setInsightNotificationEnabled(false);

        Data inputData = new Data.Builder()
                .putString(NotificationScheduler.KEY_SCHEDULED_DATE, "2026-10-26")
                .build();

        InsightNotificationWorker worker = TestWorkerBuilder.from(context, InsightNotificationWorker.class)
                .setInputData(inputData)
                .build();

        ListenableWorker.Result result = worker.doWork();
        assertEquals(ListenableWorker.Result.success(), result);

        StatusBarNotification[] active = notificationManager.getActiveNotifications();
        assertEquals(0, active.length);
    }
}
