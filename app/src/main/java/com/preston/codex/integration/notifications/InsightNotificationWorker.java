package com.preston.codex.integration.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.preston.codex.AppContainer;
import com.preston.codex.CodexApplication;
import com.preston.codex.MainActivity;
import com.preston.codex.R;
import com.preston.codex.domain.scheduling.InsightDetails;
import com.preston.codex.integration.PermissionHelper;
import com.preston.codex.integration.widget.WidgetRenderer;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Self-rescheduling worker that constructs and posts the Daily Insight notification.
 * Implements idempotency via last_notified_date, lock-screen privacy, and late-worker checks.
 */
public class InsightNotificationWorker extends Worker {

    public InsightNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppContainer container = CodexApplication.getContainer(context);
        NotificationPreferences preferences = container.getNotificationPreferences();

        // Amendment #3: Ensure channels exist even on a fresh install without MainActivity having launched
        NotificationChannelInitializer.ensureChannelsCreated(context);

        try {
            if (!preferences.isInsightNotificationEnabled()) {
                return Result.success();
            }

            LocalDate today = LocalDate.now(container.getClock().withZone(container.getZoneSupplier().get()));

            // Idempotency: skip if already notified today
            if (preferences.wasNotifiedToday(NotificationChannelInitializer.CHANNEL_ID_INSIGHT, today)) {
                return Result.success();
            }

            // Late-worker rule: if scheduled date has passed, skip posting stale notification
            String scheduledDateStr = getInputData().getString(NotificationScheduler.KEY_SCHEDULED_DATE);
            if (scheduledDateStr != null && !scheduledDateStr.isEmpty()) {
                LocalDate scheduledDate = LocalDate.parse(scheduledDateStr);
                if (scheduledDate.isBefore(today)) {
                    return Result.success();
                }
            }

            // Check system-level notifications enabled and POST_NOTIFICATIONS permission
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()
                    || !PermissionHelper.hasPostNotificationsPermission(context)) {
                return Result.success();
            }

            // Select and load today's insight
            container.getDailyInsightEngine().getOrSelectDailyInsight();
            Optional<InsightDetails> detailsOpt = container.getInsightDetailsRepository().getDailyInsightDetails(today);
            if (!detailsOpt.isPresent()) {
                return Result.success();
            }

            InsightDetails details = detailsOpt.get();

            // Lock-screen public version (generic, no private quote text)
            Notification publicNotification = new NotificationCompat.Builder(context, NotificationChannelInitializer.CHANNEL_ID_INSIGHT)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notif_insight_title))
                    .setContentText(context.getString(R.string.notif_insight_public_text))
                    .build();

            // Content intent to open app
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(WidgetRenderer.EXTRA_ENTRY_ID, details.getEntryId());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    NotificationChannelInitializer.NOTIFICATION_ID_INSIGHT,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            String authorLine = formatAuthor(details.getAuthor(), details.getSourceTitle());
            Notification notification = new NotificationCompat.Builder(context, NotificationChannelInitializer.CHANNEL_ID_INSIGHT)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notif_insight_title))
                    .setContentText(details.getQuoteText())
                    .setStyle(new NotificationCompat.BigTextStyle()
                            .bigText("“" + details.getQuoteText() + "”" + (authorLine.isEmpty() ? "" : "\n" + authorLine)))
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(publicNotification)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();

            NotificationManagerCompat.from(context).notify(NotificationChannelInitializer.NOTIFICATION_ID_INSIGHT, notification);

            // Synchronously record last notified date
            preferences.setLastNotifiedDate(NotificationChannelInitializer.CHANNEL_ID_INSIGHT, today);

            return Result.success();
        } finally {
            // Self-reschedule next run with APPEND_OR_REPLACE
            container.getNotificationScheduler().scheduleInsight(ExistingWorkPolicy.APPEND_OR_REPLACE);
        }
    }

    private static String formatAuthor(String author, String sourceTitle) {
        boolean hasAuthor = author != null && !author.trim().isEmpty();
        boolean hasTitle = sourceTitle != null && !sourceTitle.trim().isEmpty();
        if (hasAuthor && hasTitle) return "— " + author.trim() + ", " + sourceTitle.trim();
        if (hasAuthor) return "— " + author.trim();
        if (hasTitle) return "— " + sourceTitle.trim();
        return "";
    }
}
