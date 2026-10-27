package com.preston.codex.integration.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.preston.codex.AppContainer;
import com.preston.codex.CodexApplication;

/**
 * BroadcastReceiver for widget user interactions (Shuffle and Bookmark).
 * Uses goAsync() and executes directly on AppContainer's single-thread executor.
 */
public class WidgetActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) {
            return;
        }

        final PendingResult pendingResult = goAsync();
        final AppContainer container = CodexApplication.getContainer(context);

        container.getExecutorService().execute(() -> {
            try {
                String action = intent.getAction();
                long entryId = intent.getLongExtra(WidgetRenderer.EXTRA_ENTRY_ID, -1L);

                if (WidgetRenderer.ACTION_SHUFFLE.equals(action)) {
                    container.getDailyInsightEngine().shuffle();
                } else if (WidgetRenderer.ACTION_BOOKMARK.equals(action)) {
                    boolean desired = intent.getBooleanExtra(WidgetRenderer.EXTRA_DESIRED_BOOKMARK_STATE, false);
                    if (entryId > 0 && container.getDailyInsightRepository().isEntryEligible(entryId)) {
                        container.getBookmarkService().setBookmark(entryId, desired);
                    }
                }

                // Render directly without WorkManager latency
                container.getWidgetRenderer().renderAllWidgets(context);
            } finally {
                pendingResult.finish();
            }
        });
    }
}
