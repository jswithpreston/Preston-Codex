package com.preston.codex.integration.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.ListenableWorker;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.testing.TestWorkerBuilder;
import androidx.work.testing.WorkManagerTestInitHelper;

import com.preston.codex.AppContainer;
import com.preston.codex.CodexApplication;
import com.preston.codex.integration.notifications.NotificationScheduler;

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
import java.util.concurrent.Executors;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class WidgetMidnightRefreshWorkerTest {

    private Context context;
    private AppContainer container;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Configuration config = new Configuration.Builder().build();
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config);

        Clock clock = Clock.fixed(Instant.parse("2026-10-26T00:00:05Z"), ZoneId.of("UTC"));
        container = new AppContainer(context, clock, () -> ZoneId.of("UTC"), Executors.newSingleThreadExecutor());
        CodexApplication.setContainerForTest(container);
    }

    @After
    public void tearDown() {
        CodexApplication.resetContainerForTest();
    }

    @Test
    public void midnightWorker_executesAndSelfReschedules() throws Exception {
        WidgetMidnightRefreshWorker worker = TestWorkerBuilder.from(context, WidgetMidnightRefreshWorker.class)
                .build();

        ListenableWorker.Result result = worker.doWork();
        assertEquals(ListenableWorker.Result.success(), result);

        WorkManager wm = WorkManager.getInstance(context);
        List<WorkInfo> infos = wm.getWorkInfosForUniqueWork(NotificationScheduler.WORK_NAME_MIDNIGHT).get();
        boolean hasEnqueued = infos.stream().anyMatch(w -> w.getState() == WorkInfo.State.ENQUEUED);
        assertTrue("Worker must self-reschedule for next midnight", hasEnqueued);
    }
}
