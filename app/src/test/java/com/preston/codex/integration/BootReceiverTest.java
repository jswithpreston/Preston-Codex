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
public class BootReceiverTest {

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
    public void bootCompleted_executesWithoutCrash() throws Exception {
        BootReceiver receiver = new BootReceiver();
        receiver.onReceive(context, new Intent(Intent.ACTION_BOOT_COMPLETED));
        executor.submit(() -> {}).get(2, TimeUnit.SECONDS);

        assertNotNull(CodexApplication.getContainer(context));
    }

    @Test
    public void packageReplaced_executesWithoutCrash() throws Exception {
        BootReceiver receiver = new BootReceiver();
        receiver.onReceive(context, new Intent(Intent.ACTION_MY_PACKAGE_REPLACED));
        executor.submit(() -> {}).get(2, TimeUnit.SECONDS);

        assertNotNull(CodexApplication.getContainer(context));
    }
}
