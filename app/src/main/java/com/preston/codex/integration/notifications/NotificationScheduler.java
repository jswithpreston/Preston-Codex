package com.preston.codex.integration.notifications;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.preston.codex.domain.scheduling.NextRunCalculator;
import com.preston.codex.integration.widget.WidgetMidnightRefreshWorker;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Idempotently schedules self-rescheduling one-time WorkManager workers
 * for daily insights, review reminders, and widget midnight refreshes.
 */
public final class NotificationScheduler {

    public static final String WORK_NAME_MIDNIGHT = "codex_widget_midnight_refresh";
    public static final String WORK_NAME_INSIGHT = "codex_insight_notification";
    public static final String WORK_NAME_REVIEW = "codex_review_notification";

    public static final String KEY_SCHEDULED_DATE = "scheduled_local_date";

    private final Context appContext;
    private final NotificationPreferences preferences;
    private final Clock clock;
    private final Supplier<ZoneId> zoneSupplier;

    public NotificationScheduler(
            Context context,
            NotificationPreferences preferences,
            Clock clock,
            Supplier<ZoneId> zoneSupplier
    ) {
        this.appContext = Objects.requireNonNull(context, "context must not be null").getApplicationContext();
        this.preferences = Objects.requireNonNull(preferences, "preferences must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.zoneSupplier = Objects.requireNonNull(zoneSupplier, "zoneSupplier must not be null");
    }

    /**
     * Idempotently ensures all background workers are scheduled.
     * Called from BaseInsightWidgetProvider onEnabled/onUpdate, MainActivity.onResume, and BootReceiver.
     */
    public void ensureScheduled() {
        scheduleMidnight(ExistingWorkPolicy.REPLACE);
        if (preferences.isInsightNotificationEnabled()) {
            scheduleInsight(ExistingWorkPolicy.REPLACE);
        } else {
            WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME_INSIGHT);
        }

        if (preferences.isReviewNotificationEnabled()) {
            scheduleReview(ExistingWorkPolicy.REPLACE);
        } else {
            WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME_REVIEW);
        }
    }

    public void scheduleMidnight(ExistingWorkPolicy policy) {
        ZoneId zone = zoneSupplier.get();
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(zone));
        Duration delay = NextRunCalculator.calculateDelayUntilMidnight(now, zone);
        ZonedDateTime nextMidnight = NextRunCalculator.calculateNextMidnight(now, zone);

        Data inputData = new Data.Builder()
                .putString(KEY_SCHEDULED_DATE, nextMidnight.toLocalDate().toString())
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(WidgetMidnightRefreshWorker.class)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(appContext).enqueueUniqueWork(WORK_NAME_MIDNIGHT, policy, request);
    }

    public void scheduleInsight(ExistingWorkPolicy policy) {
        ZoneId zone = zoneSupplier.get();
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(zone));
        LocalTime targetTime = preferences.getInsightNotificationTime();
        Duration delay = NextRunCalculator.calculateDelay(now, targetTime, zone);
        ZonedDateTime nextRun = NextRunCalculator.calculateNextRun(now, targetTime, zone);

        Data inputData = new Data.Builder()
                .putString(KEY_SCHEDULED_DATE, nextRun.toLocalDate().toString())
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(InsightNotificationWorker.class)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(appContext).enqueueUniqueWork(WORK_NAME_INSIGHT, policy, request);
    }

    public void scheduleReview(ExistingWorkPolicy policy) {
        ZoneId zone = zoneSupplier.get();
        ZonedDateTime now = ZonedDateTime.now(clock.withZone(zone));
        LocalTime targetTime = preferences.getReviewNotificationTime();
        Duration delay = NextRunCalculator.calculateDelay(now, targetTime, zone);
        ZonedDateTime nextRun = NextRunCalculator.calculateNextRun(now, targetTime, zone);

        Data inputData = new Data.Builder()
                .putString(KEY_SCHEDULED_DATE, nextRun.toLocalDate().toString())
                .build();

        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(ReviewNotificationWorker.class)
                .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(appContext).enqueueUniqueWork(WORK_NAME_REVIEW, policy, request);
    }

    public void cancelAll() {
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME_MIDNIGHT);
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME_INSIGHT);
        WorkManager.getInstance(appContext).cancelUniqueWork(WORK_NAME_REVIEW);
    }
}
