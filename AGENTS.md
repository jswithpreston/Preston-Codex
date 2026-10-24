# PROJECT: Preston Codex (native Android)

You are a senior Android engineer building "Preston Codex": an offline-first
personal planner + knowledge-retention app. The full product spec is in
Preston-Codex.md (repo root). It is the source of truth. If this file and the
spec conflict, stop and ask me. Never silently reinterpret requirements.

## Stack (fixed)
- Native Android, Java 17, XML + Material Components (UI comes last).
- Room/SQLite, WorkManager, AppWidgetProvider + RemoteViews.
- Gradle with wrapper, version catalog (libs.versions.toml), pinned versions.
  minSdk 26, compileSdk/targetSdk = current stable. Verify the latest stable
  AGP/Gradle/Room/WorkManager/Material versions rather than guessing.
- JUnit4 + Mockito for unit tests, Room in-memory DB for DAO tests,
  Robolectric only where Android classes are unavoidable.
- No backend, no AI/network dependency, no Rust, no Kotlin, no analytics.
- Package: com.preston.codex

## Architecture rules
- Layers: domain / data / importer / android-integration / ui (see spec 14.3).
- domain/ and importer/ MUST NOT import android.* (pure Java, JVM-testable).
- No business rules in Activities, Fragments, or widget providers.
- All DB multi-record operations run in transactions.
- Time: inject a Clock; never call System.currentTimeMillis()/LocalDate.now()
  directly in domain code. Use java.time (desugaring enabled).
- Never log quotes, notes, or other private content.

## Design source of truth (UI)
- Approved screen designs are in the design/ folder. Each subfolder is one
  screen or flow (today, library, review, tasks, more, import_1_4_pick_file,
  import_2_4_preview, import_3_4_conflict_review, import_4_4_completion_report,
  import_history, widgets_light_theme). Folders such as
  stitch_preston_codex_app_design and warm_editorial_scholar hold the overall
  design system/theme. Each contains images and/or Stitch HTML/CSS.
- The Stitch code is a WEB reference (HTML/CSS). Never copy it into the app and
  never use a WebView. Translate each design into native Android: XML layouts,
  Material Components, drawables, and values/ resources.
- Extract design tokens (colors, typography, spacing, corner radius) into
  res/values/colors.xml, themes.xml, styles.xml, dimens.xml, with light and
  dark variants (values-night). Never hardcode colors, sizes, or strings in
  layouts. All text goes in strings.xml.
- Match the designs for layout, hierarchy, spacing, and states. Where a design
  cannot be built natively as drawn (especially widgets, which are limited to
  RemoteViews-compatible views), adapt it, keep the intent, and list the
  deviation in your report.
- Only build screens that exist in design/. If a screen needs behavior the data
  layer does not support, or a screen is missing, stop and ask. Do not invent
  screens or features.
- UI code contains no business logic. Screens bind to existing
  repositories/ViewModels only.
- Designs never override the data-integrity rules in this file or the spec.
  If a design conflicts with the spec, the spec wins and you flag it.

## Locked decisions (change only with my approval)
1. Import identity: explicit ID field > fingerprint (SHA-256 of normalized
   source title + normalized quote text) > fuzzy match. Normalization: trim,
   collapse whitespace, unify line endings and quote characters; do NOT strip
   words or meaningful punctuation.
2. Potential duplicate = normalized-token Jaccard similarity >= 0.85 and no
   exact/ID match. Flag for review; never auto-merge. Similar quotes from
   DIFFERENT sources are never auto-merged.
3. Unknown Markdown fields are preserved verbatim in an `extraFields` JSON
   column (no data loss) and reported in the preview.
4. Daily insight: one canonical row per local date. Shuffle replaces that
   day's selection and appends to a selection-history table. The app and widget
   read the same row. No consecutive-day repeats when >= 2 eligible entries.
5. Spaced repetition: deterministic, interval-ladder based.
   Forgot -> 1 day, reset step to 0. Difficult -> repeat at max(1, step-1)
   ladder position. Remembered -> advance 1 step. Easy -> advance 2 steps.
   Default ladder (days): 1, 3, 7, 14, 30, 60, 120. Configurable daily
   review limit (default 20). Overdue items sorted by days overdue, then
   lowest step. Full review history kept; changing the ladder never rewrites
   history.
6. Recurrence edits: always ask scope ("this occurrence", "this and future").
   Past occurrences are never rewritten. Each occurrence has its own status.
7. Restore: replace-after-confirmation. Validate the backup, show contents,
   snapshot the current DB first, roll back on failure. Backup format is a
   versioned ZIP (manifest.json with schemaVersion + JSON/Markdown payloads).
8. Deleted entries go to a recoverable "trash" state for 30 days before
   permanent deletion. Archive is distinct from delete.
9. Tasks: auto-reschedule applies only to tasks flagged flexible. Deadline
   tasks stay overdue until the user acts.

## Working method (MANDATORY)
- Work in small phases from the spec (section 20). Do ONLY the phase I name.
- Before coding a phase: produce a short implementation plan and list files you
  will create or change. Wait for my approval.
- Write tests FIRST for parsers, matching, scheduling, and review logic.
- After each phase: run the build and all tests, then give me: what was built,
  test results, deviations from the spec, open questions.
- A phase is done only when ./gradlew test and ./gradlew assembleDebug pass.
- Do not build UI polish, extra features, or "nice-to-haves" outside the spec.
- If a requirement is ambiguous, ask. Do not guess on data-integrity behavior.
- Commit per logical unit with clear messages.
