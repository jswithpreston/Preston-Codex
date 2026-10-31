package com.preston.codex;

import android.content.Context;

import com.preston.codex.data.database.AppDatabase;
import com.preston.codex.data.database.RoomTransactionRunner;
import com.preston.codex.data.repository.RoomDailyInsightRepository;
import com.preston.codex.data.repository.RoomInsightDetailsRepository;
import com.preston.codex.data.repository.RoomKnowledgeRepository;
import com.preston.codex.data.repository.RoomReviewRepository;
import com.preston.codex.domain.knowledge.BookmarkService;
import com.preston.codex.domain.knowledge.KnowledgeDomainRepository;
import com.preston.codex.domain.recall.ReviewConfig;
import com.preston.codex.domain.recall.ReviewRepository;
import com.preston.codex.domain.recall.ReviewScheduler;
import com.preston.codex.domain.scheduling.DailyInsightEngine;
import com.preston.codex.domain.scheduling.DailyInsightRepository;
import com.preston.codex.domain.scheduling.InsightDetailsRepository;
import com.preston.codex.domain.scheduling.InsightSelector;
import com.preston.codex.domain.scheduling.RandomInsightSelector;
import com.preston.codex.integration.KeyValueStore;
import com.preston.codex.integration.SharedPreferencesKeyValueStore;
import com.preston.codex.integration.notifications.NotificationPreferences;
import com.preston.codex.integration.notifications.NotificationScheduler;
import com.preston.codex.integration.widget.InsightViewDataMapper;
import com.preston.codex.integration.widget.WidgetRenderer;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

/**
 * Manual composition root holding singletons and dependencies.
 * Kept small, lazy, and replaceable for tests.
 */
public class AppContainer {

    private final Context appContext;
    private final Clock clock;
    private final Supplier<ZoneId> zoneSupplier;
    private final ExecutorService executorService;

    private AppDatabase database;
    private RoomTransactionRunner transactionRunner;
    private DailyInsightRepository dailyInsightRepository;
    private InsightDetailsRepository insightDetailsRepository;
    private KnowledgeDomainRepository knowledgeRepository;
    private ReviewRepository reviewRepository;
    private DailyInsightEngine dailyInsightEngine;
    private BookmarkService bookmarkService;
    private ReviewScheduler reviewScheduler;
    private KeyValueStore keyValueStore;
    private NotificationPreferences notificationPreferences;
    private NotificationScheduler notificationScheduler;
    private WidgetRenderer widgetRenderer;

    public AppContainer(Context context) {
        this(context, Clock.systemDefaultZone(), ZoneId::systemDefault, Executors.newSingleThreadExecutor());
    }

    public AppContainer(Context context, Clock clock, Supplier<ZoneId> zoneSupplier, ExecutorService executorService) {
        this.appContext = Objects.requireNonNull(context, "context must not be null").getApplicationContext();
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.zoneSupplier = Objects.requireNonNull(zoneSupplier, "zoneSupplier must not be null");
        this.executorService = Objects.requireNonNull(executorService, "executorService must not be null");
    }

    public Context getAppContext() {
        return appContext;
    }

    public Clock getClock() {
        return clock;
    }

    public Supplier<ZoneId> getZoneSupplier() {
        return zoneSupplier;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public synchronized AppDatabase getDatabase() {
        if (database == null) {
            database = AppDatabase.getInstance(appContext);
        }
        return database;
    }

    public synchronized RoomTransactionRunner getTransactionRunner() {
        if (transactionRunner == null) {
            transactionRunner = new RoomTransactionRunner(getDatabase());
        }
        return transactionRunner;
    }

    public synchronized DailyInsightRepository getDailyInsightRepository() {
        if (dailyInsightRepository == null) {
            dailyInsightRepository = new RoomDailyInsightRepository(
                    getDatabase().dailyInsightDao(),
                    getDatabase().knowledgeEntryDao()
            );
        }
        return dailyInsightRepository;
    }

    public synchronized InsightDetailsRepository getInsightDetailsRepository() {
        if (insightDetailsRepository == null) {
            insightDetailsRepository = new RoomInsightDetailsRepository(
                    getDatabase().dailyInsightDao(),
                    getDatabase().knowledgeEntryDao(),
                    getDatabase().knowledgeSourceDao()
            );
        }
        return insightDetailsRepository;
    }

    public synchronized KnowledgeDomainRepository getKnowledgeRepository() {
        if (knowledgeRepository == null) {
            knowledgeRepository = new RoomKnowledgeRepository(
                    getDatabase().knowledgeEntryDao(),
                    getDatabase().knowledgeRelationshipDao()
            );
        }
        return knowledgeRepository;
    }

    public synchronized ReviewRepository getReviewRepository() {
        if (reviewRepository == null) {
            reviewRepository = new RoomReviewRepository(
                    getDatabase().reviewDao(),
                    getDatabase().knowledgeEntryDao()
            );
        }
        return reviewRepository;
    }

    public synchronized DailyInsightEngine getDailyInsightEngine() {
        if (dailyInsightEngine == null) {
            InsightSelector selector = new RandomInsightSelector(new java.util.Random());
            dailyInsightEngine = new DailyInsightEngine(
                    getDailyInsightRepository(),
                    selector,
                    clock,
                    zoneSupplier,
                    getTransactionRunner()
            );
        }
        return dailyInsightEngine;
    }

    public synchronized BookmarkService getBookmarkService() {
        if (bookmarkService == null) {
            bookmarkService = new BookmarkService(getKnowledgeRepository(), getTransactionRunner());
        }
        return bookmarkService;
    }

    public synchronized ReviewScheduler getReviewScheduler() {
        if (reviewScheduler == null) {
            reviewScheduler = new ReviewScheduler(
                    getReviewRepository(),
                    new ReviewConfig(),
                    clock,
                    zoneSupplier,
                    getTransactionRunner()
            );
        }
        return reviewScheduler;
    }

    public synchronized KeyValueStore getKeyValueStore() {
        if (keyValueStore == null) {
            keyValueStore = new SharedPreferencesKeyValueStore(appContext, "codex_notification_prefs");
        }
        return keyValueStore;
    }

    public synchronized NotificationPreferences getNotificationPreferences() {
        if (notificationPreferences == null) {
            notificationPreferences = new NotificationPreferences(getKeyValueStore());
        }
        return notificationPreferences;
    }

    public synchronized WidgetRenderer getWidgetRenderer() {
        if (widgetRenderer == null) {
            widgetRenderer = new WidgetRenderer(
                    getInsightDetailsRepository(),
                    new InsightViewDataMapper(),
                    clock,
                    zoneSupplier
            );
        }
        return widgetRenderer;
    }

    public synchronized NotificationScheduler getNotificationScheduler() {
        if (notificationScheduler == null) {
            notificationScheduler = new NotificationScheduler(
                    appContext,
                    getNotificationPreferences(),
                    clock,
                    zoneSupplier
            );
        }
        return notificationScheduler;
    }

    private com.preston.codex.domain.time.LocalDateProvider localDateProvider;
    private com.preston.codex.domain.tasks.repository.TaskRepository taskRepository;
    private com.preston.codex.domain.tasks.recurrence.OccurrenceGenerator occurrenceGenerator;
    private com.preston.codex.domain.tasks.recurrence.RecurrenceService recurrenceService;
    private com.preston.codex.domain.tasks.service.TaskService taskService;
    private com.preston.codex.domain.tasks.recurrence.RecurrenceEditService recurrenceEditService;
    private com.preston.codex.domain.tasks.missed.MissedOccurrenceService missedOccurrenceService;
    private com.preston.codex.domain.tasks.postpone.PostponementService postponementService;
    private com.preston.codex.domain.tasks.backlog.BacklogReviewService backlogReviewService;

    public synchronized com.preston.codex.domain.time.LocalDateProvider getLocalDateProvider() {
        if (localDateProvider == null) {
            localDateProvider = new com.preston.codex.domain.time.LocalDateProvider(clock, zoneSupplier);
        }
        return localDateProvider;
    }

    public synchronized com.preston.codex.domain.tasks.repository.TaskRepository getTaskRepository() {
        if (taskRepository == null) {
            taskRepository = new com.preston.codex.data.repository.RoomTaskRepository(
                    getDatabase().taskDao(),
                    getDatabase().taskTagDao(),
                    getDatabase().recurrenceRuleDao(),
                    getDatabase().taskOccurrenceDao(),
                    getDatabase().taskEventDao(),
                    getDatabase().rescheduleHistoryDao(),
                    getDatabase().reminderStateDao()
            );
        }
        return taskRepository;
    }

    public synchronized com.preston.codex.domain.tasks.recurrence.OccurrenceGenerator getOccurrenceGenerator() {
        if (occurrenceGenerator == null) {
            occurrenceGenerator = new com.preston.codex.domain.tasks.recurrence.OccurrenceGenerator();
        }
        return occurrenceGenerator;
    }

    public synchronized com.preston.codex.domain.tasks.recurrence.RecurrenceService getRecurrenceService() {
        if (recurrenceService == null) {
            recurrenceService = new com.preston.codex.domain.tasks.recurrence.RecurrenceService(
                    getTaskRepository(),
                    getOccurrenceGenerator(),
                    getLocalDateProvider(),
                    getTransactionRunner()
            );
        }
        return recurrenceService;
    }

    public synchronized com.preston.codex.domain.tasks.service.TaskService getTaskService() {
        if (taskService == null) {
            taskService = new com.preston.codex.domain.tasks.service.TaskService(
                    getTaskRepository(),
                    getRecurrenceService(),
                    getLocalDateProvider(),
                    getKeyValueStore(),
                    getTransactionRunner()
            );
        }
        return taskService;
    }

    public synchronized com.preston.codex.domain.tasks.recurrence.RecurrenceEditService getRecurrenceEditService() {
        if (recurrenceEditService == null) {
            recurrenceEditService = new com.preston.codex.domain.tasks.recurrence.RecurrenceEditService(
                    getTaskRepository(),
                    getRecurrenceService(),
                    getLocalDateProvider(),
                    getTransactionRunner()
            );
        }
        return recurrenceEditService;
    }

    public synchronized com.preston.codex.domain.tasks.missed.MissedOccurrenceService getMissedOccurrenceService() {
        if (missedOccurrenceService == null) {
            missedOccurrenceService = new com.preston.codex.domain.tasks.missed.MissedOccurrenceService(
                    getTaskRepository(),
                    getLocalDateProvider(),
                    getTransactionRunner()
            );
        }
        return missedOccurrenceService;
    }

    public synchronized com.preston.codex.domain.tasks.postpone.PostponementService getPostponementService() {
        if (postponementService == null) {
            postponementService = new com.preston.codex.domain.tasks.postpone.PostponementService(
                    getTaskRepository(),
                    getLocalDateProvider(),
                    getTransactionRunner()
            );
        }
        return postponementService;
    }

    public synchronized com.preston.codex.domain.tasks.backlog.BacklogReviewService getBacklogReviewService() {
        if (backlogReviewService == null) {
            backlogReviewService = new com.preston.codex.domain.tasks.backlog.BacklogReviewService(
                    getTaskRepository(),
                    getLocalDateProvider(),
                    getTransactionRunner()
            );
        }
        return backlogReviewService;
    }
}
