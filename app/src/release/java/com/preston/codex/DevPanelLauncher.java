package com.preston.codex;

import android.app.Activity;

/**
 * Release variant: no-op stub. Dev panel never attaches in release.
 */
public final class DevPanelLauncher {

    private DevPanelLauncher() {
    }

    public static void attach(Activity activity) {
        // No-op in release builds
    }
}
