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
import com.preston.codex.domain.recall.ReviewQueueItem;
import com.preston.codex.integration.PermissionHelper;

import java.time.LocalDate;
import java.util.List;

/**
 * Self-rescheduling worker for daily recall/spaced repetition review reminders.
 * Posts notification only when items are due in the review queue and channel is enabled.
 */
public class ReviewNotificationWorker extends Worker {

    public ReviewNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AppContainer container = CodexApplication.getContainer(context);
        NotificationPreferences preferences = container.getNotificationPreferences();

        NotificationChannelInitializer.ensureChannelsCreated(context);

        try {
            if (!preferences.isReviewNotificationEnabled()) {
                return Result.success();
            }

            LocalDate today = LocalDate.now(container.getClock().withZone(container.getZoneSupplier().get()));

            if (preferences.wasNotifiedToday(NotificationChannelInitializer.CHANNEL_ID_REVIEW, today)) {
                return Result.success();
            }

            String scheduledDateStr = getInputData().getString(NotificationScheduler.KEY_SCHEDULED_DATE);
            if (scheduledDateStr != null && !scheduledDateStr.isEmpty()) {
                LocalDate scheduledDate = LocalDate.parse(scheduledDateStr);
                if (scheduledDate.isBefore(today)) {
                    return Result.success();
                }
            }

            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()
                    || !PermissionHelper.hasPostNotificationsPermission(context)) {
                return Result.success();
            }

            List<ReviewQueueItem> queue = container.getReviewScheduler().getReviewQueue();
            if (queue == null || queue.isEmpty()) {
                return Result.success();
            }

            int count = queue.size();
            String text = count == 1
                    ? context.getString(R.string.notif_review_text_single)
                    : context.getString(R.string.notif_review_text_multiple, count);

            Notification publicNotification = new NotificationCompat.Builder(context, NotificationChannelInitializer.CHANNEL_ID_REVIEW)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notif_review_title))
                    .setContentText(context.getString(R.string.notif_review_public_text))
                    .build();

            Intent intent = new Intent(context, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    NotificationChannelInitializer.NOTIFICATION_ID_REVIEW,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Notification notification = new NotificationCompat.Builder(context, NotificationChannelInitializer.CHANNEL_ID_REVIEW)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(context.getString(R.string.notif_review_title))
                    .setContentText(text)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(publicNotification)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build();

            NotificationManagerCompat.from(context).notify(NotificationChannelInitializer.NOTIFICATION_ID_REVIEW, notification);

            preferences.setLastNotifiedDate(NotificationChannelInitializer.CHANNEL_ID_REVIEW, today);

            return Result.success();
        } finally {
            container.getNotificationScheduler().scheduleReview(ExistingWorkPolicy.APPEND_OR_REPLACE);
        }
    }
}
