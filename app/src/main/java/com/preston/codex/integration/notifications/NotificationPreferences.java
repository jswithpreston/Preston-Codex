package com.preston.codex.integration.notifications;

import com.preston.codex.integration.KeyValueStore;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Pure Java preferences manager for notifications and widget state.
 * Uses {@link KeyValueStore} for persistence, allowing unit testing without Android mocks.
 */
public final class NotificationPreferences {

    public static final String KEY_INSIGHT_ENABLED = "notif_insight_enabled";
    public static final String KEY_INSIGHT_HOUR = "notif_insight_hour";
    public static final String KEY_INSIGHT_MINUTE = "notif_insight_minute";

    public static final String KEY_REVIEW_ENABLED = "notif_review_enabled";
    public static final String KEY_REVIEW_HOUR = "notif_review_hour";
    public static final String KEY_REVIEW_MINUTE = "notif_review_minute";

    public static final String KEY_PREFIX_LAST_DATE = "notif_last_date_";
    public static final String KEY_POST_NOTIFICATIONS_ASKED = "notif_post_permissions_asked";

    public static final int DEFAULT_INSIGHT_HOUR = 8;
    public static final int DEFAULT_INSIGHT_MINUTE = 0;

    public static final int DEFAULT_REVIEW_HOUR = 18;
    public static final int DEFAULT_REVIEW_MINUTE = 0;

    private final KeyValueStore store;

    public NotificationPreferences(KeyValueStore store) {
        this.store = Objects.requireNonNull(store, "store must not be null");
    }

    public boolean isInsightNotificationEnabled() {
        return store.getBoolean(KEY_INSIGHT_ENABLED, true);
    }

    public void setInsightNotificationEnabled(boolean enabled) {
        store.putBoolean(KEY_INSIGHT_ENABLED, enabled);
    }

    public LocalTime getInsightNotificationTime() {
        int hour = clampHour(store.getInt(KEY_INSIGHT_HOUR, DEFAULT_INSIGHT_HOUR));
        int minute = clampMinute(store.getInt(KEY_INSIGHT_MINUTE, DEFAULT_INSIGHT_MINUTE));
        return LocalTime.of(hour, minute);
    }

    public void setInsightNotificationTime(LocalTime time) {
        Objects.requireNonNull(time, "time must not be null");
        store.putInt(KEY_INSIGHT_HOUR, time.getHour());
        store.putInt(KEY_INSIGHT_MINUTE, time.getMinute());
    }

    public void setInsightTimeParts(int hour, int minute) {
        store.putInt(KEY_INSIGHT_HOUR, clampHour(hour));
        store.putInt(KEY_INSIGHT_MINUTE, clampMinute(minute));
    }

    public boolean isReviewNotificationEnabled() {
        // Amendment #7: default is false until Phase 6 review UI is built
        return store.getBoolean(KEY_REVIEW_ENABLED, false);
    }

    public void setReviewNotificationEnabled(boolean enabled) {
        store.putBoolean(KEY_REVIEW_ENABLED, enabled);
    }

    public LocalTime getReviewNotificationTime() {
        int hour = clampHour(store.getInt(KEY_REVIEW_HOUR, DEFAULT_REVIEW_HOUR));
        int minute = clampMinute(store.getInt(KEY_REVIEW_MINUTE, DEFAULT_REVIEW_MINUTE));
        return LocalTime.of(hour, minute);
    }

    public void setReviewNotificationTime(LocalTime time) {
        Objects.requireNonNull(time, "time must not be null");
        store.putInt(KEY_REVIEW_HOUR, time.getHour());
        store.putInt(KEY_REVIEW_MINUTE, time.getMinute());
    }

    public void setReviewTimeParts(int hour, int minute) {
        store.putInt(KEY_REVIEW_HOUR, clampHour(hour));
        store.putInt(KEY_REVIEW_MINUTE, clampMinute(minute));
    }

    public String getLastNotifiedDate(String channelId) {
        return store.getString(KEY_PREFIX_LAST_DATE + channelId, "");
    }

    public void setLastNotifiedDate(String channelId, LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        store.putString(KEY_PREFIX_LAST_DATE + channelId, date.toString());
    }

    public boolean wasNotifiedToday(String channelId, LocalDate today) {
        Objects.requireNonNull(today, "today must not be null");
        String last = getLastNotifiedDate(channelId);
        return today.toString().equals(last);
    }

    public boolean hasAskedPostNotifications() {
        return store.getBoolean(KEY_POST_NOTIFICATIONS_ASKED, false);
    }

    public void setAskedPostNotifications(boolean asked) {
        store.putBoolean(KEY_POST_NOTIFICATIONS_ASKED, asked);
    }

    private static int clampHour(int hour) {
        return Math.max(0, Math.min(23, hour));
    }

    private static int clampMinute(int minute) {
        return Math.max(0, Math.min(59, minute));
    }
}
