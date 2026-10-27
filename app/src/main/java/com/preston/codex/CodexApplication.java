package com.preston.codex;

import android.app.Application;
import android.content.Context;

import com.preston.codex.integration.notifications.NotificationChannelInitializer;

import java.util.Objects;

/**
 * Application subclass for Preston Codex.
 * Manages the singleton {@link AppContainer} and initializes notification channels.
 */
public class CodexApplication extends Application {

    private static volatile AppContainer sContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        // Amendment #3: Create notification channels at application startup
        NotificationChannelInitializer.ensureChannelsCreated(this);
    }

    public static AppContainer getContainer(Context context) {
        Objects.requireNonNull(context, "context must not be null");
        if (sContainer == null) {
            synchronized (CodexApplication.class) {
                if (sContainer == null) {
                    sContainer = new AppContainer(context.getApplicationContext());
                }
            }
        }
        return sContainer;
    }

    /**
     * Replaces the composition container for testing.
     */
    public static void setContainerForTest(AppContainer testContainer) {
        synchronized (CodexApplication.class) {
            sContainer = testContainer;
        }
    }

    /**
     * Resets the container after test completion.
     */
    public static void resetContainerForTest() {
        synchronized (CodexApplication.class) {
            sContainer = null;
        }
    }
}
