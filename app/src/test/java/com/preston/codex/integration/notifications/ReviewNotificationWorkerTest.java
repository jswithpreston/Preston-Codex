package com.preston.codex.integration.notifications;

import static org.junit.Assert.assertEquals;
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
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeSourceEntity;
import com.preston.codex.data.entity.ReviewScheduleEntity;

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
import java.time.ZoneId;
import java.util.concurrent.Executors;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ReviewNotificationWorkerTest {

    private Context context;
    private AppDatabase db;
    private AppContainer container;
    private Clock clock;
    private NotificationManager notificationManager;

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
        entry.quoteText = "A principle to recall.";
        entry.entryType = "QUOTE";
        entry.createdAt = 1000;
        entry.updatedAt = 1000;
        long entryId = db.knowledgeEntryDao().insert(entry);

        ReviewScheduleEntity sched = new ReviewScheduleEntity();
        sched.entryId = entryId;
        sched.step = 0;
        sched.nextReviewDate = "2026-10-26";
        sched.lastReviewedAt = 1000;
        sched.reviewCount = 0;
        db.reviewDao().upsertSchedule(sched);

        clock = Clock.fixed(Instant.parse("2026-10-26T18:05:00Z"), ZoneId.of("UTC"));
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
    public void defaultDisabled_skipsPosting() throws Exception {
        Data inputData = new Data.Builder()
                .putString(NotificationScheduler.KEY_SCHEDULED_DATE, "2026-10-26")
                .build();

        ReviewNotificationWorker worker = TestWorkerBuilder.from(context, ReviewNotificationWorker.class)
                .setInputData(inputData)
                .build();

        ListenableWorker.Result result = worker.doWork();
        assertEquals(ListenableWorker.Result.success(), result);

        StatusBarNotification[] active = notificationManager.getActiveNotifications();
        assertEquals("Disabled by default: must not post", 0, active.length);
    }

    @Test
    public void enabled_withQueueItems_postsNotificationWithCount() throws Exception {
        container.getNotificationPreferences().setReviewNotificationEnabled(true);

        Data inputData = new Data.Builder()
                .putString(NotificationScheduler.KEY_SCHEDULED_DATE, "2026-10-26")
                .build();

        ReviewNotificationWorker worker = TestWorkerBuilder.from(context, ReviewNotificationWorker.class)
                .setInputData(inputData)
                .build();

        ListenableWorker.Result result = worker.doWork();
        assertEquals(ListenableWorker.Result.success(), result);

        StatusBarNotification[] active = notificationManager.getActiveNotifications();
        assertEquals(1, active.length);

        StatusBarNotification sbn = active[0];
        assertEquals(NotificationChannelInitializer.NOTIFICATION_ID_REVIEW, sbn.getId());
        Notification notification = sbn.getNotification();

        assertNotNull(notification.publicVersion);
        assertEquals("Knowledge Reviews Due", notification.extras.getString(Notification.EXTRA_TITLE));
        assertTrue(notification.extras.getString(Notification.EXTRA_TEXT).contains("1 item ready"));
    }

    @Test
    public void enabled_withEmptyQueue_skipsPosting() throws Exception {
        container.getNotificationPreferences().setReviewNotificationEnabled(true);
        // Clear schedules
        db.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM review_schedule");
        db.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM knowledge_entry");

        Data inputData = new Data.Builder()
                .putString(NotificationScheduler.KEY_SCHEDULED_DATE, "2026-10-26")
                .build();

        ReviewNotificationWorker worker = TestWorkerBuilder.from(context, ReviewNotificationWorker.class)
                .setInputData(inputData)
                .build();

        ListenableWorker.Result result = worker.doWork();
        assertEquals(ListenableWorker.Result.success(), result);

        StatusBarNotification[] active = notificationManager.getActiveNotifications();
        assertEquals(0, active.length);
    }
}
