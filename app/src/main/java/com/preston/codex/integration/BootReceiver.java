package com.preston.codex.integration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.preston.codex.AppContainer;
import com.preston.codex.CodexApplication;

/**
 * Manifest receiver listening to system BOOT_COMPLETED and MY_PACKAGE_REPLACED broadcasts.
 * Restores WorkManager background workers and refreshes widgets.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        final PendingResult pendingResult = goAsync();
        final AppContainer container = CodexApplication.getContainer(context);

        container.getExecutorService().execute(() -> {
            try {
                container.getNotificationScheduler().ensureScheduled();
                container.getWidgetRenderer().renderAllWidgets(context);
            } finally {
                pendingResult.finish();
            }
        });
    }
}
