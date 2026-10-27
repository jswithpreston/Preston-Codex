package com.preston.codex.integration;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.content.ContextCompat;

import java.util.Objects;

/**
 * Utility for querying POST_NOTIFICATIONS permission state.
 */
public final class PermissionHelper {

    private PermissionHelper() {
    }

    public static boolean hasPostNotificationsPermission(Context context) {
        Objects.requireNonNull(context, "context must not be null");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED;
    }
}
