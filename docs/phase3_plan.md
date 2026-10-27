# Phase 3 Implementation Plan (Final) — Daily Insight Android Integration

> [!IMPORTANT]
> **Plan updated with final amendments. Ready to build test-first.**
> Locked decisions from AGENTS.md and user amendments 1–10 incorporated.

---

## 0. Scope Boundary

- **In Scope**:
  - `BaseInsightWidgetProvider` with `SmallInsightWidgetProvider`, `MediumInsightWidgetProvider`, `LargeInsightWidgetProvider`.
  - Direct rendering in `goAsync` bounded executor via shared `WidgetRenderer` (which resolves layout based on the widget provider component name).
  - RemoteViews rendering with `apply`-based unit tests for all 3 sizes and empty state.
  - Canonical shuffle and bookmark handling carrying explicit `desiredBookmarkState` (`boolean`).
  - Stale/trashed entry graceful no-op and auto-refresh to eligible entry.
  - Multi-tier refresh: single idempotent `ensureScheduled()` called from `onEnabled`/`onUpdate`, `MainActivity.onResume`, and `BootReceiver`.
  - Self-rescheduling unique one-time WorkManager workers: `REPLACE` from outside the worker, `APPEND_OR_REPLACE` from inside the worker.
  - Pure `NextRunCalculator` supporting `date.atStartOfDay(zone).plusSeconds(5)` for midnight rollover (tested with DST midnight jumps e.g. `America/Havana`) and daily notification scheduling.
  - Notification idempotency via `last_notified_date` written synchronously; fixed notification IDs per channel (`ID_INSIGHT = 1001`, `ID_REVIEW = 1002`).
  - Late-worker rule evaluating input `KEY_SCHEDULED_DATE` vs today's date.
  - Lockscreen privacy via `setPublicVersion()`.
  - Permission handling: `POST_NOTIFICATIONS` asked at most once via Activity Result API with persisted boolean flag.
  - Pure-Java `KeyValueStore` + `NotificationPreferences` (default `notif_review_enabled = false`).
  - Domain read model `InsightDetails` and `InsightDetailsRepository` (zero `data.entity.*` or `data.dao.*` in `integration`).
  - `CodexApplication` in `com.preston.codex.CodexApplication` matching manifest, with replaceable `AppContainer` for testing.
  - `DevPanelLauncher` in `src/debug` (opens `DebugDevPanelActivity`) and `src/release` (no-op); debug-only tests in `src/testDebug`.
  - Pinned `androidx.work:work-testing:2.12.0`.
  - Manifest: `android:allowBackup="false"`, Robolectric `requestedPermissions` test, CI release manifest check for NO `INTERNET`.
- **Out of Scope**:
  - Tasks, recurrence, task reminders, large widget task list (Phase 4).
  - Main app screens (Phase 6).

---

## 1. WorkManager Scheduling & Policies

### 1.1 Unique Work Names
- `"codex_widget_midnight_refresh"`
- `"codex_insight_notification"`
- `"codex_review_notification"`

### 1.2 Callers & Work Policies
| Caller | Context | Policy |
|---|---|---|
| `NotificationScheduler.ensureScheduled()` | Outside worker (app start, boot, timezone change, widget update) | `ExistingWorkPolicy.REPLACE` |
| `NotificationScheduler.scheduleNow()` | Dev panel manual trigger | `ExistingWorkPolicy.REPLACE` |
| `WidgetMidnightRefreshWorker` (self-reschedule) | Inside worker | `ExistingWorkPolicy.APPEND_OR_REPLACE` |
| `InsightNotificationWorker` (self-reschedule) | Inside worker | `ExistingWorkPolicy.APPEND_OR_REPLACE` |
| `ReviewNotificationWorker` (self-reschedule) | Inside worker | `ExistingWorkPolicy.APPEND_OR_REPLACE` |

### 1.3 Input Data & Late-Worker Semantics
When scheduling, input data includes:
- `KEY_SCHEDULED_DATE`: ISO string (e.g. `"2026-10-26"`).

When worker runs:
1. Read `scheduledDate = inputData.getString(KEY_SCHEDULED_DATE)`.
2. Read `today = LocalDate.now(clock)`.
3. Check `lastNotifiedDate` from `KeyValueStore`:
   - If `lastNotifiedDate == today`: already notified today -> skip posting, enqueue next run with `APPEND_OR_REPLACE`, return `success()`.
4. If `scheduledDate != null && LocalDate.parse(scheduledDate).isBefore(today)`:
   - Worker ran late and day has already passed -> skip posting stale reminder, enqueue next run for upcoming target time with `APPEND_OR_REPLACE`, return `success()`.
5. Otherwise (on time or delayed within the same date):
   - Post notification with fixed ID.
   - Synchronously write `last_notified_date = today.toString()`.
   - Enqueue next run with `APPEND_OR_REPLACE`, return `success()`.

---

## 2. Package Structure & Architecture Boundaries

- Package root: `com.preston.codex`
- Application class: `com.preston.codex.CodexApplication`
- Manifest declarations:
  - `<application android:name=".CodexApplication" android:allowBackup="false" ...>`
  - `<receiver android:name=".integration.widget.SmallInsightWidgetProvider" ... />`
  - `<receiver android:name=".integration.widget.MediumInsightWidgetProvider" ... />`
  - `<receiver android:name=".integration.widget.LargeInsightWidgetProvider" ... />`
  - `<receiver android:name=".integration.widget.WidgetActionReceiver" android:exported="false" />`
  - `<receiver android:name=".integration.DateTimeChangeReceiver" android:exported="true">`
    (listens to `android.intent.action.TIME_SET`, `android.intent.action.TIMEZONE_CHANGED`)
  - `<receiver android:name=".integration.BootReceiver" android:exported="true">`
    (listens to `android.intent.action.BOOT_COMPLETED`, `android.intent.action.MY_PACKAGE_REPLACED`)

`ArchitectureRulesTest` strictly enforces:
- `com.preston.codex.integration.*` has **zero** imports of `com.preston.codex.data.entity.*` and `com.preston.codex.data.dao.*`.
- `com.preston.codex.domain.*` has **zero** imports of `android.*`.

---

## 3. Pure Java NextRunCalculator

- Daily time target calculation:
  ```java
  ZonedDateTime now = ZonedDateTime.now(clock.withZone(zone));
  ZonedDateTime targetToday = now.toLocalDate().atTime(targetTime).atZone(zone);
  if (!now.isBefore(targetToday)) {
      targetToday = now.toLocalDate().plusDays(1).atTime(targetTime).atZone(zone);
  }
  return Duration.between(now, targetToday);
  ```
- Midnight rollover target calculation:
  ```java
  LocalDate nextDate = now.toLocalDate().plusDays(1);
  ZonedDateTime nextMidnight = nextDate.atStartOfDay(zone).plusSeconds(5);
  return Duration.between(now, nextMidnight);
  ```
  Safely handles midnight skips in zones like `America/Havana` where clocks jump past 00:00.

---

## 4. Widget Rendering & Provider Hierarchy

- `BaseInsightWidgetProvider extends AppWidgetProvider`:
  - `onEnabled`: calls `container.getNotificationScheduler().ensureScheduled(context)`.
  - `onUpdate`: calls `container.getNotificationScheduler().ensureScheduled(context)` and calls `goAsync()` to render via `WidgetRenderer`.
- Concrete providers:
  - `SmallInsightWidgetProvider`
  - `MediumInsightWidgetProvider`
  - `LargeInsightWidgetProvider`
- `WidgetRenderer.renderWidgets(Context context, int[] appWidgetIds)`:
  - Determines provider component name for each widget ID.
  - Selects `R.layout.widget_small`, `widget_medium`, or `widget_large`.
  - Sets up PendingIntents with unique request codes `(appWidgetId * 10) + actionCode`.
  - Bookmark PendingIntent sets `EXTRA_DESIRED_BOOKMARK_STATE = !isBookmarked`.
  - Updates `AppWidgetManager`.
- `WidgetActionReceiver.onReceive`:
  - `PendingResult pr = goAsync();`
  - In single-thread executor:
    ```java
    try {
        if (ACTION_SHUFFLE.equals(action)) {
            container.getDailyInsightEngine().shuffle();
        } else if (ACTION_BOOKMARK.equals(action)) {
            long id = intent.getLongExtra(EXTRA_ENTRY_ID, -1L);
            boolean desired = intent.getBooleanExtra(EXTRA_DESIRED_BOOKMARK_STATE, false);
            if (id > 0 && container.getDailyInsightRepository().isEntryEligible(id)) {
                container.getBookmarkService().setBookmark(id, desired);
            }
        }
        container.getWidgetRenderer().renderAllWidgets(context);
    } finally {
        pr.finish();
    }
    ```

---

## 5. DevPanel & Build Variants

- `src/debug/java/com/preston/codex/DevPanelLauncher.java`: launches `DebugDevPanelActivity`.
- `src/release/java/com/preston/codex/DevPanelLauncher.java`: no-op `launch(Activity activity)`.
- `src/debug/java/com/preston/codex/debug/DebugDevPanelActivity.java`:
  - Imports `assets/sample_library.md`.
  - Debug-only archive and trash SQL / DAO executions.
  - Widget refresh and notification triggers.
- `src/testDebug/java/com/preston/codex/debug/DebugDevPanelTest.java`: verifies dev panel actions in debug variant.
- `./gradlew test` executes both `testDebugUnitTest` and `testReleaseUnitTest`.

---

## 6. Permissions & Manifest Verification

- App manifest has `android:allowBackup="false"`.
- Declared permissions:
  ```xml
  <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
  <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
  ```
- Merged manifest test asserts NO `android.permission.INTERNET`.
- Resources contain `res/drawable/ic_notification.xml`.
- `NotificationPreferences` stores `KEY_POST_NOTIFICATIONS_ASKED` boolean.

---

## 7. Test Counts Per Class

Actual `@Test` counts after amendments 1–10 (both `testDebugUnitTest` and `testReleaseUnitTest` compile and pass; total confirmed by `./gradlew test`):

### Pure Java Tests (`src/test/java`)
1. `NextRunCalculatorTest`: 8 tests
2. `ContentHashCalculatorTest`: 5 tests
3. `EntryClassifierTest`: 7 tests
4. `EntryMatcherTest`: 16 tests
5. `EntryNormalizerTest`: 9 tests
6. `FingerprintCalculatorTest`: 8 tests
7. `JaccardCalculatorTest`: 5 tests
8. `MarkdownParserTest`: 22 tests
9. `ImportCommitterTest`: 38 tests
10. `MigrationTest`: 1 test
11. `RoomBookmarkIntegrityTest`: 1 test
12. `RoomDailyInsightDaoTest`: 3 tests
13. `RoomReviewDaoTest`: 3 tests
14. `ClockTest`: 2 tests
15. `BookmarkServiceTest`: 3 tests
16. `RelationshipServiceTest`: 6 tests
17. `ReviewSchedulerTest`: 17 tests
18. `DailyInsightEngineTest`: 15 tests
19. `NotificationPreferencesTest`: 5 tests
20. `InsightViewDataMapperTest`: 4 tests
21. `WidgetInsightViewDataTest`: 3 tests
22. `ArchitectureRulesTest`: 2 tests

### Robolectric Tests (`src/test/java`)
23. `CodexApplicationTest`: 2 tests
24. `ManifestPermissionTest`: 2 tests
25. `WidgetRemoteViewsApplyTest`: 4 tests
26. `InsightWidgetProviderTest`: 4 tests
27. `WidgetActionReceiverTest`: 3 tests
28. `DateTimeChangeReceiverTest`: 2 tests
29. `BootReceiverTest`: 2 tests
30. `NotificationSchedulerWorkTestingTest`: 3 tests
31. `InsightNotificationWorkerTest`: 4 tests
32. `ReviewNotificationWorkerTest`: 3 tests
33. `WidgetMidnightRefreshWorkerTest`: 1 test

### Debug Variant Tests (`src/testDebug/java`)
34. `DebugDevPanelTest`: 3 tests

**Total**: 217 tests across 34 classes, all passing in both debug and release configurations.
