package com.preston.codex;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CodexApplicationTest {

    @After
    public void tearDown() {
        CodexApplication.resetContainerForTest();
    }

    @Test
    public void mergedManifest_loadsCodexApplication() {
        Application app = ApplicationProvider.getApplicationContext();
        assertNotNull(app);
        assertTrue("Application should be an instance of CodexApplication", app instanceof CodexApplication);
    }

    @Test
    public void appContainer_canBeReplacedInTests() {
        Context context = ApplicationProvider.getApplicationContext();
        AppContainer defaultContainer = CodexApplication.getContainer(context);
        assertNotNull(defaultContainer);

        AppContainer customContainer = new AppContainer(context);
        CodexApplication.setContainerForTest(customContainer);
        assertSame(customContainer, CodexApplication.getContainer(context));

        CodexApplication.resetContainerForTest();
    }
}
