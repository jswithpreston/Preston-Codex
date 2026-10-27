package com.preston.codex.integration.widget;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.preston.codex.AppContainer;
import com.preston.codex.CodexApplication;

/**
 * Self-rescheduling one-time worker executed at midnight to refresh daily widgets
 * and rollover date-sensitive content.
 */
public class WidgetMidnightRefreshWorker extends Worker {

    public WidgetMidnightRefreshWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppContainer container = CodexApplication.getContainer(context);

        // Render widgets for the new local date
        container.getWidgetRenderer().renderAllWidgets(context);

        // Self-reschedule next midnight using APPEND_OR_REPLACE
        container.getNotificationScheduler().scheduleMidnight(ExistingWorkPolicy.APPEND_OR_REPLACE);

        return Result.success();
    }
}
