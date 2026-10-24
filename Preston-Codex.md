# Preston Codex — Personal Planner & Knowledge Retention System
## Master Product Requirements and Technical Specification

**Document type:** Product and Engineering Specification  
**Status:** Functional specification — pre-implementation  
**Platform:** Native Android  
**Primary language:** Java  
**Database:** Room / SQLite  
**Build and distribution:** Gradle + GitHub Actions  
**Operating principle:** Functionality first, data integrity always, UI/UX after functional requirements are locked.

---

# 1. Product Overview

## 1.1 Vision

Build an offline-first Android application that combines personal scheduling, task management, knowledge retention, and practical application of learned information.

The application will help its user:

- Organize responsibilities across specific dates and recurring schedules.
- Handle incomplete tasks without losing deadlines or creating an unmanageable backlog.
- Capture valuable information from books, articles, personal notes, and other learning resources.
- Import and incrementally update knowledge using structured Markdown files.
- Automatically cross-reference imported knowledge against existing records without silently overwriting or duplicating entries.
- Display a selected insight each day through the application and Android home-screen widget.
- Reinforce learning through active recall and spaced repetition.
- Turn knowledge into actionable tasks and track its application.
- Preserve, back up, export, restore, and manage accumulated knowledge safely.

The application is not merely a to-do list, notes application, or quote collection. It is a personal system for planning, remembering, connecting, and applying information.

## 1.2 Core principles

1. **Offline-first:** Core functionality must work without an internet connection.
2. **Non-destructive imports:** Existing knowledge must never be silently replaced by an import.
3. **User control:** The user decides how conflicts, overdue tasks, and destructive operations are handled.
4. **Traceability:** Imported knowledge retains its source and origin.
5. **Consistency:** The widget and application display the same underlying records.
6. **Recoverability:** Important data must be exportable and recoverable from backups.
7. **Testability:** Business rules must be independently testable.
8. **Maintainability:** Separate application logic, persistence, scheduling, parsing, and Android integration.
9. **No unnecessary complexity:** Do not introduce a backend, AI dependency, or Rust module without a demonstrated requirement.
10. **Functional completeness before visual design:** Final screen designs and styling begin only after functionality and acceptance criteria are locked.

# 2. Functional Scope

The application consists of the following interconnected subsystems:

1. Task and schedule management.
2. Knowledge library and source management.
3. Markdown import and synchronization engine.
4. Duplicate detection and conflict resolution.
5. Daily insight selection and presentation.
6. Active recall and spaced repetition.
7. Knowledge-to-action tracking.
8. Android home-screen widget.
9. Notifications and scheduled background work.
10. Search, filtering, and knowledge relationships.
11. Backup, restoration, import history, and export.
12. Settings, privacy, and data management.
13. Automated testing and APK delivery.

These are functional modules, not a prescribed UI layout.

# 3. Task and Schedule Management

## 3.1 Task creation

Users must be able to create tasks with:

- Title and optional description.
- Scheduled date and optional time.
- Priority.
- Optional deadline.
- Estimated duration.
- Tags or categories.
- Recurrence rules.
- Flexibility classification.
- Completion status.
- Optional links to knowledge entries.
- Creation and modification timestamps.

Tasks can be created for today, a future date, or a date selected from the calendar. Users must also be able to create an unscheduled task and assign its date later.

## 3.2 Task states

The system should distinguish between:

- Unscheduled.
- Scheduled.
- In progress.
- Completed.
- Postponed.
- Overdue.
- Cancelled.

These states must be implemented consistently and must not rely solely on how a task appears in the interface.

Completion should be reversible, with the original scheduling history retained where practical.

## 3.3 Recurring tasks

Support recurring schedules such as:

- Daily.
- Selected days of the week.
- Weekly.
- Monthly.
- Custom intervals.

Recurrence definitions must be stored separately from individual task occurrences.

Each occurrence must have its own completion status. Completing Monday's occurrence must not mark Tuesday's occurrence complete.

Changing a recurrence rule must not unexpectedly rewrite historical occurrences. The system must define whether edits affect future occurrences only or the entire series, and make the chosen scope explicit.

## 3.4 Incomplete tasks and rescheduling

The application must never silently move a task's deadline merely because it was not completed.

Rules:

- Ordinary tasks can be postponed manually.
- Flexible tasks may be automatically rescheduled according to configured rules.
- Deadline-bound tasks remain overdue until explicitly rescheduled or completed.
- Recurring schedules remain intact when an occurrence is missed.
- Missed recurring occurrences must be handled according to the user's selected policy.
- Postponed tasks must retain their previous scheduled date and rescheduling history.
- Tasks should be reviewable in a daily backlog-management process.

Automatic rescheduling must consider the target date, priority, deadline, and existing schedule. The application must not indefinitely postpone tasks without exposing them to the user.

## 3.5 Calendar and history

The task engine must support:

- Daily and weekly task views.
- Date-based task retrieval.
- Upcoming deadlines.
- Overdue task identification.
- Completed task history.
- Postponement history.
- Recurring occurrence history.

All date calculations must use the device's local time zone appropriately, with careful handling of daylight-saving changes where applicable.

# 4. Knowledge Library

## 4.1 Purpose

The knowledge library stores valuable information from books and other sources so it can be searched, reviewed, remembered, and applied later.

Knowledge must be independent of the daily insight currently displayed. Changing the daily insight must never modify or delete the underlying record.

## 4.2 Supported knowledge types

The data model should support:

- Quotes.
- Insights and interpretations.
- Principles.
- Definitions.
- Frameworks.
- Personal notes.
- Practical lessons.
- Actionable recommendations.

The first version must fully support the user's specified quote-and-insight Markdown format. Additional knowledge types can use optional fields or explicitly versioned extensions.

## 4.3 Knowledge metadata

Each knowledge entry should support:

- Permanent internal ID.
- Optional stable ID from Markdown.
- Quote or primary content.
- Insight or takeaway.
- Source book or resource.
- Author.
- Chapter, section, or page reference.
- Tags.
- Optional application instructions.
- Optional related-entry references.
- Creation and modification timestamps.
- Import provenance.
- Bookmark status.
- Archive status.
- Review history.
- Optional original Markdown content or source reference.

Not every field must be populated. Missing optional fields must not prevent a valid entry from being imported.

## 4.4 Source management

Books and other resources should be represented separately from their individual insights.

A source record should support a title, author, source type, optional publication metadata, and associated knowledge entries.

This allows multiple insights to belong to the same book without repeating inconsistent source metadata in the database.

When entries refer to different books, they must retain their distinct source attribution even if their quotes or ideas are similar.

## 4.5 Knowledge operations

The application must support:

- Create.
- Read.
- Edit.
- Search.
- Filter by source, type, or tag.
- Bookmark and unbookmark.
- Archive and restore.
- Delete individual entries.
- Delete an entire source and handle its associated entries explicitly.
- Export.
- Review history.
- Connect related insights.

The system must not treat removal from a Markdown file as an instruction to delete a database entry.

# 5. Markdown Import and Synchronization Engine

This subsystem is a critical component of the application.

## 5.1 Supported initial format

The initial importer must support UTF-8 Markdown files structured as follows:

```markdown
# Book: Deep Work
# Author: Cal Newport

---
**Quote:** "If you don't produce, you won't thrive—no matter how skilled or talented you are."
**Insight:** High-value work requires uninterrupted focus to master complex tasks quickly.
**Tags:** focus, productivity, work
**Source:** Chapter 1

---
**Quote:** "Clarity about what matters provides clarity about what does not."
**Insight:** Eliminating low-value shallow tasks frees cognitive capacity.
**Tags:** focus, prioritization
**Source:** Chapter 3
```

The parser must recognize the book metadata and extract individual entries separated by Markdown horizontal rules.

The parser must handle blank lines, Unicode, Windows and Unix line endings, escaped characters, and permitted variations in whitespace.

Unknown fields must be handled according to a documented extensibility policy rather than causing silent data loss.

Malformed entries must produce actionable errors identifying the source file and entry location.

## 5.2 Import workflow

Every import must follow these stages:

1. File selection.
2. File reading and validation.
3. Markdown parsing.
4. Metadata normalization.
5. Entry fingerprint generation.
6. Cross-reference against existing sources and entries.
7. Duplicate and conflict classification.
8. Import preview and validation report.
9. User confirmation when necessary.
10. Transactional database insertion of approved new records.
11. Import-history recording.
12. Completion report.

The importer must not require an internet connection.

## 5.3 Incremental imports

Importing a previously imported file must not replace the library with the contents of that file.

Example:

- Initial file contains two insights.
- The user adds three new insights to the same file.
- The user imports the updated file.
- The importer identifies the two existing entries and the three new entries.
- The final library contains five entries.

Reimporting the unchanged file must not create duplicates.

The importer must compare individual entries rather than comparing entire files or relying exclusively on file hashes.

## 5.4 Stable identity and fingerprints

Every stored entry must have a permanent database ID.

The Markdown format should optionally support a stable `ID` field. When present, that ID provides a strong identity signal for the entry.

For entries without explicit IDs, the importer must use normalized fingerprints and secondary matching rules based on available source metadata and quote content.

Normalization may standardize whitespace, line endings, and insignificant formatting. It must not erase meaningful differences in words or punctuation.

The system must distinguish identity matching from content-change detection.

If an entry with a stable ID has changed, the importer must recognize the existing record and flag the difference for review rather than automatically replacing it.

If no stable ID exists, a substantially changed quote may be indistinguishable from a new entry. In that case, the importer should flag likely matches instead of guessing.

## 5.5 Import classification

Each parsed entry must be classified as one of the following:

- **New:** No credible existing match; eligible for insertion.
- **Unchanged:** Existing record matches; no action required.
- **Duplicate:** Another entry already represents the same knowledge item.
- **Potential duplicate:** Similar enough to require review.
- **Conflict:** A stable identity matches an existing entry, but its content or metadata differs.
- **Invalid:** The entry cannot be parsed or fails required validation.
- **Skipped:** The user or import policy explicitly excludes the entry.

The import preview must summarize counts for these categories.

## 5.6 Conflict handling

Existing knowledge must never be silently overwritten.

For conflicting entries, the application must offer a review process that allows the user to:

- Keep the existing entry.
- Apply selected changes explicitly.
- Save the incoming content as a separate entry if it is genuinely different.
- Skip the incoming entry.

Review history and bookmarks must be preserved when an existing record is updated.

## 5.7 Transaction safety

A batch import must be validated before committing approved changes.

Database operations should run transactionally. If a critical insertion or persistence operation fails, the transaction must roll back instead of leaving the library in an inconsistent state.

A rejected entry should not invalidate unrelated entries when the import policy permits safe partial acceptance. The preview must make exclusions and validation errors explicit.

The application must report what was actually committed, not merely what was parsed.

## 5.8 Import history

Record:

- Import date and time.
- Original filename.
- Source identity where available.
- Content hash for diagnostics.
- Number of entries parsed.
- Number of new entries inserted.
- Number of unchanged entries.
- Duplicate and conflict counts.
- Validation errors.
- Final import outcome.

Import history should support diagnostics and recovery, but it must not become an alternative source of truth that silently overwrites current database records.

# 6. Optional Markdown Extensions

The original format must remain valid without modification.

Optional extensions may include:

```markdown
---
**ID:** deep-work-ch1-focus
**Quote:** Example quotation from the book.
**Insight:** Explanation of the principle.
**Tags:** focus, productivity
**Source:** Chapter 1
**Application:** Complete one uninterrupted study session.
**Type:** principle
**Review:** spaced
**Related:** another-insight-id
---
```

Supported optional fields may include:

- `ID`
- `Application`
- `Type`
- `Review`
- `Related`
- `Created`
- `Updated`

The parser must define behavior for each supported field, including invalid values and unknown fields.

These extensions must be introduced through a documented schema version or compatibility policy. Existing Markdown files must remain importable.

## 6.1 Markdown remains user-editable

Markdown is the portable, human-readable interchange format.

Room/SQLite is the operational database used by the application.

The two must not be treated as synchronized replicas that overwrite one another automatically. Import is additive by default, and export generates files from selected database records.

Changes made to a Markdown file are applied to an existing entry only through explicit conflict resolution or a deliberate update operation.

# 7. Daily Insight Selection Engine

## 7.1 Purpose

The selection engine determines which stored insight should appear as the daily insight.

It must operate locally and select from the knowledge library, subject to configured eligibility rules.

## 7.2 Daily selection

The engine must:

- Select an eligible entry from the library.
- Persist the selection for the current local date.
- Keep the selection stable across app restarts and widget refreshes.
- Avoid consecutive-day repetition when enough eligible entries exist.
- Handle an empty library gracefully.
- Handle a library containing only one eligible entry.
- Avoid displaying archived or otherwise ineligible entries.
- Retain the original source attribution and takeaway.

A daily selection record should include the local date, selected insight ID, and relevant selection metadata.

A date change should not be inferred from a background timer alone. The application must recalculate eligibility when opened or refreshed and respond appropriately to date changes.

## 7.3 Shuffle

Shuffle must:

- Select another eligible entry when alternatives exist.
- Avoid selecting the currently displayed entry where possible.
- Persist the new selection.
- Preserve the original daily selection history.
- Avoid changing the source record or its content.
- Remain functional offline.

The product specification should distinguish a temporary shuffle from the canonical daily selection. The implementation must settle on one documented behavior before release so the app and widget remain consistent.

## 7.4 Bookmarks

Bookmark status must be stored persistently in the database.

Users must be able to:

- Bookmark an insight.
- Remove a bookmark.
- View all bookmarked entries.
- Open a bookmarked entry from the widget or library.
- Retain bookmarks across app restarts and widget refreshes.

Bookmarking an insight must not alter its import identity or content fingerprint.

# 8. Active Recall and Spaced Repetition

## 8.1 Active recall

The application should support reviewing the meaning of an insight before revealing its saved explanation.

A review session may display the quote or prompt first, then reveal the takeaway and ask the user to assess their recall.

This feature must be optional. The daily widget can continue to display the full quote and takeaway according to the user's chosen presentation mode.

## 8.2 Review ratings

Support four initial ratings:

- Forgot.
- Difficult.
- Remembered.
- Easy.

Each review record should retain the entry ID, review timestamp, rating, previous review state, and resulting next-review date.

## 8.3 Review scheduling

Implement a deterministic spaced-repetition algorithm with configurable intervals.

The initial algorithm should:

- Schedule forgotten items sooner.
- Repeat difficult items sooner than well-remembered items.
- Increase intervals for consistently remembered items.
- Respect a configurable daily review limit.
- Handle overdue reviews.
- Preserve review history when scheduling parameters change.

The algorithm must be covered by unit tests. Exact interval values and adjustment rules must be documented before implementation.

The daily insight and the review queue are related but separate systems. The daily insight can come from the wider library, while the review queue prioritizes items due for review.

# 9. Knowledge Application Engine

The application must help users turn knowledge into concrete action.

Each insight may include an application instruction.

Users must be able to create a task linked to a knowledge entry, mark the action completed, and optionally record a short reflection.

The relationship must remain intact if the task is postponed or completed.

For example:

- Insight: Protect uninterrupted focus.
- Application: Complete one focused study session.
- Scheduled task: Study for a defined period.
- Reflection: Record whether the principle was successfully applied.

The system must not automatically invent or execute actions based on an insight. The user must confirm the action and its schedule.

Knowledge relationships should also support connections between related insights from different sources. The initial implementation can use explicit links and tags without requiring AI or semantic-search infrastructure.

# 10. Android Home-Screen Widget

## 10.1 Technology

Implement the widget using native Android `AppWidgetProvider`, XML layouts, and `RemoteViews` in Java, following the supported Android APIs.

Use Room as the underlying data store and Android-compatible scheduling mechanisms for refreshes.

## 10.2 Widget content

The intended design contains:

- Heading: INSIGHT OF THE DAY.
- Current local date.
- Quote.
- Author and source attribution.
- TAKEAWAY heading.
- Insight text.
- Shuffle action.
- Bookmark action.

An optional larger widget can include today's task progress and a quick action to open the task list.

The final visual layout is deliberately deferred until functional requirements are complete.

## 10.3 Widget behavior

The widget must:

- Display the persisted daily insight.
- Update immediately following a supported user action.
- Persist shuffle and bookmark actions.
- Open the relevant insight when tapped.
- Handle missing or deleted records gracefully.
- Display a helpful empty state when no knowledge exists.
- Remain useful offline.
- Recover after device restart and widget recreation.
- Respect Android background-execution and battery restrictions.

The implementation must account for widget lifecycle callbacks and avoid relying on a continuously running background service.

# 11. Notifications and Background Work

The notification subsystem should support:

- Task reminders.
- Deadline reminders.
- Daily insight availability.
- Scheduled knowledge reviews.
- Optional review-session reminders.

Users must be able to configure reminder preferences and disable individual notification categories.

Notifications must respect Android notification permissions and applicable operating-system restrictions.

Use WorkManager for deferrable background work and periodic refreshes. Use AlarmManager only when the timing requirements justify it and the relevant Android restrictions are handled.

Do not create duplicate notifications when work is rescheduled or the device restarts.

Notification processing must be idempotent: repeating the same scheduled operation must not generate duplicate reminders for the same event.

# 12. Search and Retrieval

The application must provide local search across:

- Quotes.
- Insight text.
- Book titles.
- Authors.
- Source references.
- Tags.
- Application instructions.

Filtering should support source, tag, knowledge type, bookmark status, archive status, and review status.

Initial search can use SQLite queries and appropriate indexes. SQLite full-text search can be introduced if ordinary queries become insufficient.

Search results must retain their source context and open the corresponding knowledge entry.

Related-entry functionality should initially support explicit relationships and tag-based discovery. Advanced semantic search is outside the initial release.

# 13. Data Management and Recovery

## 13.1 Backup

Provide a way to create a backup containing the information necessary to restore the user's library and application state.

Depending on the chosen backup format, this may include:

- Sources.
- Knowledge entries.
- Tags and relationships.
- Bookmarks.
- Review history.
- Daily insight history.
- Tasks and recurrence definitions.
- Relevant settings.

The backup format must be versioned and validated.

## 13.2 Restore

Before restoration, validate the backup and report its contents.

Restoration must have a clearly documented behavior: either replace the selected application dataset after confirmation or merge compatible records using stable identities. It must never silently merge incompatible records or discard current data.

If restoration fails, preserve the original database.

## 13.3 Export

Support exporting selected or all knowledge entries to Markdown using the documented format.

Exports should preserve source metadata, stable IDs, tags, and supported optional fields.

The user should be able to move the library to another installation without depending on an account or proprietary cloud format.

## 13.4 Deletion

Provide distinct operations for:

- Deleting an individual entry.
- Deleting a source and explicitly handling its associated entries.
- Clearing the entire knowledge library.
- Resetting all application data.

Clearing knowledge must not accidentally delete tasks, schedules, or settings.

Bulk deletion requires explicit confirmation. A recovery mechanism should support undo or restore where practical.

Permanent deletion must be clearly distinguished from archiving.

# 14. Functional Architecture

## 14.1 Primary stack

- Java for application logic.
- Android SDK for native platform functionality.
- XML layouts and Material Components for the future UI layer.
- Gradle for builds.
- Room/SQLite for persistence.
- WorkManager and, where appropriate, AlarmManager for scheduled operations.
- `AppWidgetProvider` and `RemoteViews` for the widget.
- A Java-compatible Markdown parser for structured Markdown processing.
- GitHub Actions for continuous integration and APK generation.

## 14.2 Rust policy

Rust is not part of the initial implementation.

Java and SQLite are sufficient for the expected workload of personal task management, Markdown parsing, search, review scheduling, and widget updates.

Rust may be reconsidered only after profiling identifies a measurable performance problem that cannot reasonably be addressed through Java or database optimization.

## 14.3 Layered architecture

The application should separate the following responsibilities:

**Domain layer**
- Task rules.
- Recurrence rules.
- Rescheduling policies.
- Knowledge identity and matching.
- Review scheduling.
- Daily insight selection.

**Data layer**
- Room entities.
- DAOs.
- Repositories.
- Database migrations.
- Backup and restore.
- Import-history persistence.

**Import layer**
- File access.
- Markdown parsing.
- Normalization.
- Fingerprinting.
- Cross-referencing.
- Conflict classification.
- Import previews and transactions.

**Android integration layer**
- Notifications.
- Widget updates.
- Background work.
- System date and time changes.
- File selection and permissions.

**Presentation layer**
- Screens.
- Navigation.
- User interaction.
- Rendering of insight content.
- Validation feedback.

The domain and parsing logic should be testable without launching Android screens.

## 14.4 Suggested project structure

```text
personal-planner/
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/com/example/planner/
│       │   │   ├── domain/
│       │   │   │   ├── tasks/
│       │   │   │   ├── knowledge/
│       │   │   │   ├── recall/
│       │   │   │   └── scheduling/
│       │   │   ├── data/
│       │   │   │   ├── database/
│       │   │   │   ├── dao/
│       │   │   │   ├── repository/
│       │   │   │   └── backup/
│       │   │   ├── importer/
│       │   │   ├── notifications/
│       │   │   ├── widget/
│       │   │   ├── ui/
│       │   │   └── MainActivity.java
│       │   └── res/
│       └── test/
├── .github/
│   └── workflows/
│       └── android.yml
├── gradle/
├── build.gradle
└── settings.gradle
```

This is a starting organization, not a requirement to place all functionality in a single Android module. Additional modules may be introduced if they improve testability or maintainability.

# 15. Data Integrity and Database Rules

The database design must support:

- Stable primary keys.
- Foreign-key relationships where appropriate.
- Uniqueness constraints for stable external IDs within their intended scope.
- Indexes for frequently queried fields.
- Transactional multi-record operations.
- Schema migrations.
- Consistent timestamps.
- Explicit archive and deletion states.
- Persistent review and import histories.

At minimum, the domain model should include:

- `Task`
- `RecurrenceRule`
- `TaskOccurrence`
- `KnowledgeSource`
- `KnowledgeEntry`
- `KnowledgeTag`
- `KnowledgeRelationship`
- `Bookmark` or an equivalent persisted bookmark field
- `ReviewRecord`
- `ReviewSchedule`
- `DailyInsightSelection`
- `ImportBatch`
- `ImportResult`
- `AppSettings`

The final schema must be normalized enough to prevent inconsistent source metadata and duplicate relationships.

Deletion rules must be explicit. A database migration must never silently erase the knowledge library.

# 16. Security and Privacy

The application must:

- Operate without mandatory accounts.
- Avoid sending knowledge to external services.
- Avoid unnecessary storage permissions.
- Use the Android Storage Access Framework for user-selected files.
- Keep imported documents within the selected access scope.
- Validate files before parsing.
- Avoid executing content from imported Markdown.
- Treat Markdown as data, not executable instructions.
- Protect backups from accidental disclosure.
- Avoid logging complete private notes or book excerpts unnecessarily.

If encrypted backups are introduced, their encryption and key-recovery behavior must be specified separately.

# 17. Testing Strategy

## 17.1 Markdown parser tests

Verify:

- Valid files.
- Missing optional fields.
- Missing required fields.
- Multiple entries.
- Empty entries.
- Unicode content.
- Escaped quotation marks.
- Different line endings.
- Additional supported metadata.
- Unknown fields.
- Malformed separators.
- Large files.

## 17.2 Import and synchronization tests

Verify:

- First import inserts the expected entries.
- Reimporting an unchanged file inserts zero duplicates.
- Adding new entries inserts only the new records.
- Editing an entry with a stable ID produces a conflict.
- Existing bookmarks and review histories remain intact.
- Similar quotes from different sources are not incorrectly merged.
- Potential duplicates are flagged appropriately.
- Invalid files do not corrupt the database.
- Failed transactions roll back.
- Import reports match the actual committed changes.

## 17.3 Scheduling tests

Verify:

- Daily and weekly recurrence.
- Missed recurring occurrences.
- Postponement behavior.
- Deadline preservation.
- Completion and reopening.
- Time-zone and date transitions.
- Review intervals.
- Due-review ordering.
- Stable daily insight selection.

## 17.4 Widget tests

Verify:

- Correct insight rendering from persisted data.
- Shuffle persistence.
- Bookmark persistence.
- Empty-library behavior.
- Missing or archived entry handling.
- Refresh behavior.
- Device restart recovery.
- Multiple widget sizes where supported.

## 17.5 Data safety tests

Verify:

- Backup creation.
- Backup validation.
- Successful restoration.
- Restoration failure recovery.
- Export and reimport compatibility.
- Database migration without data loss.
- Bulk deletion isolation.
- Undo or recovery behavior.

# 18. GitHub Actions and APK Delivery

## 18.1 Continuous integration

The workflow should:

1. Check out the repository.
2. Configure the selected JDK.
3. Configure Gradle and its cache.
4. Run static checks and unit tests.
5. Build the debug APK.
6. Run available Android integration tests.
7. Upload the APK artifact when the build succeeds.

The workflow should run on pull requests and pushes to the relevant branches.

## 18.2 Release builds

A separate release workflow should:

- Run the required test suite.
- Build the release variant.
- Sign the release artifact securely.
- Store signing credentials in GitHub Secrets or an equivalent protected secret store.
- Upload the release APK as a downloadable artifact.

A Play Store release can be considered later. It is not required for the initial personal-use distribution.

## 18.3 Reproducibility

Pin compatible toolchain versions, use the Gradle wrapper, and document the build environment.

The build should not depend on uncommitted local files or manually performed steps.

# 19. Functional Acceptance Criteria

The initial release is not complete until all critical criteria pass.

### Knowledge import

- Importing a valid Markdown file creates the expected entries.
- Importing the same file again creates no duplicates.
- Adding new entries and reimporting adds only new records.
- Existing entries are never silently overwritten.
- Conflicts are identifiable and resolvable.
- Failed imports cannot corrupt existing data.
- Import history accurately reflects the outcome.

### Knowledge management

- Users can search, filter, bookmark, archive, edit, and delete entries.
- Sources and relationships remain consistent.
- Knowledge can be exported and restored.
- Bulk deletion does not unintentionally delete tasks or settings.

### Daily insight and recall

- A valid insight can be selected from the library.
- Daily selection remains stable until its defined selection policy changes.
- Shuffle changes the selection without altering source content.
- Bookmarking persists across restarts.
- Review scheduling responds consistently to user ratings.
- Related tasks preserve their link to the originating insight.

### Planner

- Tasks can be scheduled and completed.
- Recurrence rules generate independent occurrences.
- Incomplete tasks follow the configured postponement policy.
- Deadlines are not silently changed.
- History is retained as specified.

### Widget and delivery

- The widget displays persisted knowledge and supports its defined actions.
- The application works offline.
- GitHub Actions produces a working debug APK.
- Critical tests run automatically.
- Database migrations preserve existing data.

# 20. Development Phases

## Phase 1 — Specification and technical foundation

- Finalize functional requirements.
- Define domain entities and database relationships.
- Lock Markdown compatibility rules.
- Define import identity and conflict policies.
- Define task and recurrence behavior.
- Select compatible Android and Gradle versions.
- Establish the repository and automated build.

**Exit criterion:** Technical decisions and functional acceptance criteria are documented.

## Phase 2 — Persistence and knowledge engine

- Implement Room entities and migrations.
- Implement the Markdown parser.
- Implement normalization and identity matching.
- Implement incremental import.
- Implement import previews and transactional persistence.
- Implement import history and recovery behavior.
- Write comprehensive tests.

**Exit criterion:** Repeated and incremental imports behave correctly under automated tests.

## Phase 3 — Task and scheduling engine

- Implement tasks and task occurrences.
- Implement recurrence.
- Implement deadlines and rescheduling policies.
- Implement notifications.
- Test scheduling edge cases.

**Exit criterion:** Scheduling rules work independently of the interface.

## Phase 4 — Recall and insight engine

- Implement daily selection.
- Implement shuffle persistence.
- Implement bookmarks.
- Implement review scheduling.
- Implement knowledge relationships.
- Implement knowledge-linked actions.

**Exit criterion:** Knowledge can be selected, reviewed, and connected to tasks consistently.

## Phase 5 — Functional Android integration

- Integrate the Android widget.
- Integrate file selection.
- Integrate notification and background-work mechanisms.
- Implement backup, restoration, and export.
- Validate offline operation.

**Exit criterion:** All critical functions are accessible and persistent on a real Android device.

## Phase 6 — UI/UX design and implementation

Begin the detailed visual design only after the functional system is locked.

Design every screen, navigation flow, insight layout, import preview, error state, empty state, confirmation flow, and widget size.

Implement the approved designs in Java and XML, without embedding business rules inside the presentation layer.

**Exit criterion:** The implemented interface satisfies the established functional and visual requirements.

## Phase 7 — Hardening and release

- Complete regression testing.
- Test database upgrades and recovery.
- Test battery and notification behavior.
- Test import and export compatibility.
- Build and verify signed APKs.
- Document installation and recovery procedures.

**Exit criterion:** The application meets its acceptance criteria and can be built and installed reliably.

# 21. Explicitly Out of Scope for the Initial Release

The following are deferred unless a demonstrated need changes the scope:

- Mandatory cloud services or user accounts.
- AI-generated summaries or automatic interpretation.
- Automatic rewriting of knowledge during import.
- Rust integration without profiling evidence.
- Complex analytics dashboards.
- Advanced semantic search.
- Automatic execution of tasks based on insights.
- Full ebook-management functionality beyond the selected import formats.
- Cross-device synchronization.

These exclusions protect the core objective: a dependable, private, offline-first system that preserves and surfaces useful knowledge.

# 22. Definition of Success

The application succeeds when the user can reliably:

1. Plan responsibilities across dates and recurring schedules.
2. Capture knowledge in editable Markdown files.
3. Import updated files repeatedly without duplicating or overwriting existing entries.
4. Find stored information through search and source references.
5. See a meaningful daily insight in the app and widget.
6. Review difficult knowledge more frequently than well-remembered knowledge.
7. Turn selected principles into practical actions.
8. Back up, export, restore, and deliberately delete data.
9. Build and install the application through the documented GitHub Actions workflow.

The central measure of success is not the number of features or the amount of knowledge collected. It is whether the system reliably helps the user plan, remember, retrieve, and apply valuable information over time.

**Final engineering principle:** Establish correct data behavior first, test the underlying rules independently, and design the visual experience around the completed functional system—not the other way around.
