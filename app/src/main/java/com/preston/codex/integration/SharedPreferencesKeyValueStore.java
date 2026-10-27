package com.preston.codex.integration;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Objects;

/**
 * Android {@link SharedPreferences} adapter implementing {@link KeyValueStore}.
 * Uses synchronous commit() for immediate thread-safe durability.
 */
public final class SharedPreferencesKeyValueStore implements KeyValueStore {

    private final SharedPreferences preferences;

    public SharedPreferencesKeyValueStore(Context context, String prefsName) {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(prefsName, "prefsName must not be null");
        this.preferences = context.getApplicationContext().getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }

    public SharedPreferencesKeyValueStore(SharedPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences, "preferences must not be null");
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return preferences.getBoolean(key, defValue);
    }

    @Override
    public void putBoolean(String key, boolean value) {
        preferences.edit().putBoolean(key, value).commit();
    }

    @Override
    public int getInt(String key, int defValue) {
        return preferences.getInt(key, defValue);
    }

    @Override
    public void putInt(String key, int value) {
        preferences.edit().putInt(key, value).commit();
    }

    @Override
    public String getString(String key, String defValue) {
        return preferences.getString(key, defValue);
    }

    @Override
    public void putString(String key, String value) {
        preferences.edit().putString(key, value).commit();
    }
}
