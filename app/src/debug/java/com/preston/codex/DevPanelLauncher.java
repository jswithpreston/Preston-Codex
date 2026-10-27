package com.preston.codex;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.preston.codex.debug.DebugDevPanelActivity;

/**
 * Debug variant: reveals the dev panel launch button and wires click handler.
 */
public final class DevPanelLauncher {

    private DevPanelLauncher() {
    }

    public static void attach(Activity activity) {
        View button = activity.findViewById(R.id.btn_open_dev_panel);
        if (button != null) {
            button.setVisibility(View.VISIBLE);
            button.setOnClickListener(v -> {
                Intent intent = new Intent(activity, DebugDevPanelActivity.class);
                activity.startActivity(intent);
            });
        }
    }
}
