package com.preston.codex;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.preston.codex.integration.notifications.NotificationPreferences;
import com.preston.codex.integration.widget.WidgetRenderer;

/**
 * Launcher activity — Phase 3 thin placeholder.
 * Contains no business logic; displays entry ID and manages permission request once.
 */
public class MainActivity extends AppCompatActivity {

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Permission outcome noted; background scheduling continues regardless
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handleIntentExtra(getIntent());
        DevPanelLauncher.attach(this);

        requestNotificationPermissionIfNeeded();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentExtra(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppContainer container = CodexApplication.getContainer(this);
        container.getNotificationScheduler().ensureScheduled();
        container.getWidgetRenderer().renderAllWidgets(this);
    }

    private void handleIntentExtra(Intent intent) {
        TextView entryText = findViewById(R.id.text_entry_id);
        if (entryText == null) {
            return;
        }

        if (intent != null && intent.hasExtra(WidgetRenderer.EXTRA_ENTRY_ID)) {
            long entryId = intent.getLongExtra(WidgetRenderer.EXTRA_ENTRY_ID, -1L);
            if (entryId > 0) {
                entryText.setText("Selected Entry ID: " + entryId);
                return;
            }
        }
        entryText.setText("No entry selected");
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            NotificationPreferences prefs = CodexApplication.getContainer(this).getNotificationPreferences();
            if (!prefs.hasAskedPostNotifications()) {
                prefs.setAskedPostNotifications(true);
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
