package com.preston.codex.integration.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import com.preston.codex.CodexApplication;
import com.preston.codex.MainActivity;
import com.preston.codex.R;
import com.preston.codex.domain.scheduling.DailyInsightEngine;
import com.preston.codex.domain.scheduling.InsightDetails;
import com.preston.codex.domain.scheduling.InsightDetailsRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Shared renderer that builds and pushes RemoteViews for all widget instances.
 * Pure presentation logic; reads from {@link InsightDetailsRepository}.
 */
public final class WidgetRenderer {

    public static final String ACTION_SHUFFLE = "com.preston.codex.action.WIDGET_SHUFFLE";
    public static final String ACTION_BOOKMARK = "com.preston.codex.action.WIDGET_BOOKMARK";

    public static final String EXTRA_ENTRY_ID = "com.preston.codex.extra.ENTRY_ID";
    public static final String EXTRA_DESIRED_BOOKMARK_STATE = "com.preston.codex.extra.DESIRED_BOOKMARK_STATE";
    public static final String EXTRA_APPWIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID;

    private static final int ACTION_CODE_OPEN = 1;
    private static final int ACTION_CODE_SHUFFLE = 2;
    private static final int ACTION_CODE_BOOKMARK = 3;

    private final InsightDetailsRepository repository;
    private final InsightViewDataMapper mapper;
    private final Clock clock;
    private final Supplier<ZoneId> zoneSupplier;

    public WidgetRenderer(
            InsightDetailsRepository repository,
            InsightViewDataMapper mapper,
            Clock clock,
            Supplier<ZoneId> zoneSupplier
    ) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.zoneSupplier = Objects.requireNonNull(zoneSupplier, "zoneSupplier must not be null");
    }

    public void renderAllWidgets(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);

        int[] smallIds = manager.getAppWidgetIds(new ComponentName(context, SmallInsightWidgetProvider.class));
        int[] mediumIds = manager.getAppWidgetIds(new ComponentName(context, MediumInsightWidgetProvider.class));
        int[] largeIds = manager.getAppWidgetIds(new ComponentName(context, LargeInsightWidgetProvider.class));

        if (smallIds.length > 0) renderWidgets(context, manager, smallIds);
        if (mediumIds.length > 0) renderWidgets(context, manager, mediumIds);
        if (largeIds.length > 0) renderWidgets(context, manager, largeIds);
    }

    public void renderWidgets(Context context, int[] appWidgetIds) {
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        renderWidgets(context, manager, appWidgetIds);
    }

    public void renderWidgets(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        LocalDate today = LocalDate.now(clock.withZone(zoneSupplier.get()));

        // Ensure canonical daily selection is evaluated
        DailyInsightEngine engine = CodexApplication.getContainer(context).getDailyInsightEngine();
        engine.getOrSelectDailyInsight();

        Optional<InsightDetails> detailsOpt = repository.getDailyInsightDetails(today);
        WidgetInsightViewData viewData = mapper.map(detailsOpt, today);

        for (int widgetId : appWidgetIds) {
            AppWidgetProviderInfo info = manager.getAppWidgetInfo(widgetId);
            int layoutId = resolveLayout(context, info);
            RemoteViews views = buildRemoteViews(context, widgetId, layoutId, viewData);
            manager.updateAppWidget(widgetId, views);
        }
    }

    public RemoteViews buildRemoteViews(Context context, int widgetId, int layoutId, WidgetInsightViewData viewData) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layoutId);

        if (viewData.isEmpty()) {
            views.setViewVisibility(R.id.widget_populated_container, View.GONE);
            views.setViewVisibility(R.id.widget_empty_container, View.VISIBLE);
            views.setTextViewText(R.id.widget_empty_text, context.getString(R.string.widget_empty_message));
            views.setTextViewText(R.id.widget_date_text, viewData.getFormattedDate());

            Intent openAppIntent = new Intent(context, MainActivity.class);
            openAppIntent.putExtra(EXTRA_ENTRY_ID, -1L);
            openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent openPending = PendingIntent.getActivity(
                    context,
                    (widgetId * 10) + ACTION_CODE_OPEN,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widget_root, openPending);
            views.setOnClickPendingIntent(R.id.widget_empty_container, openPending);
            return views;
        }

        views.setViewVisibility(R.id.widget_empty_container, View.GONE);
        views.setViewVisibility(R.id.widget_populated_container, View.VISIBLE);

        views.setTextViewText(R.id.widget_heading_text, context.getString(R.string.widget_heading_insight));
        views.setTextViewText(R.id.widget_date_text, viewData.getFormattedDate());
        views.setTextViewText(R.id.widget_quote_text, viewData.getQuoteText());
        views.setTextViewText(R.id.widget_author_text, viewData.getAuthorLine());

        // Large widget takeaway section
        if (layoutId == R.layout.widget_large) {
            views.setTextViewText(R.id.widget_takeaway_heading, context.getString(R.string.widget_heading_takeaway));
            views.setTextViewText(R.id.widget_takeaway_text, viewData.getInsightText());
        }

        // Tap on widget body opens MainActivity with entry ID
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.putExtra(EXTRA_ENTRY_ID, viewData.getEntryId());
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPending = PendingIntent.getActivity(
                context,
                (widgetId * 10) + ACTION_CODE_OPEN,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_populated_container, openPending);

        // Buttons for Medium and Large
        if (layoutId == R.layout.widget_medium || layoutId == R.layout.widget_large) {
            // Shuffle
            Intent shuffleIntent = new Intent(context, WidgetActionReceiver.class);
            shuffleIntent.setAction(ACTION_SHUFFLE);
            shuffleIntent.putExtra(EXTRA_APPWIDGET_ID, widgetId);
            shuffleIntent.putExtra(EXTRA_ENTRY_ID, viewData.getEntryId());
            PendingIntent shufflePending = PendingIntent.getBroadcast(
                    context,
                    (widgetId * 10) + ACTION_CODE_SHUFFLE,
                    shuffleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widget_btn_shuffle, shufflePending);
            views.setTextViewText(R.id.widget_btn_shuffle, context.getString(R.string.widget_action_shuffle));

            // Bookmark (explicit desired state)
            boolean desiredBookmark = !viewData.isBookmarked();
            Intent bookmarkIntent = new Intent(context, WidgetActionReceiver.class);
            bookmarkIntent.setAction(ACTION_BOOKMARK);
            bookmarkIntent.putExtra(EXTRA_APPWIDGET_ID, widgetId);
            bookmarkIntent.putExtra(EXTRA_ENTRY_ID, viewData.getEntryId());
            bookmarkIntent.putExtra(EXTRA_DESIRED_BOOKMARK_STATE, desiredBookmark);
            PendingIntent bookmarkPending = PendingIntent.getBroadcast(
                    context,
                    (widgetId * 10) + ACTION_CODE_BOOKMARK,
                    bookmarkIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            views.setOnClickPendingIntent(R.id.widget_btn_bookmark, bookmarkPending);

            if (viewData.isBookmarked()) {
                views.setTextViewText(R.id.widget_btn_bookmark, context.getString(R.string.widget_action_bookmarked));
            } else {
                views.setTextViewText(R.id.widget_btn_bookmark, context.getString(R.string.widget_action_bookmark));
            }
        }

        return views;
    }

    private static int resolveLayout(Context context, AppWidgetProviderInfo info) {
        if (info != null && info.provider != null) {
            String className = info.provider.getClassName();
            if (className.equals(LargeInsightWidgetProvider.class.getName())) {
                return R.layout.widget_large;
            } else if (className.equals(MediumInsightWidgetProvider.class.getName())) {
                return R.layout.widget_medium;
            } else {
                return R.layout.widget_small;
            }
        }
        return R.layout.widget_medium;
    }
}
