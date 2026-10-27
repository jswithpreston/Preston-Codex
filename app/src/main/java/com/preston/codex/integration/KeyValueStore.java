package com.preston.codex.integration;

/**
 * Pure Java key-value storage abstraction.
 * Allows pure unit testing of preferences without mock Android SharedPreferences.
 */
public interface KeyValueStore {

    boolean getBoolean(String key, boolean defValue);

    void putBoolean(String key, boolean value);

    int getInt(String key, int defValue);

    void putInt(String key, int value);

    String getString(String key, String defValue);

    void putString(String key, String value);
}
