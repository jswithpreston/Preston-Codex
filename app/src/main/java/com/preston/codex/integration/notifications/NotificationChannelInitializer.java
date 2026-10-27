package com.preston.codex.integration.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.preston.codex.R;

import java.util.Objects;

/**
 * Idempotently initialises the notification channels required by the app.
 * Can be called safely from Application.onCreate() and Worker.doWork().
 */
public final class NotificationChannelInitializer {

    public static final String CHANNEL_ID_INSIGHT = "codex_insight";
    public static final String CHANNEL_ID_REVIEW = "codex_review";

    public static final int NOTIFICATION_ID_INSIGHT = 1001;
    public static final int NOTIFICATION_ID_REVIEW = 1002;

    private NotificationChannelInitializer() {
    }

    public static void ensureChannelsCreated(Context context) {
        Objects.requireNonNull(context, "context must not be null");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }

        // Daily insight channel
        NotificationChannel insightChannel = new NotificationChannel(
                CHANNEL_ID_INSIGHT,
                context.getString(R.string.notif_channel_insight_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        insightChannel.setDescription(context.getString(R.string.notif_channel_insight_desc));
        manager.createNotificationChannel(insightChannel);

        // Review reminders channel
        NotificationChannel reviewChannel = new NotificationChannel(
                CHANNEL_ID_REVIEW,
                context.getString(R.string.notif_channel_review_name),
                NotificationManager.IMPORTANCE_DEFAULT
        );
        reviewChannel.setDescription(context.getString(R.string.notif_channel_review_desc));
        manager.createNotificationChannel(reviewChannel);
    }
}
