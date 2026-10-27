package com.preston.codex.integration;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;

import com.preston.codex.AppContainer;
import com.preston.codex.CodexApplication;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class DateTimeChangeReceiverTest {

    private Context context;
    private ExecutorService executor;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        executor = Executors.newSingleThreadExecutor();
        AppContainer container = new AppContainer(context) {
            @Override
            public ExecutorService getExecutorService() {
                return executor;
            }
        };
        CodexApplication.setContainerForTest(container);
    }

    @After
    public void tearDown() {
        CodexApplication.resetContainerForTest();
        executor.shutdown();
    }

    @Test
    public void timeSet_executesWithoutCrash() throws Exception {
        DateTimeChangeReceiver receiver = new DateTimeChangeReceiver();
        receiver.onReceive(context, new Intent(Intent.ACTION_TIME_CHANGED));
        executor.submit(() -> {}).get(2, TimeUnit.SECONDS);

        assertNotNull(CodexApplication.getContainer(context));
    }

    @Test
    public void timezoneChanged_executesWithoutCrash() throws Exception {
        DateTimeChangeReceiver receiver = new DateTimeChangeReceiver();
        receiver.onReceive(context, new Intent(Intent.ACTION_TIMEZONE_CHANGED));
        executor.submit(() -> {}).get(2, TimeUnit.SECONDS);

        assertNotNull(CodexApplication.getContainer(context));
    }
}
