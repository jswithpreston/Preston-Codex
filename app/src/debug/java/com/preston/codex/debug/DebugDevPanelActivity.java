package com.preston.codex.debug;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.preston.codex.AppContainer;
import com.preston.codex.CodexApplication;
import com.preston.codex.MainActivity;
import com.preston.codex.R;
import com.preston.codex.data.repository.RoomMatchLookup;
import com.preston.codex.domain.recall.ReviewQueueItem;
import com.preston.codex.domain.scheduling.InsightDetails;
import com.preston.codex.importer.ContentHashCalculator;
import com.preston.codex.importer.EntryClassifier;
import com.preston.codex.importer.EntryMatcher;
import com.preston.codex.importer.EntryNormalizer;
import com.preston.codex.importer.FingerprintCalculator;
import com.preston.codex.importer.ImportCommitter;
import com.preston.codex.importer.ImportPreviewBuilder;
import com.preston.codex.importer.JaccardCalculator;
import com.preston.codex.importer.MarkdownParser;
import com.preston.codex.importer.model.ImportPreview;
import com.preston.codex.importer.model.ImportReport;
import com.preston.codex.importer.model.ParsedFile;
import com.preston.codex.integration.notifications.NotificationChannelInitializer;
import com.preston.codex.integration.notifications.NotificationPreferences;
import com.preston.codex.integration.widget.WidgetRenderer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

/**
 * Debug-only Dev Panel Activity.
 * Never compiles into release builds.
 */
public class DebugDevPanelActivity extends AppCompatActivity {

    private TextView statusText;
    private SwitchCompat switchInsight;
    private SwitchCompat switchReview;
    private Button btnInsightTime;
    private Button btnReviewTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_dev_panel);

        statusText = findViewById(R.id.text_status);
        switchInsight = findViewById(R.id.switch_insight_enabled);
        switchReview = findViewById(R.id.switch_review_enabled);
        btnInsightTime = findViewById(R.id.btn_pick_insight_time);
        btnReviewTime = findViewById(R.id.btn_pick_review_time);

        findViewById(R.id.btn_import_sample).setOnClickListener(v -> importSampleLibrary());
        findViewById(R.id.btn_archive_current).setOnClickListener(v -> archiveCurrentEntry());
        findViewById(R.id.btn_trash_current).setOnClickListener(v -> trashCurrentEntry());
        findViewById(R.id.btn_refresh_widgets).setOnClickListener(v -> refreshWidgets());
        findViewById(R.id.btn_fire_insight_notif).setOnClickListener(v -> fireInsightNotificationNow());
        findViewById(R.id.btn_fire_review_notif).setOnClickListener(v -> fireReviewNotificationNow());

        setupPreferencesUi();
    }

    private void setupPreferencesUi() {
        AppContainer container = CodexApplication.getContainer(this);
        NotificationPreferences prefs = container.getNotificationPreferences();

        switchInsight.setChecked(prefs.isInsightNotificationEnabled());
        switchInsight.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setInsightNotificationEnabled(isChecked);
            container.getNotificationScheduler().ensureScheduled();
            updateStatus("Insight notification " + (isChecked ? "enabled" : "disabled"));
        });

        switchReview.setChecked(prefs.isReviewNotificationEnabled());
        switchReview.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.setReviewNotificationEnabled(isChecked);
            container.getNotificationScheduler().ensureScheduled();
            updateStatus("Review reminder " + (isChecked ? "enabled" : "disabled"));
        });

        updateTimeButtons(prefs);

        btnInsightTime.setOnClickListener(v -> {
            LocalTime cur = prefs.getInsightNotificationTime();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                prefs.setInsightTimeParts(hourOfDay, minute);
                container.getNotificationScheduler().ensureScheduled();
                updateTimeButtons(prefs);
                updateStatus("Insight time set to " + String.format("%02d:%02d", hourOfDay, minute));
            }, cur.getHour(), cur.getMinute(), true).show();
        });

        btnReviewTime.setOnClickListener(v -> {
            LocalTime cur = prefs.getReviewNotificationTime();
            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                prefs.setReviewTimeParts(hourOfDay, minute);
                container.getNotificationScheduler().ensureScheduled();
                updateTimeButtons(prefs);
                updateStatus("Review time set to " + String.format("%02d:%02d", hourOfDay, minute));
            }, cur.getHour(), cur.getMinute(), true).show();
        });
    }

    private void updateTimeButtons(NotificationPreferences prefs) {
        LocalTime it = prefs.getInsightNotificationTime();
        btnInsightTime.setText(String.format("Insight Time: %02d:%02d", it.getHour(), it.getMinute()));

        LocalTime rt = prefs.getReviewNotificationTime();
        btnReviewTime.setText(String.format("Review Time: %02d:%02d", rt.getHour(), rt.getMinute()));
    }

    private void importSampleLibrary() {
        AppContainer container = CodexApplication.getContainer(this);
        container.getExecutorService().execute(() -> {
            try {
                InputStream is = getAssets().open("sample_library.md");
                StringBuilder sb = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                }
                String content = sb.toString();

                MarkdownParser parser = new MarkdownParser();
                ParsedFile parsedFile = parser.parse("sample_library.md", content);

                EntryNormalizer normalizer = new EntryNormalizer();
                FingerprintCalculator fpCalc = new FingerprintCalculator(normalizer);
                ContentHashCalculator hashCalc = new ContentHashCalculator(normalizer);
                JaccardCalculator jaccardCalc = new JaccardCalculator();

                ImportCommitter committer = new ImportCommitter(
                        container.getDatabase(),
                        container.getClock(),
                        normalizer,
                        fpCalc,
                        hashCalc,
                        jaccardCalc
                );
                ImportReport report = committer.commit(parsedFile, "sample_library_debug_hash");

                container.getWidgetRenderer().renderAllWidgets(this);

                runOnUiThread(() -> {
                    updateStatus("Imported " + report.getEntriesNew() + " entries (" + report.getEntriesUnchanged() + " unchanged)");
                    Toast.makeText(this, "Import completed: " + report.getEntriesNew() + " new", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> updateStatus("Import error: " + e.getMessage()));
            }
        });
    }

    private void archiveCurrentEntry() {
        AppContainer container = CodexApplication.getContainer(this);
        container.getExecutorService().execute(() -> {
            Optional<Long> currentIdOpt = container.getDailyInsightEngine().getOrSelectDailyInsight();
            if (!currentIdOpt.isPresent()) {
                runOnUiThread(() -> updateStatus("No active entry to archive"));
                return;
            }
            long id = currentIdOpt.get();
            container.getDatabase().getOpenHelper().getWritableDatabase()
                    .execSQL("UPDATE knowledge_entry SET is_archived = 1 WHERE id = ?", new Object[]{id});
            container.getWidgetRenderer().renderAllWidgets(this);
            runOnUiThread(() -> {
                updateStatus("Archived entry " + id);
                Toast.makeText(this, "Archived entry " + id, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void trashCurrentEntry() {
        AppContainer container = CodexApplication.getContainer(this);
        container.getExecutorService().execute(() -> {
            Optional<Long> currentIdOpt = container.getDailyInsightEngine().getOrSelectDailyInsight();
            if (!currentIdOpt.isPresent()) {
                runOnUiThread(() -> updateStatus("No active entry to trash"));
                return;
            }
            long id = currentIdOpt.get();
            container.getDatabase().getOpenHelper().getWritableDatabase()
                    .execSQL("UPDATE knowledge_entry SET is_trashed = 1 WHERE id = ?", new Object[]{id});
            container.getWidgetRenderer().renderAllWidgets(this);
            runOnUiThread(() -> {
                updateStatus("Trashed entry " + id);
                Toast.makeText(this, "Trashed entry " + id, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void refreshWidgets() {
        AppContainer container = CodexApplication.getContainer(this);
        container.getExecutorService().execute(() -> {
            container.getWidgetRenderer().renderAllWidgets(this);
            runOnUiThread(() -> {
                updateStatus("Refreshed all widgets");
                Toast.makeText(this, "Widgets refreshed", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void fireInsightNotificationNow() {
        AppContainer container = CodexApplication.getContainer(this);
        NotificationChannelInitializer.ensureChannelsCreated(this);
        container.getExecutorService().execute(() -> {
            LocalDate today = LocalDate.now(container.getClock().withZone(container.getZoneSupplier().get()));
            container.getDailyInsightEngine().getOrSelectDailyInsight();
            Optional<InsightDetails> detailsOpt = container.getInsightDetailsRepository().getDailyInsightDetails(today);
            if (!detailsOpt.isPresent()) {
                runOnUiThread(() -> updateStatus("No eligible insight to notify"));
                return;
            }

            InsightDetails details = detailsOpt.get();

            Notification publicNotif = new NotificationCompat.Builder(this, NotificationChannelInitializer.CHANNEL_ID_INSIGHT)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(getString(R.string.notif_insight_title))
                    .setContentText(getString(R.string.notif_insight_public_text))
                    .build();

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(WidgetRenderer.EXTRA_ENTRY_ID, details.getEntryId());
            PendingIntent pi = PendingIntent.getActivity(
                    this,
                    NotificationChannelInitializer.NOTIFICATION_ID_INSIGHT,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Notification notif = new NotificationCompat.Builder(this, NotificationChannelInitializer.CHANNEL_ID_INSIGHT)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(getString(R.string.notif_insight_title))
                    .setContentText(details.getQuoteText())
                    .setStyle(new NotificationCompat.BigTextStyle().bigText("“" + details.getQuoteText() + "”"))
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(publicNotif)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();

            NotificationManagerCompat.from(this).notify(NotificationChannelInitializer.NOTIFICATION_ID_INSIGHT, notif);
            runOnUiThread(() -> {
                updateStatus("Fired Daily Insight notification");
                Toast.makeText(this, "Daily Insight notification posted", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void fireReviewNotificationNow() {
        AppContainer container = CodexApplication.getContainer(this);
        NotificationChannelInitializer.ensureChannelsCreated(this);
        container.getExecutorService().execute(() -> {
            List<ReviewQueueItem> queue = container.getReviewScheduler().getReviewQueue();
            int count = queue != null ? queue.size() : 0;
            String text = count == 1
                    ? getString(R.string.notif_review_text_single)
                    : getString(R.string.notif_review_text_multiple, count);

            Notification publicNotif = new NotificationCompat.Builder(this, NotificationChannelInitializer.CHANNEL_ID_REVIEW)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(getString(R.string.notif_review_title))
                    .setContentText(getString(R.string.notif_review_public_text))
                    .build();

            Intent intent = new Intent(this, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(
                    this,
                    NotificationChannelInitializer.NOTIFICATION_ID_REVIEW,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Notification notif = new NotificationCompat.Builder(this, NotificationChannelInitializer.CHANNEL_ID_REVIEW)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(getString(R.string.notif_review_title))
                    .setContentText(text)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(publicNotif)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build();

            NotificationManagerCompat.from(this).notify(NotificationChannelInitializer.NOTIFICATION_ID_REVIEW, notif);
            runOnUiThread(() -> {
                updateStatus("Fired Review notification (count=" + count + ")");
                Toast.makeText(this, "Review notification posted (" + count + " items)", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updateStatus(String msg) {
        if (statusText != null) {
            statusText.setText(msg);
        }
    }
}
