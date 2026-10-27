package com.preston.codex.integration.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import com.preston.codex.AppContainer;
import com.preston.codex.CodexApplication;

/**
 * Base AppWidgetProvider providing shared lifecycle behavior and goAsync() executor rendering.
 */
public abstract class BaseInsightWidgetProvider extends AppWidgetProvider {

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        AppContainer container = CodexApplication.getContainer(context);
        container.getNotificationScheduler().ensureScheduled();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }

        AppContainer container = CodexApplication.getContainer(context);
        container.getNotificationScheduler().ensureScheduled();

        final PendingResult pendingResult = goAsync();
        container.getExecutorService().execute(() -> {
            try {
                container.getWidgetRenderer().renderWidgets(context, appWidgetManager, appWidgetIds);
            } finally {
                pendingResult.finish();
            }
        });
    }
}
