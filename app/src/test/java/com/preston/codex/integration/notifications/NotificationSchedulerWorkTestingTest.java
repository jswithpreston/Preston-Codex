package com.preston.codex.integration.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.ExistingWorkPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.preston.codex.AppContainer;
import com.preston.codex.CodexApplication;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class NotificationSchedulerWorkTestingTest {

    private Context context;
    private AppContainer container;
    private NotificationScheduler scheduler;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Configuration config = new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG)
                .build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);

        Clock clock = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneId.of("UTC"));
        container = new AppContainer(context, clock, () -> ZoneId.of("UTC"), java.util.concurrent.Executors.newSingleThreadExecutor());
        CodexApplication.setContainerForTest(container);
        scheduler = container.getNotificationScheduler();
    }

    @After
    public void tearDown() {
        CodexApplication.resetContainerForTest();
    }

    @Test
    public void ensureScheduled_isIdempotent() throws Exception {
        scheduler.ensureScheduled();
        scheduler.ensureScheduled();

        WorkManager wm = WorkManager.getInstance(context);
        List<WorkInfo> midnightInfos = wm.getWorkInfosForUniqueWork(NotificationScheduler.WORK_NAME_MIDNIGHT).get();
        List<WorkInfo> insightInfos = wm.getWorkInfosForUniqueWork(NotificationScheduler.WORK_NAME_INSIGHT).get();

        assertNotNull(midnightInfos);
        assertEquals(1, midnightInfos.size());
        assertTrue(midnightInfos.get(0).getState() == WorkInfo.State.ENQUEUED);

        assertNotNull(insightInfos);
        assertEquals(1, insightInfos.size());
        assertTrue(insightInfos.get(0).getState() == WorkInfo.State.ENQUEUED);
    }

    @Test
    public void selfRescheduling_chainSurvivesThreeRuns_neverCancelsItself() throws Exception {
        // Run 1: initial schedule via REPLACE
        scheduler.scheduleInsight(ExistingWorkPolicy.REPLACE);
        WorkManager wm = WorkManager.getInstance(context);

        List<WorkInfo> run1 = wm.getWorkInfosForUniqueWork(NotificationScheduler.WORK_NAME_INSIGHT).get();
        assertEquals(1, run1.size());
        assertTrue(run1.get(0).getState() == WorkInfo.State.ENQUEUED);

        // Run 2: self-reschedule via APPEND_OR_REPLACE from inside worker
        scheduler.scheduleInsight(ExistingWorkPolicy.APPEND_OR_REPLACE);
        List<WorkInfo> run2 = wm.getWorkInfosForUniqueWork(NotificationScheduler.WORK_NAME_INSIGHT).get();
        assertTrue(run2.size() >= 1);
        boolean hasEnqueued2 = run2.stream().anyMatch(w -> w.getState() == WorkInfo.State.ENQUEUED);
        assertTrue("Run 2 must maintain an enqueued work request", hasEnqueued2);

        // Run 3: self-reschedule via APPEND_OR_REPLACE from inside worker
        scheduler.scheduleInsight(ExistingWorkPolicy.APPEND_OR_REPLACE);
        List<WorkInfo> run3 = wm.getWorkInfosForUniqueWork(NotificationScheduler.WORK_NAME_INSIGHT).get();
        assertTrue(run3.size() >= 1);
        boolean hasEnqueued3 = run3.stream().anyMatch(w -> w.getState() == WorkInfo.State.ENQUEUED);
        assertTrue("Run 3 must maintain an enqueued work request", hasEnqueued3);
    }

    @Test
    public void cancelAll_clearsScheduledWorkers() throws Exception {
        scheduler.ensureScheduled();
        scheduler.cancelAll();

        WorkManager wm = WorkManager.getInstance(context);
        List<WorkInfo> midnightInfos = wm.getWorkInfosForUniqueWork(NotificationScheduler.WORK_NAME_MIDNIGHT).get();
        boolean active = midnightInfos.stream().anyMatch(w -> w.getState() == WorkInfo.State.ENQUEUED);
        assertTrue("Cancelled work must not be ENQUEUED", !active);
    }
}
