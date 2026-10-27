# Preston Codex

Offline-first personal planner + knowledge-retention app for Android. Native Java, Room/SQLite, WorkManager, AppWidgetProvider + RemoteViews. No backend, no AI, no network dependency.

## Stack

| Component | Version |
|---|---|
| Language | Java 17 |
| Gradle / AGP | 8.13 / 8.13.0 |
| compileSdk / targetSdk | 36 |
| minSdk | 26 |
| Room | 2.8.5 |
| WorkManager | 2.12.0 |
| Material Components | 1.14.0 |
| Testing | JUnit 4, Mockito, Robolectric 4.13, WorkManager testing |
| CI | GitHub Actions (JDK 17 Temurin) |
| Package | `com.preston.codex` |

## Architecture

Layers: `domain` / `data` / `importer` / `android-integration` / `ui` (see `Preston-Codex.md` section 14.3 and `docs/phase3_plan.md`).

- `domain/` and `importer/` are pure Java — zero `android.*` imports (JVM-testable).
- No business logic in Activities, Fragments, or widget providers.
- All DB multi-record operations run in transactions.
- Time is injected via `java.time.Clock`; no `System.currentTimeMillis()`/`LocalDate.now()` in domain code.
- Room schema exported to `app/schemas/` and tracked in version control; no `fallbackToDestructiveMigration`.

## Build & test

```bash
# Unit tests (debug + release variants)
./gradlew test

# Debug variant only
./gradlew testDebugUnitTest

# Release variant only
./gradlew testReleaseUnitTest

# Debug APK
./gradlew assembleDebug
```

CI runs `./gradlew test`, asserts the merged RELEASE manifest contains no `android.permission.INTERNET`, and builds `assembleDebug`.

### Test results (verified)

`./gradlew test` — **217 tests across 34 classes**, passing in both `testDebugUnitTest` and `testReleaseUnitTest`.

Key test classes:
- `NextRunCalculatorTest` (8) — includes `America/Havana` DST midnight-jump case
- `MarkdownParserTest` (22), `ImportCommitterTest` (38), `EntryMatcherTest` (16)
- `ReviewSchedulerTest` (17), `DailyInsightEngineTest` (15)
- `WidgetRemoteViewsApplyTest` (4) — `RemoteViews.apply` on Small/Medium/Large/empty state
- `InsightNotificationWorkerTest`, `ReviewNotificationWorkerTest` — notification posting on fresh install
- `NotificationSchedulerWorkTestingTest` — self-rescheduling chain survives 3+ runs, never cancels itself
- `DebugDevPanelTest` (3) — `src/testDebug` only

## Design

Approved screen designs live in `design/` (Stitch HTML/CSS web reference — never copied into the app, no WebView). Design tokens are extracted into `res/values/` (`colors.xml`, `themes.xml`, `styles.xml`, `dimens.xml`) with light/dark (`values-night`) variants; all text in `strings.xml`.

Only screens that exist in `design/` are built. Deviations (e.g. widget RemoteViews limitations) are listed in `docs/phase3_plan.md`.

## Key locked decisions

- Import identity: explicit ID field > SHA-256 fingerprint > fuzzy Jaccard ≥ 0.85; never auto-merge; similar quotes from different sources are never merged.
- Daily insight: one canonical row per local date; no consecutive-day repeats when ≥ 2 eligible entries.
- Spaced repetition: deterministic interval ladder (1, 3, 7, 14, 30, 60, 120 days).
- Deleted entries: recoverable trash for 30 days, then permanent deletion. Archive is distinct from delete.
- No `INTERNET` permission — enforced by CI grep of the merged release manifest.

## Spec

Full product requirements: `Preston-Codex.md` (repo root, source of truth).
