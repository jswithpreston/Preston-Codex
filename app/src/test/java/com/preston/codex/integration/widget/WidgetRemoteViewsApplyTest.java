package com.preston.codex.integration.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;

import com.preston.codex.R;
import com.preston.codex.domain.scheduling.InsightDetails;
import com.preston.codex.domain.scheduling.InsightDetailsRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class WidgetRemoteViewsApplyTest {

    private Context context;
    private WidgetRenderer renderer;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        Clock clock = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneId.of("UTC"));
        InsightDetailsRepository repo = new InsightDetailsRepository() {
            @Override
            public Optional<InsightDetails> getDailyInsightDetails(LocalDate date) {
                return Optional.empty();
            }

            @Override
            public Optional<InsightDetails> getInsightDetailsById(long entryId) {
                return Optional.empty();
            }
        };
        renderer = new WidgetRenderer(repo, new InsightViewDataMapper(), clock, () -> ZoneId.of("UTC"));
    }

    @Test
    public void smallWidget_populated_appliesViewsCorrectly() {
        WidgetInsightViewData data = new WidgetInsightViewData(
                1L,
                "“Deep work produces rare value.”",
                "— Cal Newport, Deep Work",
                "Focus deeply.",
                "Mon, Oct 26, 2026",
                false,
                false
        );

        RemoteViews remoteViews = renderer.buildRemoteViews(context, 101, R.layout.widget_small, data);
        View root = remoteViews.apply(context, new FrameLayout(context));

        View populated = root.findViewById(R.id.widget_populated_container);
        View empty = root.findViewById(R.id.widget_empty_container);
        assertEquals(View.VISIBLE, populated.getVisibility());
        assertEquals(View.GONE, empty.getVisibility());

        TextView quote = root.findViewById(R.id.widget_quote_text);
        TextView author = root.findViewById(R.id.widget_author_text);
        TextView date = root.findViewById(R.id.widget_date_text);

        assertEquals("“Deep work produces rare value.”", quote.getText().toString());
        assertEquals("— Cal Newport, Deep Work", author.getText().toString());
        assertEquals("Mon, Oct 26, 2026", date.getText().toString());

        // Buttons must not exist in small layout
        assertNull(root.findViewById(R.id.widget_btn_shuffle));
        assertNull(root.findViewById(R.id.widget_btn_bookmark));
    }

    @Test
    public void mediumWidget_populated_hasButtonsWithCorrectLabels() {
        WidgetInsightViewData data = new WidgetInsightViewData(
                2L,
                "“Clear goals drive action.”",
                "— Author",
                "",
                "Mon, Oct 26, 2026",
                true,
                false
        );

        RemoteViews remoteViews = renderer.buildRemoteViews(context, 102, R.layout.widget_medium, data);
        View root = remoteViews.apply(context, new FrameLayout(context));

        TextView shuffleBtn = root.findViewById(R.id.widget_btn_shuffle);
        TextView bookmarkBtn = root.findViewById(R.id.widget_btn_bookmark);

        assertNotNull(shuffleBtn);
        assertNotNull(bookmarkBtn);
        assertEquals("Shuffle", shuffleBtn.getText().toString());
        assertEquals("Bookmarked", bookmarkBtn.getText().toString());
    }

    @Test
    public void largeWidget_populated_includesTakeawaySection() {
        WidgetInsightViewData data = new WidgetInsightViewData(
                3L,
                "“Systems beat goals.”",
                "— James Clear",
                "Focus on daily habit design rather than targets alone.",
                "Mon, Oct 26, 2026",
                false,
                false
        );

        RemoteViews remoteViews = renderer.buildRemoteViews(context, 103, R.layout.widget_large, data);
        View root = remoteViews.apply(context, new FrameLayout(context));

        TextView takeawayHeading = root.findViewById(R.id.widget_takeaway_heading);
        TextView takeawayText = root.findViewById(R.id.widget_takeaway_text);

        assertNotNull(takeawayHeading);
        assertNotNull(takeawayText);
        assertEquals("TAKEAWAY", takeawayHeading.getText().toString());
        assertEquals("Focus on daily habit design rather than targets alone.", takeawayText.getText().toString());
    }

    @Test
    public void emptyState_allWidgets_appliesEmptyStateLayout() {
        WidgetInsightViewData emptyData = WidgetInsightViewData.createEmpty("Mon, Oct 26, 2026");

        for (int layoutId : new int[]{R.layout.widget_small, R.layout.widget_medium, R.layout.widget_large}) {
            RemoteViews remoteViews = renderer.buildRemoteViews(context, 104, layoutId, emptyData);
            View root = remoteViews.apply(context, new FrameLayout(context));

            View populated = root.findViewById(R.id.widget_populated_container);
            View empty = root.findViewById(R.id.widget_empty_container);
            TextView emptyText = root.findViewById(R.id.widget_empty_text);

            assertEquals(View.GONE, populated.getVisibility());
            assertEquals(View.VISIBLE, empty.getVisibility());
            assertTrue(emptyText.getText().toString().contains("Add knowledge entries"));
        }
    }
}
