package com.preston.codex.integration.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.preston.codex.integration.KeyValueStore;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

public class NotificationPreferencesTest {

    private FakeKeyValueStore store;
    private NotificationPreferences preferences;

    @Before
    public void setUp() {
        store = new FakeKeyValueStore();
        preferences = new NotificationPreferences(store);
    }

    @Test
    public void defaults_matchSpecification() {
        assertTrue(preferences.isInsightNotificationEnabled());
        assertFalse(preferences.isReviewNotificationEnabled()); // Amendment #7: default false
        assertEquals(LocalTime.of(8, 0), preferences.getInsightNotificationTime());
        assertEquals(LocalTime.of(18, 0), preferences.getReviewNotificationTime());
        assertFalse(preferences.hasAskedPostNotifications());
        assertEquals("", preferences.getLastNotifiedDate(NotificationChannelInitializer.CHANNEL_ID_INSIGHT));
        assertEquals("", preferences.getLastNotifiedDate(NotificationChannelInitializer.CHANNEL_ID_REVIEW));
    }

    @Test
    public void setNotificationEnabled_persistsCorrectly() {
        preferences.setInsightNotificationEnabled(false);
        assertFalse(preferences.isInsightNotificationEnabled());

        preferences.setReviewNotificationEnabled(true);
        assertTrue(preferences.isReviewNotificationEnabled());
    }

    @Test
    public void setNotificationTime_persistsAndClamps() {
        preferences.setInsightNotificationTime(LocalTime.of(9, 45));
        assertEquals(LocalTime.of(9, 45), preferences.getInsightNotificationTime());

        // Clamping invalid inputs via int setter
        preferences.setInsightTimeParts(-5, 75);
        assertEquals(LocalTime.of(0, 59), preferences.getInsightNotificationTime());

        preferences.setInsightTimeParts(25, -10);
        assertEquals(LocalTime.of(23, 0), preferences.getInsightNotificationTime());
    }

    @Test
    public void lastNotifiedDate_roundTripsSynchronously() {
        LocalDate date = LocalDate.of(2026, 10, 26);
        preferences.setLastNotifiedDate(NotificationChannelInitializer.CHANNEL_ID_INSIGHT, date);

        assertEquals("2026-10-26", preferences.getLastNotifiedDate(NotificationChannelInitializer.CHANNEL_ID_INSIGHT));
        assertTrue(preferences.wasNotifiedToday(NotificationChannelInitializer.CHANNEL_ID_INSIGHT, date));
        assertFalse(preferences.wasNotifiedToday(NotificationChannelInitializer.CHANNEL_ID_INSIGHT, date.plusDays(1)));
    }

    @Test
    public void postNotificationsAsked_persists() {
        assertFalse(preferences.hasAskedPostNotifications());
        preferences.setAskedPostNotifications(true);
        assertTrue(preferences.hasAskedPostNotifications());
    }

    private static class FakeKeyValueStore implements KeyValueStore {
        private final Map<String, Object> map = new HashMap<>();

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object v = map.get(key);
            return v instanceof Boolean ? (Boolean) v : defValue;
        }

        @Override
        public void putBoolean(String key, boolean value) {
            map.put(key, value);
        }

        @Override
        public int getInt(String key, int defValue) {
            Object v = map.get(key);
            return v instanceof Integer ? (Integer) v : defValue;
        }

        @Override
        public void putInt(String key, int value) {
            map.put(key, value);
        }

        @Override
        public String getString(String key, String defValue) {
            Object v = map.get(key);
            return v instanceof String ? (String) v : defValue;
        }

        @Override
        public void putString(String key, String value) {
            map.put(key, value);
        }
    }
}
