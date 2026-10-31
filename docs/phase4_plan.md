# Phase 4 Plan (PLAN ONLY - Awaiting Approval)

## 1. Compliance Table (one row per numbered item in docs/PHASE4_DESIGN.md 1-32)

| Item | Requirement | Plan Section | Compliance Notes |
|---|---|---|---|
| 1 | Schema v3 create-only: MIGRATION_2_3 (no alter/drop), MigrationTestHelper from 2.json with populated rows, FK pragma ON, exported 3.json. Room-generated 3.json is authority. Never use destructive migration. | 2.1 Schema v3 Migration | Implement create-only migration, migration test helper from 2.json, export schema to app/schemas/3.json. |
| 2 | No SQL DEFAULT clauses anywhere (Java supplies every value). | 2.1 Schema v3 Migration | All defaults handled explicitly in Java entities/DAOs/services. |
| 3 | Tables: task, task_tag, recurrence_rule, task_occurrence, task_event, reschedule_history, reminder_state. | 2.1 Schema v3 Migration | Define all 7 tables per schema design with Room entities in data/ and plain domain models in domain/. |
| 4 | task includes: title (TEXT NOT NULL), description (nullable), status (TEXT NOT NULL: OPEN \| IN_PROGRESS \| COMPLETED \| CANCELLED), task_kind (ONE_OFF\|SERIES), previous_task_id (nullable self-FK ON DELETE SET NULL), knowledge_entry_id (nullable FK to knowledge_entry ON DELETE SET NULL), reflection, is_flexible, priority (TEXT enum name, sorted in Java), deadline (ISO TEXT date only), estimated_duration_minutes (nullable, null = 30-min default), scheduled_date (ISO TEXT), scheduled_time (ISO TEXT), auto_reschedule_count, completed_at, cancelled_at, timestamps. | 2.1 Schema v3 Migration | Implement Task entity and domain model with all specified fields, types, and FKs. |
| 5 | task_tag(task_id, tag): composite PK, ON DELETE CASCADE, tags lowercase. Do NOT store as column on task. | 2.1 Schema v3 Migration | Separate entity/table for tags; setTags enforces lowercase conversion. |
| 6 | recurrence_rule: type (DAILY, WEEKLY, MONTHLY, CUSTOM_INTERVAL), interval, weekdays (storage format e.g. "MON,WED,FRI"), start_date, end_date, occurrence_count, anchor_day_of_month, missed_policy. | 2.1 Schema v3 Migration, 2.3 Recurrence Materialization | Implement RecurrenceRule entity and model; weekdays formatted as comma-separated 3-letter codes. |
| 7 | task_occurrence: task_id, rule_id, occurrence_date (identity, never changes), effective_date (only postponement/carry-forward changes), status, title_override, time_override, note_override, reflection, auto_reschedule_count, completed_at, cancelled_at, timestamps. Unique (task_id, occurrence_date). Children CASCADE from task. Occurrences have no deadline of their own; deadline reminders apply to one-off tasks. | 2.1 Schema v3 Migration, 2.3 Recurrence Materialization | Implement TaskOccurrence with constraints and index; explicit rule that occurrences do not have deadlines. |
| 8 | reminder_state: reminder_type, owner_type, owner_id, trigger_at, trigger_local_date, trigger_local_time (nullable), channel, status (PENDING, FIRED, CANCELLED, MISSED), fired_at, idempotency_key (UNIQUE), created_at. No FK to owners; cleanup explicit on task delete and tested. Indexes on (owner_type, owner_id), status, trigger_at, and unique on idempotency_key. | 2.1 Schema v3 Migration, 3.1 Reminders | Implement ReminderState entity with specified indexes; TaskService.deleteTask explicitly removes associated reminder_state rows. |
| 9 | Persist only facts. Task status: OPEN, IN_PROGRESS, COMPLETED, CANCELLED. Occurrence: same plus SKIPPED. POSTPONED, OVERDUE, "unscheduled" NEVER stored. | 2.2 State Model & Domain Entities | Enums defined; no stored derived states in database. |
| 10 | TaskStateResolver takes an input object (persisted status, effective or scheduled date, today, hasRescheduleHistory, kind) and returns derived state plus isOverdue flag. POSTPONED = not terminal AND date >= today AND hasRescheduleHistory. IN_PROGRESS with past date is IN_PROGRESS with isOverdue=true. | 2.2 State Model & Domain Entities | Implement pure TaskStateResolver in domain/ with input object and exact state derivation rules. |
| 11 | SERIES rows excluded from one-off queries and derived-state listings; series appears only through occurrences. | 2.1 Schema v3 Migration, 2.2 State Model & Domain Entities | DAO and repository queries filter task_kind = 'ONE_OFF'. |
| 12 | Completion reversible. Events record transitions and original scheduling history kept. | 2.5 Task Service & Operations | TaskService complete/reopen records TaskEvent and preserves reschedule_history. |
| 13 | Materialization 90-day forward window, idempotent, never modifies existing rows. Start at max(rule.start_date, today), NO backfill. Count-limited rules count all existing occurrences (CANCELLED/SKIPPED included). Pure OccurrenceGenerator: generate(rule, today, existingDates, retainedDates, existingCount) -> List<LocalDate>. | 2.3 Recurrence Materialization | OccurrenceGenerator is pure Java with no clock/zone/repo dependencies. |
| 14 | ensureMaterialized(today) only way occurrences created. Called from app open, daily worker, rule create/edit. Read queries never mutate. | 2.3 Recurrence Materialization | RecurrenceService coordinates reading existing/retained dates, calls OccurrenceGenerator, and persists new occurrences. |
| 15 | Weekly intervals use ISO weeks (Monday start). Monthly use anchor_day_of_month, clamped to last day of shorter months (leap years tested). | 2.3 Recurrence Materialization | Pure date arithmetic handling leap years and variable month lengths. |
| 16 | Edit scope explicit: THIS_OCCURRENCE sets override columns only; THIS_AND_FUTURE creates new task row with previous_task_id, ends old rule day before, deletes only untouched future occurrences of old rule (OPEN, no events, no history), new rule generator skips dates old series retained, remaining count adjusted, copies knowledge_entry_id, tags, and is_flexible (not reflection text); past occurrences never rewritten. | 2.4 Recurrence Editing | RecurrenceEditService implements exact branching and copies tags and is_flexible. |
| 17 | Maintenance on app open and daily worker, idempotent, uses effective_date < today (never occurrence_date), exposes recentlyHandled(). KEEP_OVERDUE default (copied into rule at creation). SKIP_AUTOMATICALLY -> SKIPPED with POLICY event. CARRY_FORWARD_FLEXIBLE_ONLY -> flexible only limited by N, set effective_date to today, increment counter, log reschedule_history. Else KEEP_OVERDUE. Carried occurrence not re-handled same day. | 2.6 Missed Occurrences | MissedOccurrenceService with policies and same-day guard. |
| 18 | Manual postponement keeps previous date and appends reschedule_history (from, to, reason, time). Deadline tasks never auto-moved; manual move past deadline allowed but flagged. | 2.7 Postponement | PostponementService with reschedule history logging. |
| 19 | Backlog review explicit user action only, never silent/background. Candidates: flexible AND incomplete AND effective/scheduled date < today. Non-flexible never moved. Flexible with deadline may move but never past deadline; if deadline passed "needs attention" no move. | 2.8 Backlog Review & Planning | BacklogReviewService candidate filtering and qualification rules. |
| 20 | preview() proposes moves, writes nothing; apply(approved) writes in one transaction (dates, counters, reschedule_history, events). apply() re-validates each approved move inside the transaction (still incomplete, caps still hold, deadline still respected), skips stale moves, and returns a report of applied and skipped moves. | 2.8 Backlog Review & Planning | Two-phase review workflow with in-transaction re-validation. |
| 21 | Config constants: 6 tasks/day, 240 min/day, 14-day horizon, N=3, default estimate 30 min (used when estimated_duration_minutes is null). Sort: priority desc, deadline asc (nulls last), days overdue desc, id asc. Planner updates in-memory per-day load after each move, checks caps after adding, doesn't count candidate against old day, counts tasks/occurrences by effective date. Task at N "needs attention" not moved. | 2.8 Backlog Review & Planning | TaskPlanner config and scheduling algorithm. |
| 22 | Task may reference knowledge_entry (nullable ON DELETE SET NULL) plus optional short reflection. Link survives postponement/completion. Deleting entry keeps task and reflection. | 2.9 Knowledge Link | Schema FK behavior and persistence integrity tests. |
| 23 | Reminders: daily digest at 08:00 (default on), timed reminders for tasks with scheduled time (default on, one next-trigger chain worker waking at earliest pending reminder_state), deadline reminders D-1 and D-day at 08:00 (default on). Separate channels and preference keys. | 3.1 Reminders (Milestone 2) | ReminderService and workers (deferred to Milestone 2). |
| 24 | Idempotency keys: digest/deadline = TYPE:OWNER_TYPE:OWNER_ID:LOCAL_DATE; timed = TASK_TIMED:OWNER_TYPE:OWNER_ID:LOCAL_DATETIME. No epoch. | 3.1 Reminders (Milestone 2) | Deterministic key generator. |
| 25 | reminder_state created/updated/cancelled in same transaction as task create, reschedule, complete, cancel, materialization. Completing/cancelling cancels pending reminders. Deleting owner removes its rows. | 3.1 Reminders (Milestone 2) | Transactional lifecycle management. |
| 26 | On timezone/time change recompute trigger_at of pending rows from stored local date/time without changing keys. Never create reminder whose trigger already past. Late worker same local date fires once; later date marks MISSED. Nonexistent local time on spring-forward fires at next valid instant. | 3.1 Reminders (Milestone 2) | DST and timezone adjustment logic. |
| 27 | Self-rescheduling: REPLACE from outside worker, APPEND_OR_REPLACE from inside. No exact alarms. | 3.1 Reminders (Milestone 2) | WorkManager integration pattern. |
| 28 | Debug only (src/debug never release): persisted clock offset, mutable debug clock; buttons to add one-off task, add weekly recurring task, fast-forward one day, run backlog preview, run task-reminder worker now, run deadline-reminder worker now (WorkManager ignores debug clock). Debug tests in src/testDebug. | 3.2 Debug (Milestone 2) | Debug panel extensions (deferred to Milestone 2). |
| 29 | Milestone 1: schema v3, migration, domain engine (resolver, generator, edit scopes, missed policy, backlog planner, services) and tests. Milestone 1 verified by automated tests only; phone testing starts with Milestone 2. STOP and report before reminder code. Milestone 2: reminders, debug additions, remaining tests. | 4. Milestones & Checklists | Strict milestone boundary enforcement. |
| 30 | Test-first. Required coverage: DST days, zone where DST skips midnight, zone change, month ends, leap years, weekday/ISO-week, idempotent materialization, no-backfill, split-series no duplicate dates, carried occurrences not re-handled same day, backlog load updated after each move, deadline boundary cases (deadline == today, deadline == target day), completing Monday leaves Tuesday untouched, migration FK behavior. | 5. Testing Strategy & Test Inventory | Unit and DAO test matrix (>90 tests). |
| 31 | Answers: horizon 90 days forward no backfill; deadline granularity date only; tags use task_tag; daily cap 6 tasks or 240 min; N=3; reminder defaults on as listed. | 6. Confirmed Answers | Locked configuration choices. |
| 32 | Do not commit or push. Report test counts per class, path of 3.json, every deviation from this document. | 7. Reporting Requirements | Output format constraints. |

---

## 2. Milestone 1 Plan (STOP and report before Milestone 2)

Milestone 1 is verified strictly by automated tests (JVM unit tests and in-memory Room DAO / migration tests). Device / phone testing starts in Milestone 2.

### 2.1 Schema v3 Migration
**Files to create/change:**
- `app/src/main/java/com/preston/codex/data/database/AppDatabase.java` - bump database version to 3, register `MIGRATION_2_3`, register new entities
- `app/src/main/java/com/preston/codex/data/database/migration/Migration2To3.java` - create-only migration containing Room 3.json DDL
- `app/src/main/java/com/preston/codex/data/entity/TaskEntity.java`
- `app/src/main/java/com/preston/codex/data/entity/TaskTagEntity.java`
- `app/src/main/java/com/preston/codex/data/entity/RecurrenceRuleEntity.java`
- `app/src/main/java/com/preston/codex/data/entity/TaskOccurrenceEntity.java`
- `app/src/main/java/com/preston/codex/data/entity/TaskEventEntity.java`
- `app/src/main/java/com/preston/codex/data/entity/RescheduleHistoryEntity.java`
- `app/src/main/java/com/preston/codex/data/entity/ReminderStateEntity.java`
- `app/src/main/java/com/preston/codex/data/dao/TaskDao.java`
- `app/src/main/java/com/preston/codex/data/dao/TaskOccurrenceDao.java`
- `app/src/main/java/com/preston/codex/data/dao/RecurrenceRuleDao.java`
- `app/src/main/java/com/preston/codex/data/dao/TaskEventDao.java`
- `app/src/main/java/com/preston/codex/data/dao/RescheduleHistoryDao.java`
- `app/src/main/java/com/preston/codex/data/dao/ReminderStateDao.java`
- `app/src/main/java/com/preston/codex/data/mapper/TaskMapper.java` - bidirectional mapping between data entities and domain models
- `app/schemas/com.preston.codex.data.database.AppDatabase/3.json` - exported Room schema authority
- `app/src/test/java/com/preston/codex/data/Migration2To3Test.java` - migration test from 2.json fixture

**Actual SQL DDL for Migration 2 to 3:**
```sql
-- 1. task table
CREATE TABLE IF NOT EXISTS `task` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `task_kind` TEXT NOT NULL,
    `title` TEXT NOT NULL,
    `description` TEXT,
    `status` TEXT NOT NULL,
    `previous_task_id` INTEGER,
    `knowledge_entry_id` INTEGER,
    `reflection` TEXT,
    `is_flexible` INTEGER NOT NULL,
    `priority` TEXT NOT NULL,
    `deadline` TEXT,
    `estimated_duration_minutes` INTEGER,
    `scheduled_date` TEXT,
    `scheduled_time` TEXT,
    `auto_reschedule_count` INTEGER NOT NULL,
    `completed_at` INTEGER,
    `cancelled_at` INTEGER,
    `created_at` INTEGER NOT NULL,
    `updated_at` INTEGER NOT NULL,
    FOREIGN KEY(`previous_task_id`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL,
    FOREIGN KEY(`knowledge_entry_id`) REFERENCES `knowledge_entry`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS `index_task_previous_task_id` ON `task` (`previous_task_id`);
CREATE INDEX IF NOT EXISTS `index_task_knowledge_entry_id` ON `task` (`knowledge_entry_id`);
CREATE INDEX IF NOT EXISTS `index_task_scheduled_date` ON `task` (`scheduled_date`);
CREATE INDEX IF NOT EXISTS `index_task_deadline` ON `task` (`deadline`);

-- 2. task_tag table
CREATE TABLE IF NOT EXISTS `task_tag` (
    `task_id` INTEGER NOT NULL,
    `tag` TEXT NOT NULL,
    PRIMARY KEY(`task_id`, `tag`),
    FOREIGN KEY(`task_id`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_task_tag_task_id` ON `task_tag` (`task_id`);

-- 3. recurrence_rule table
CREATE TABLE IF NOT EXISTS `recurrence_rule` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `task_id` INTEGER NOT NULL,
    `type` TEXT NOT NULL,
    `interval` INTEGER NOT NULL,
    `weekdays` TEXT,
    `start_date` TEXT NOT NULL,
    `end_date` TEXT,
    `occurrence_count` INTEGER,
    `anchor_day_of_month` INTEGER,
    `missed_policy` TEXT NOT NULL,
    `created_at` INTEGER NOT NULL,
    `updated_at` INTEGER NOT NULL,
    FOREIGN KEY(`task_id`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_recurrence_rule_task_id` ON `recurrence_rule` (`task_id`);

-- 4. task_occurrence table
CREATE TABLE IF NOT EXISTS `task_occurrence` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `task_id` INTEGER NOT NULL,
    `rule_id` INTEGER NOT NULL,
    `occurrence_date` TEXT NOT NULL,
    `effective_date` TEXT NOT NULL,
    `status` TEXT NOT NULL,
    `title_override` TEXT,
    `time_override` TEXT,
    `note_override` TEXT,
    `reflection` TEXT,
    `auto_reschedule_count` INTEGER NOT NULL,
    `completed_at` INTEGER,
    `cancelled_at` INTEGER,
    `created_at` INTEGER NOT NULL,
    `updated_at` INTEGER NOT NULL,
    FOREIGN KEY(`task_id`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
    FOREIGN KEY(`rule_id`) REFERENCES `recurrence_rule`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_task_occurrence_task_id_occurrence_date` ON `task_occurrence` (`task_id`, `occurrence_date`);
CREATE INDEX IF NOT EXISTS `index_task_occurrence_rule_id` ON `task_occurrence` (`rule_id`);
CREATE INDEX IF NOT EXISTS `index_task_occurrence_effective_date` ON `task_occurrence` (`effective_date`);

-- 5. task_event table
CREATE TABLE IF NOT EXISTS `task_event` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `task_id` INTEGER NOT NULL,
    `occurrence_id` INTEGER,
    `event_type` TEXT NOT NULL,
    `details` TEXT,
    `timestamp` INTEGER NOT NULL,
    FOREIGN KEY(`task_id`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_task_event_task_id` ON `task_event` (`task_id`);

-- 6. reschedule_history table
CREATE TABLE IF NOT EXISTS `reschedule_history` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `task_id` INTEGER NOT NULL,
    `occurrence_id` INTEGER,
    `from_date` TEXT,
    `to_date` TEXT NOT NULL,
    `reason` TEXT NOT NULL,
    `timestamp` INTEGER NOT NULL,
    FOREIGN KEY(`task_id`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS `index_reschedule_history_task_id` ON `reschedule_history` (`task_id`);

-- 7. reminder_state table
CREATE TABLE IF NOT EXISTS `reminder_state` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `reminder_type` TEXT NOT NULL,
    `owner_type` TEXT NOT NULL,
    `owner_id` INTEGER NOT NULL,
    `trigger_at` INTEGER NOT NULL,
    `trigger_local_date` TEXT NOT NULL,
    `trigger_local_time` TEXT,
    `channel` TEXT NOT NULL,
    `status` TEXT NOT NULL,
    `fired_at` INTEGER,
    `idempotency_key` TEXT NOT NULL,
    `created_at` INTEGER NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS `index_reminder_state_idempotency_key` ON `reminder_state` (`idempotency_key`);
CREATE INDEX IF NOT EXISTS `index_reminder_state_owner_type_owner_id` ON `reminder_state` (`owner_type`, `owner_id`);
CREATE INDEX IF NOT EXISTS `index_reminder_state_status` ON `reminder_state` (`status`);
CREATE INDEX IF NOT EXISTS `index_reminder_state_trigger_at` ON `reminder_state` (`trigger_at`);
```

**Requirements:**
- Room entities reside in `com.preston.codex.data.entity.*`.
- Plain domain models reside in `com.preston.codex.domain.tasks.*`.
- All dates and times are stored as ISO TEXT strings (`LocalDate.toString()`, `LocalTime.toString()`), matching existing conventions; data mappers convert between ISO TEXT and `java.time` objects.
- `priority` is stored as TEXT (enum name), sorted in Java.
- `estimated_duration_minutes` is nullable (null indicates default of 30 minutes in planning logic).
- No SQL `DEFAULT` clauses anywhere.

---

### 2.2 State Model & Domain Entities
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/model/Task.java` - plain domain model
- `app/src/main/java/com/preston/codex/domain/tasks/model/TaskOccurrence.java` - plain domain model
- `app/src/main/java/com/preston/codex/domain/tasks/model/RecurrenceRule.java` - plain domain model
- `app/src/main/java/com/preston/codex/domain/tasks/model/TaskEvent.java` - plain domain model
- `app/src/main/java/com/preston/codex/domain/tasks/model/RescheduleRecord.java` - plain domain model
- `app/src/main/java/com/preston/codex/domain/tasks/model/ReminderState.java` - plain domain model
- `app/src/main/java/com/preston/codex/domain/tasks/model/TaskKind.java` (`ONE_OFF`, `SERIES`)
- `app/src/main/java/com/preston/codex/domain/tasks/model/TaskStatus.java` (`OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`)
- `app/src/main/java/com/preston/codex/domain/tasks/model/OccurrenceStatus.java` (`OPEN`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`, `SKIPPED`)
- `app/src/main/java/com/preston/codex/domain/tasks/model/TaskPriority.java` (`LOW`, `MEDIUM`, `HIGH`, `URGENT`)
- `app/src/main/java/com/preston/codex/domain/tasks/model/DerivedTaskState.java` (`NO_DATE`, `SCHEDULED`, `POSTPONED`, `IN_PROGRESS`, `COMPLETED`, `CANCELLED`)
- `app/src/main/java/com/preston/codex/domain/tasks/state/TaskStateInput.java` - value object:
  ```java
  public record TaskStateInput(
      TaskStatus status,
      LocalDate date, // scheduledDate or effectiveDate
      LocalDate today,
      boolean hasRescheduleHistory,
      TaskKind kind
  ) {}
  ```
- `app/src/main/java/com/preston/codex/domain/tasks/state/TaskStateResult.java` - value object:
  ```java
  public record TaskStateResult(
      DerivedTaskState state,
      boolean isOverdue
  ) {}
  ```
- `app/src/main/java/com/preston/codex/domain/tasks/state/TaskStateResolver.java` - pure Java resolver
- `app/src/test/java/com/preston/codex/domain/tasks/state/TaskStateResolverTest.java`

**State Derivation Rules:**
- Terminal states:
  - If `status == COMPLETED`, return `(COMPLETED, isOverdue = false)`.
  - If `status == CANCELLED`, return `(CANCELLED, isOverdue = false)`.
- If `kind == ONE_OFF` and `date == null`: return `(NO_DATE, isOverdue = false)` (UI label: "Unscheduled").
- Overdue determination:
  - `isOverdue = (date != null && date.isBefore(today))`.
- If `status == IN_PROGRESS`:
  - Return `(IN_PROGRESS, isOverdue)`.
- If `status == OPEN`:
  - `POSTPONED`: defined as `not terminal AND date >= today AND hasRescheduleHistory`. Return `(POSTPONED, isOverdue = false)`.
  - Otherwise: return `(SCHEDULED, isOverdue)`.

---

### 2.3 Recurrence Materialization
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/recurrence/RecurrenceType.java` (`DAILY`, `WEEKLY`, `MONTHLY`, `CUSTOM_INTERVAL`)
- `app/src/main/java/com/preston/codex/domain/tasks/recurrence/MissedPolicy.java` (`KEEP_OVERDUE`, `SKIP_AUTOMATICALLY`, `CARRY_FORWARD_FLEXIBLE_ONLY`)
- `app/src/main/java/com/preston/codex/domain/tasks/recurrence/OccurrenceGenerator.java` - pure generator:
  ```java
  public final class OccurrenceGenerator {
      public List<LocalDate> generate(
          RecurrenceRule rule,
          LocalDate today,
          Set<LocalDate> existingDates,
          Set<LocalDate> retainedDates,
          int existingCount
      )
  }
  ```
- `app/src/main/java/com/preston/codex/domain/tasks/recurrence/RecurrenceService.java` - coordinates persistence and generation
- `app/src/test/java/com/preston/codex/domain/tasks/recurrence/OccurrenceGeneratorTest.java`

**Rules:**
- `OccurrenceGenerator` is strictly pure: no `Clock`, no `ZoneId`, no database/repository dependencies.
- Horizon: 90 days forward from `max(rule.startDate(), today)`. No backfilling of past dates.
- Weekdays format: comma-separated 3-letter uppercase days: `"MON,WED,FRI"` (or `"MON,TUE,WED,THU,FRI"`).
- ISO weeks start on Monday.
- Monthly anchor day: `anchor_day_of_month` clamped to the length of the month (e.g., day 31 clamps to 30 in April, 28/29 in February).
- Occurrences have NO deadline of their own; deadline reminders apply exclusively to one-off tasks.
- Count-limited rules count all existing occurrences (including `CANCELLED` and `SKIPPED`). Generation stops when `existingCount + newCount == rule.occurrenceCount()`.

---

### 2.4 Recurrence Editing
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/recurrence/RecurrenceEditScope.java` (`THIS_OCCURRENCE`, `THIS_AND_FUTURE`)
- `app/src/main/java/com/preston/codex/domain/tasks/recurrence/RecurrenceEditService.java`
- `app/src/test/java/com/preston/codex/domain/tasks/recurrence/RecurrenceEditServiceTest.java`

**Editing Rules:**
- `THIS_OCCURRENCE`:
  - Updates only the specified occurrence record using override columns (`title_override`, `time_override`, `note_override`, `reflection`).
  - Does not modify the series task or other occurrences.
- `THIS_AND_FUTURE`:
  - Creates a new `task` row (`task_kind = SERIES`, `previous_task_id = oldTask.id()`).
  - Copies `knowledge_entry_id`, `tags`, and `is_flexible` to the new series task.
  - Does NOT copy `reflection` text to the new series.
  - Truncates the old recurrence rule by setting its `end_date` to `splitDate.minusDays(1)`.
  - Deletes only untouched future occurrences of the old rule (status `OPEN`, no events, no history). Any modified or completed future occurrences are preserved.
  - Adjusts remaining count for count-limited rules: `remainingCount = originalCount - keptOccurrencesCount`.
  - Creates a new `recurrence_rule` linked to the new series task, starting at `splitDate`.
  - When materializing the new series, the generator receives `retainedDates` (dates kept by the old series) and skips them to avoid duplicate occurrences.
  - Past occurrences are never rewritten. Whole-series editing is out of scope.

---

### 2.5 Task Service & Operations
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/service/TaskService.java`
- `app/src/main/java/com/preston/codex/domain/tasks/repository/TaskRepository.java` - domain repository interface
- `app/src/main/java/com/preston/codex/data/repository/RoomTaskRepository.java` - Room implementation of domain repository
- `app/src/test/java/com/preston/codex/domain/tasks/service/TaskServiceTest.java`

**Methods & Signatures:**
```java
public interface TaskService {
    Task createOneOff(CreateOneOffTaskCommand command);
    Task createSeries(CreateSeriesTaskCommand command, RecurrenceRule rule);
    void complete(long id, boolean isOccurrence);
    void reopen(long id, boolean isOccurrence);
    void cancel(long id, boolean isOccurrence);
    void markInProgress(long id, boolean isOccurrence);
    void setReflection(long id, boolean isOccurrence, String reflection);
    void linkKnowledge(long taskId, Long knowledgeEntryId);
    void unlinkKnowledge(long taskId);
    void setTags(long taskId, Set<String> tags); // tags converted to lowercase
    void deleteTask(long taskId); // also deletes associated reminder_state rows in same transaction
    void cancelSeries(long seriesTaskId); // ends rule today and cancels untouched future OPEN occurrences with events
}
```

**Key Invariants:**
- Completing Monday's occurrence leaves Tuesday's occurrence untouched.
- Every state transition appends an immutable `TaskEvent` with timestamp and transition details.
- Deleting a task deletes all associated tags, occurrences, events, and reschedule history via SQLite CASCADE foreign keys, and explicitly purges all related `reminder_state` rows in the same transaction.

---

### 2.6 Missed Occurrences
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/missed/MissedOccurrenceService.java`
- `app/src/test/java/com/preston/codex/domain/tasks/missed/MissedOccurrenceServiceTest.java`

**Requirements:**
- Runs on application startup and in daily background work. Idempotent.
- Evaluates occurrences where `effective_date < today` (never uses `occurrence_date`).
- Policies:
  - `KEEP_OVERDUE`: default policy. No modification.
  - `SKIP_AUTOMATICALLY`: sets occurrence status to `SKIPPED` and appends a `POLICY` event to `task_event`.
  - `CARRY_FORWARD_FLEXIBLE_ONLY`: if flexible and `auto_reschedule_count < N` (N=3), sets `effective_date = today`, increments `auto_reschedule_count`, and writes a `reschedule_history` record. If non-flexible or count >= N, behaves as `KEEP_OVERDUE`.
- A carried occurrence has its effective date updated to today and is never re-handled on the same day.
- Exposes `recentlyHandled(LocalDate date)` query.

---

### 2.7 Postponement
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/postpone/PostponementService.java`
- `app/src/test/java/com/preston/codex/domain/tasks/postpone/PostponementServiceTest.java`

**Requirements:**
- Manual postponement retains the previous scheduled date and appends a `reschedule_history` entry (`from_date`, `to_date`, `reason`, `timestamp`).
- For occurrences, updates `effective_date` while leaving `occurrence_date` intact.
- Manual postponement past deadline is permitted but flagged/recorded. Tasks with deadlines are never automatically moved past their deadline.

---

### 2.8 Backlog Review & Planning
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/backlog/TaskPlannerConfig.java`:
  - Daily task cap: 6 tasks/day
  - Daily duration cap: 240 minutes/day
  - Lookahead horizon: 14 days
  - Maximum auto-reschedules: N = 3
  - Default duration: 30 minutes (used whenever `estimated_duration_minutes` is null)
- `app/src/main/java/com/preston/codex/domain/tasks/backlog/BacklogReviewCandidate.java`
- `app/src/main/java/com/preston/codex/domain/tasks/backlog/BacklogMoveProposal.java`
- `app/src/main/java/com/preston/codex/domain/tasks/backlog/BacklogReviewReport.java` (applied moves and skipped moves with reasons)
- `app/src/main/java/com/preston/codex/domain/tasks/backlog/TaskPlanner.java` - pure scheduling algorithm
- `app/src/main/java/com/preston/codex/domain/tasks/backlog/BacklogReviewService.java`
- `app/src/test/java/com/preston/codex/domain/tasks/backlog/TaskPlannerTest.java`
- `app/src/test/java/com/preston/codex/domain/tasks/backlog/BacklogReviewServiceTest.java`

**Algorithm & Workflow:**
- Review is triggered only by explicit user action; never executed silently in background.
- Candidates: tasks/occurrences where `is_flexible == true`, `status` is incomplete (`OPEN` or `IN_PROGRESS`), and `effective_date < today` (or `scheduled_date < today`).
- Non-flexible tasks are never moved.
- If `auto_reschedule_count >= N` (3): marked "needs attention", never automatically moved.
- If task has a deadline and the deadline has already passed (`deadline < today`): marked "needs attention", never moved.
- Sorting order:
  1. Priority descending (`URGENT` > `HIGH` > `MEDIUM` > `LOW`)
  2. Deadline ascending (nulls last)
  3. Days overdue descending
  4. ID ascending
- Placement logic:
  - Evaluates days from `today` up to `today.plusDays(13)` (14-day horizon).
  - Target day must satisfy: `proposedDate <= deadline` (if deadline is present).
  - Updates in-memory load for target day (task count and duration).
  - Adding candidate must not exceed caps (<= 6 tasks and <= 240 minutes).
  - Candidate does not count against its own old day.
  - Both one-off tasks and materialized occurrences count towards target day load by effective date.
- Execution via `apply()`:
  - Runs in a single database transaction.
  - Re-validates each approved move inside the transaction: verifies task is still incomplete, per-day task count and minute caps still hold for target date, and deadline is still respected.
  - Skips stale moves and returns a comprehensive `BacklogReviewReport` listing applied and skipped moves.

---

### 2.9 Knowledge Link
**Files to create/change:**
- Domain model updates and Room entity FK mapping on `knowledge_entry_id`
- `app/src/test/java/com/preston/codex/domain/tasks/knowledge/TaskKnowledgeLinkTest.java`

**Requirements:**
- A task can link to a `knowledge_entry` via nullable `knowledge_entry_id` with `ON DELETE SET NULL`.
- Optional `reflection` column stores brief textual thoughts on application of knowledge.
- Link and reflection survive task postponement and completion.
- Deleting the linked `knowledge_entry` sets `task.knowledge_entry_id` to NULL while keeping the task row and reflection text intact.

---

## 3. Milestone 2 Plan (after approval of Milestone 1 report)

Milestone 2 will be started only after Milestone 1 automated tests pass and the Milestone 1 report is approved.

### 3.1 Reminders
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/reminder/model/ReminderType.java` (`DAILY_DIGEST`, `TIMED_TASK`, `DEADLINE_D_MINUS_1`, `DEADLINE_D_DAY`)
- `app/src/main/java/com/preston/codex/domain/tasks/reminder/model/ReminderChannel.java`
- `app/src/main/java/com/preston/codex/domain/tasks/reminder/model/ReminderStatus.java` (`PENDING`, `FIRED`, `CANCELLED`, `MISSED`)
- `app/src/main/java/com/preston/codex/domain/tasks/reminder/model/OwnerType.java` (`TASK`, `TASK_OCCURRENCE`, `DAILY_DIGEST`)
- `app/src/main/java/com/preston/codex/domain/tasks/reminder/service/ReminderIdempotencyKeyGenerator.java`:
  - Digest & deadline: `TYPE:OWNER_TYPE:OWNER_ID:LOCAL_DATE`
  - Timed: `TASK_TIMED:OWNER_TYPE:OWNER_ID:LOCAL_DATETIME`
- `app/src/main/java/com/preston/codex/domain/tasks/reminder/service/ReminderService.java`
- `app/src/main/java/com/preston/codex/integration/notifications/TaskReminderWorker.java`
- `app/src/main/java/com/preston/codex/integration/notifications/DeadlineReminderWorker.java`
- `app/src/test/java/com/preston/codex/domain/tasks/reminder/*Test.java`

**Requirements:**
- Daily digest at 08:00 (default on).
- Timed reminders for tasks with scheduled time (default on; single next-trigger chain worker waking at earliest pending reminder).
- Deadline reminders on D-1 and D-day at 08:00 (default on). Deadline reminders apply to one-off tasks only.
- `reminder_state` rows created, updated, and cancelled in same transaction as task lifecycle operations. Completing/cancelling cancels pending reminders. Deleting task owner deletes its reminder rows.
- Recompute trigger_at on timezone/time change. WorkManager self-rescheduling with `REPLACE` outside worker and `APPEND_OR_REPLACE` inside worker. No exact alarms.

---

### 3.2 Debug (src/debug only)
**Files to create/change:**
- `app/src/debug/java/com/preston/codex/debug/DebugClock.java` - persisted clock offset
- `app/src/debug/java/com/preston/codex/debug/DebugPreferences.java`
- `app/src/debug/java/com/preston/codex/debug/DebugActions.java` - UI buttons: add one-off task, add weekly recurring task, fast-forward one day, run backlog preview, trigger workers now
- `app/src/testDebug/java/com/preston/codex/debug/DebugDevPanelTest.java`

---

## 4. Milestones & Checklists

### Milestone 1 Execution Checklist:
- [ ] Schema v3 migration (`MIGRATION_2_3`), entities in `data/`, and schema export to `app/schemas/.../3.json`.
- [ ] Plain domain models and pure `TaskStateResolver`.
- [ ] Pure `OccurrenceGenerator` and `RecurrenceService`.
- [ ] `RecurrenceEditService` supporting `THIS_OCCURRENCE` and `THIS_AND_FUTURE`.
- [ ] `TaskService` with full lifecycle operations, event logging, and cascade deletes.
- [ ] `MissedOccurrenceService` with three policies.
- [ ] `PostponementService` with reschedule history.
- [ ] `TaskPlanner` and `BacklogReviewService` with in-transaction re-validation.
- [ ] Knowledge link and persistence tests.
- [ ] **Verification**: Milestone 1 is verified entirely through automated unit, DAO, and migration tests. Phone testing starts with Milestone 2.
- [ ] **Exit**: STOP and report test counts per class and `3.json` path before Milestone 2.

### Milestone 2 Execution Checklist (after approval):
- [ ] Reminder state engine, idempotency key generator, and transaction hooks.
- [ ] Notification workers and WorkManager chains.
- [ ] Timezone/DST transition handling.
- [ ] Debug panel actions in `src/debug`.
- [ ] Phone testing checklist execution.

---

## 5. Testing Strategy & Test Inventory

All domain logic is tested on the JVM with pure Java unit tests using injected `Clock` and `Supplier<ZoneId>`. Database DAOs and repositories are tested with Room in-memory databases. Migrations are tested using `MigrationTestHelper` against `2.json`.

### Test Inventory & Target Counts per Class:

1. **`TaskStateResolverTest` (11 tests)**
   - Terminal status `COMPLETED` returns `(COMPLETED, isOverdue = false)` even if date is past.
   - Terminal status `CANCELLED` returns `(CANCELLED, isOverdue = false)` even if date is past.
   - Status `OPEN` with null date returns `(NO_DATE, isOverdue = false)`.
   - Status `OPEN` with `date == today` returns `(SCHEDULED, isOverdue = false)`.
   - Status `OPEN` with `date > today` returns `(SCHEDULED, isOverdue = false)`.
   - Status `OPEN` with `date < today` returns `(SCHEDULED, isOverdue = true)`.
   - Status `IN_PROGRESS` with `date < today` returns `(IN_PROGRESS, isOverdue = true)`.
   - Status `IN_PROGRESS` with `date >= today` returns `(IN_PROGRESS, isOverdue = false)`.
   - Status `OPEN` with `date >= today` AND `hasRescheduleHistory == true` returns `(POSTPONED, isOverdue = false)`.
   - Status `OPEN` with `date < today` AND `hasRescheduleHistory == true` returns `(SCHEDULED, isOverdue = true)`.
   - Series kind handling: series rows never generate derived states directly.

2. **`OccurrenceGeneratorTest` (13 tests)**
   - Daily recurrence generates correct dates over 90-day forward window.
   - Start date in future: window begins at `rule.startDate()`.
   - Start date in past: no backfill; window begins at `today`.
   - Existing dates are skipped (idempotent generation).
   - Retained dates from old series are skipped (split-series deduplication).
   - Count-limited rule stops when `existingCount + newCount == occurrenceCount`.
   - Weekly interval on Mondays (ISO week starting Monday).
   - Weekly interval with multiple weekdays (`"MON,WED,FRI"`).
   - Monthly interval with anchor day 31 clamping to 30 in April.
   - Monthly interval with anchor day 31 clamping to 29 in leap year February.
   - Monthly interval with anchor day 31 clamping to 28 in non-leap year February.
   - Custom interval (e.g., every 10 days) across month boundaries.
   - End date boundary strictly respected.

3. **`RecurrenceServiceTest` (4 tests)**
   - `ensureMaterialized` generates and inserts occurrences for active series up to 90 days.
   - `ensureMaterialized` is idempotent when invoked repeatedly on the same day.
   - `ensureMaterialized` skips ended/cancelled series.
   - `ensureMaterialized` counts all occurrences (`CANCELLED` and `SKIPPED` included) towards count limit.

4. **`TaskServiceTest` (18 tests)**
   - `createOneOff` with null duration defaults to 30 minutes, status `OPEN`, timestamps set.
   - `createOneOff` with full attributes (priority, deadline, scheduled date/time, tags, knowledge link).
   - `createSeries` creates parent task, recurrence rule, and invokes initial materialization.
   - `complete` on one-off task updates status to `COMPLETED`, records `completed_at`, appends `TaskEvent`.
   - `complete` on occurrence updates status to `COMPLETED`, records `completed_at`, appends `TaskEvent`.
   - **Critical check**: Completing Monday's occurrence leaves Tuesday's occurrence untouched in `OPEN` status.
   - `reopen` on completed task transitions back to `OPEN`, clears `completed_at`, appends `TaskEvent`.
   - `reopen` on completed occurrence transitions back to `OPEN`, clears `completed_at`, appends `TaskEvent`.
   - `cancel` on task sets `CANCELLED`, records `cancelled_at`, appends `TaskEvent`.
   - `cancel` on occurrence sets `CANCELLED`, records `cancelled_at`, appends `TaskEvent`.
   - `markInProgress` transitions task to `IN_PROGRESS` and appends `TaskEvent`.
   - `markInProgress` transitions occurrence to `IN_PROGRESS` and appends `TaskEvent`.
   - `setReflection` updates reflection text on task.
   - `setReflection` updates reflection text on occurrence.
   - `linkKnowledge` and `unlinkKnowledge` manage knowledge reference.
   - `setTags` replaces task tags with normalized lowercase tags.
   - `deleteTask` cascades deletion and purges associated `reminder_state` rows in same transaction.
   - `cancelSeries` truncates rule to today and cancels untouched future OPEN occurrences with events.

5. **`RecurrenceEditServiceTest` (13 tests)**
   - `THIS_OCCURRENCE`: sets override columns (`title_override`, `time_override`, `note_override`, `reflection`).
   - `THIS_OCCURRENCE`: verifies base series task and subsequent occurrences remain unmodified.
   - `THIS_AND_FUTURE`: creates new series task row with `previous_task_id` pointing to old task.
   - `THIS_AND_FUTURE`: copies `tags` to the new series task.
   - `THIS_AND_FUTURE`: copies `is_flexible` to the new series task.
   - `THIS_AND_FUTURE`: copies `knowledge_entry_id` to the new series task.
   - `THIS_AND_FUTURE`: does NOT copy reflection text to the new series task.
   - `THIS_AND_FUTURE`: truncates old rule `end_date` to `splitDate.minusDays(1)`.
   - `THIS_AND_FUTURE`: deletes untouched future occurrences of old rule (`OPEN`, no events, no history).
   - `THIS_AND_FUTURE`: retains touched or completed future occurrences of old rule.
   - `THIS_AND_FUTURE`: adjusts remaining count by subtracting all kept occurrences.
   - `THIS_AND_FUTURE`: new series generator skips retained dates from old series (no duplicate occurrences).
   - `THIS_AND_FUTURE`: verifies historical past occurrences are never altered or deleted.

6. **`MissedOccurrenceServiceTest` (9 tests)**
   - Ignores occurrences where `effective_date >= today`.
   - Uses `effective_date < today` (never `occurrence_date`) to identify missed occurrences.
   - `KEEP_OVERDUE` policy leaves occurrence untouched.
   - `SKIP_AUTOMATICALLY` sets occurrence status to `SKIPPED` and appends `POLICY` event.
   - `CARRY_FORWARD_FLEXIBLE_ONLY` carries flexible occurrence to today, increments count, logs history.
   - `CARRY_FORWARD_FLEXIBLE_ONLY` treats non-flexible occurrence as `KEEP_OVERDUE`.
   - `CARRY_FORWARD_FLEXIBLE_ONLY` treats occurrence with `auto_reschedule_count >= N` (3) as `KEEP_OVERDUE`.
   - Carried occurrence is not re-handled on the same day it was carried forward.
   - `recentlyHandled` query correctly surfaces handled occurrences for the specified date.

7. **`PostponementServiceTest` (4 tests)**
   - Postpone one-off task updates `scheduled_date` and appends `reschedule_history` entry.
   - Postpone occurrence updates `effective_date` (leaving `occurrence_date` unchanged) and appends history.
   - Manual postponement allows moving past deadline but logs flagged event/history.
   - Manual postponement records user-specified reason and timestamp.

8. **`BacklogReviewServiceTest` (18 tests)**
   - `preview()` identifies candidates (flexible AND incomplete AND effective/scheduled date < today).
   - `preview()` excludes non-flexible tasks.
   - `preview()` excludes completed and cancelled tasks.
   - `preview()` flags tasks with `auto_reschedule_count >= N` (3) as "needs attention" with no move.
   - `preview()` sorts candidates: priority desc, deadline asc (nulls last), days overdue desc, id asc.
   - `preview()` respects daily task cap (max 6 tasks/day).
   - `preview()` respects daily duration cap (max 240 min/day).
   - `preview()` applies 30-minute default when `estimated_duration_minutes` is null.
   - `preview()` never moves a task past its deadline.
   - `preview()` flags overdue tasks whose deadline has passed (`deadline < today`) as "needs attention", no move.
   - **Deadline boundary case**: `deadline == today` (proposal constrained to today or flagged).
   - **Deadline boundary case**: `deadline == target day` (candidate can move to target day, but cannot move to target day + 1).
   - `preview()` updates in-memory per-day load after each proposed move.
   - `preview()` does not count a candidate against its own old day.
   - `apply()` persists approved moves in a single transaction (dates, counters, history, events).
   - `apply()` re-validates each approved move inside the transaction (still incomplete, caps still hold, deadline respected).
   - `apply()` skips stale moves (e.g. task completed between preview and apply).
   - `apply()` returns structured report of applied and skipped moves with reasons.

9. **`TaskKnowledgeLinkTest` (6 tests)**
   - Link one-off task to existing knowledge entry.
   - Task reflection text stored alongside knowledge reference.
   - Knowledge link and reflection survive task postponement.
   - Knowledge link and reflection survive task completion.
   - Deleting knowledge entry sets `task.knowledge_entry_id` to NULL (`ON DELETE SET NULL`).
   - Deleting knowledge entry preserves task row, completion status, and reflection text.

10. **`RoomTaskDaoTest` (8 tests)**
    - Insert and query `TaskEntity` with all fields, enum strings, and ISO dates/times.
    - Cascade delete from `TaskEntity` to `TaskTagEntity`.
    - Cascade delete from `TaskEntity` to `TaskOccurrenceEntity`, `TaskEventEntity`, and `RescheduleHistoryEntity`.
    - Query occurrences filtered by `effective_date`.
    - Query occurrences filtered by `rule_id`.
    - Exclude `SERIES` tasks from one-off queries.
    - Unique index constraint on `(task_id, occurrence_date)` enforced.
    - Verify all required indexes exist and match schema.

11. **`Migration2To3Test` (6 tests)**
    - Migrates database from `2.json` fixture to version 3 using `MigrationTestHelper`.
    - Asserts SQLite foreign keys pragma is `ON`.
    - Verifies knowledge entries, sources, and daily insight records from v2 are intact post-migration.
    - Verifies all 7 new tables exist with exact columns, affinities, and nullability.
    - Verifies foreign key constraints on `previous_task_id`, `knowledge_entry_id`, `task_id`, and `rule_id`.
    - Verifies indexes on `previous_task_id`, `knowledge_entry_id`, `scheduled_date`, `deadline`, `rule_id`, `effective_date`, `reminder_state(owner_type, owner_id)`, `reminder_state.status`, and `reminder_state.trigger_at`.

**Total Milestone 1 Tests Planned: 110 tests** (exceeds the 90+ requirement).

---

## 6. Confirmed Answers
- **Horizon**: 90 days forward with no backfill.
- **Deadline granularity**: Date only (`LocalDate` stored as ISO TEXT).
- **Tags storage**: Normalized lowercase in separate `task_tag` table.
- **Daily capacity limits**: Max 6 tasks or 240 minutes per day.
- **Default task duration**: 30 minutes when `estimated_duration_minutes` is null.
- **Auto-reschedule limit**: N = 3 before requiring user attention.
- **Weekdays format**: Comma-separated string of 3-letter uppercase codes (e.g., `"MON,WED,FRI"`).
- **Occurrence deadlines**: Occurrences have no deadline of their own; deadline reminders apply to one-off tasks.
- **Reminder defaults**: Daily digest (08:00), timed reminders, and deadline reminders (D-1 and D-day at 08:00) all default to enabled.

---

## 7. Reporting Requirements
- Do not commit or push.
- After Milestone 1 implementation:
  - Report exact test counts per test class and total test count.
  - Report the file path of exported `3.json` (`app/schemas/com.preston.codex.data.database.AppDatabase/3.json`).
  - Report every deviation from `docs/PHASE4_DESIGN.md` explicitly.
  - Report any open questions or blocked items.

---

## Objections
None. All items in `docs/PHASE4_DESIGN.md` and user amendments 1-11 are accepted as binding. If during implementation any item proves unimplementable as specified, it will be raised under Objections in the report with concrete details rather than changed silently.
