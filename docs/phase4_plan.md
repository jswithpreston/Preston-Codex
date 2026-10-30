# Phase 4 Plan (PLAN ONLY - Awaiting Approval)

## 1. Compliance Table (one row per numbered item in docs/PHASE4_DESIGN.md 1-32)

| Item | Requirement | Plan Section | Compliance Notes |
|---|---|---|---|
| 1 | Schema v3 create-only: MIGRATION_2_3 (no alter/drop), MigrationTestHelper from 2.json with populated rows, FK pragma ON, exported 3.json. Room-generated 3.json is authority. Never use destructive migration. | 2.1 Schema v3 Migration | Implement create-only migration, migration test helper, export schema to app/schemas/3.json. |
| 2 | No SQL DEFAULT clauses anywhere (Java supplies every value). | 2.1 Schema v3 Migration | All defaults handled in Java entities/DAOs/services. |
| 3 | Tables: task, task_tag, recurrence_rule, task_occurrence, task_event, reschedule_history, reminder_state. | 2.1 Schema v3 Migration | Define all 7 tables per schema design. |
| 4 | task includes task_kind (ONE_OFF\|SERIES), previous_task_id (nullable self-FK ON DELETE SET NULL), knowledge_entry_id (nullable FK to knowledge_entry ON DELETE SET NULL), reflection, is_flexible, priority, deadline (date only), estimated_duration_minutes, scheduled_date, scheduled_time, auto_reschedule_count, completed_at, cancelled_at, timestamps. | 2.1 Schema v3 Migration | Implement Task entity with all specified fields and FKs. |
| 5 | task_tag(task_id, tag): composite PK, ON DELETE CASCADE, tags lowercase. Do NOT store as column on task. | 2.1 Schema v3 Migration | Separate entity/table for tags. |
| 6 | recurrence_rule: type (DAILY,WEEKLY,MONTHLY,CUSTOM_INTERVAL), interval, weekdays, start_date, end_date, occurrence_count, anchor_day_of_month, missed_policy. | 2.1 Schema v3 Migration | Implement RecurrenceRule entity. |
| 7 | task_occurrence: task_id, rule_id, occurrence_date (identity, never changes), effective_date (only postponement/carry-forward changes), status, title_override, time_override, note_override, reflection, auto_reschedule_count, completed_at, cancelled_at, timestamps. Unique (task_id, occurrence_date). Children CASCADE from task. | 2.1 Schema v3 Migration | Implement TaskOccurrence with constraints. |
| 8 | reminder_state: reminder_type, owner_type, owner_id, trigger_at, trigger_local_date, trigger_local_time (nullable), channel, status (PENDING,FIRED,CANCELLED,MISSED), fired_at, idempotency_key (UNIQUE), created_at. No FK to owners; cleanup explicit and tested. | 2.1 Schema v3 Migration | Implement ReminderState with no FK, idempotency key unique. |
| 9 | Persist only facts. Task status: OPEN, IN_PROGRESS, COMPLETED, CANCELLED. Occurrence: same plus SKIPPED. POSTPONED, OVERDUE, "unscheduled" NEVER stored. | 2.2 State Model | Enums defined, no stored derived states. |
| 10 | TaskStateResolver derives NO_DATE (UI label "Unscheduled"), SCHEDULED, POSTPONED and returns isOverdue flag. IN_PROGRESS with past date is IN_PROGRESS with isOverdue=true. | 2.2 State Model | Implement resolver in domain layer. |
| 11 | SERIES rows excluded from one-off queries and derived-state listings; series appears only through occurrences. | 2.2 State Model | Query/DAO logic enforces exclusion. |
| 12 | Completion reversible. Events record transitions and original scheduling history kept. | 2.2 State Model | TaskEvent and RescheduleHistory entities/tracking. |
| 13 | Materialization 90-day forward window, idempotent, never modifies existing rows. Start at max(rule.start_date,today), NO backfill. Count-limited rules count all existing occurrences (CANCELLED/SKIPPED included). | 2.3 Recurrence Materialization | Implement OccurrenceGenerator with window logic. |
| 14 | ensureMaterialized(today) only way occurrences created. Called from app open, daily worker, rule create/edit. Read queries never mutate. | 2.3 Recurrence Materialization | Service method, integration points planned. |
| 15 | Weekly intervals use ISO weeks (Monday start). Monthly use anchor_day_of_month, clamped to last day of shorter months (leap years tested). | 2.3 Recurrence Materialization | Date logic with ZoneId/Clock, tests planned. |
| 16 | Edit scope explicit: THIS_OCCURRENCE sets override columns only; THIS_AND_FUTURE creates new task row with previous_task_id, ends old rule day before, deletes only untouched future occurrences of old rule (OPEN, no events, no history), new rule generator skips dates old series retained, remaining count adjusted, copy knowledge_entry_id not reflection; past occurrences never rewritten. Whole-series editing out of scope. | 2.4 Recurrence Editing | Edit service with all steps and validation. |
| 17 | Maintenance on app open and daily worker, idempotent, uses effective_date < today (never occurrence_date), exposes recentlyHandled(). KEEP_OVERDUE default (copied into rule at creation). SKIP_AUTOMATICALLY -> SKIPPED with POLICY event. CARRY_FORWARD_FLEXIBLE_ONLY -> flexible only limited by N, set effective_date to today, increment counter, log reschedule_history. Else KEEP_OVERDUE. Carried occurrence not re-handled same day. | 2.5 Missed Occurrences | MissedOccurrenceService with policies. |
| 18 | Manual postponement keeps previous date and appends reschedule_history (from,to,reason,time). Deadline tasks never auto-moved; manual move past deadline allowed but flagged. | 2.6 Postponement | Postponement logic with history tracking. |
| 19 | Backlog review explicit user action only, never silent/background. Candidates: flexible AND incomplete AND effective/scheduled date < today. Non-flexible never moved. Flexible with deadline may move but never past deadline; if deadline passed "needs attention" no move. | 2.7 Backlog Review | BacklogReviewService with candidate rules. |
| 20 | preview() proposes moves, writes nothing; apply(approved) writes in one transaction (dates,counters,reschedule_history,events). | 2.7 Backlog Review | Two-phase API. |
| 21 | Config constants: 6 tasks/day, 240 min/day, 14-day horizon, N=3, default estimate 30 min. Sort: priority desc, deadline asc (nulls last), days overdue desc, id asc. Planner updates in-memory per-day load after each move, checks caps after adding, doesn't count candidate against old day, counts tasks/occurrences by effective date. Task at N "needs attention" not moved. | 2.7 Backlog Review | TaskPlanner with all rules and constants. |
| 22 | Task may reference knowledge_entry (nullable ON DELETE SET NULL) plus optional short reflection. Link survives postponement/completion. Deleting entry keeps task and reflection. | 2.8 Knowledge Link | Schema supports, logic preserves link. |
| 23 | Reminders: daily digest at 08:00 (default on), timed reminders for tasks with scheduled time (default on, one next-trigger chain worker waking at earliest pending reminder_state), deadline reminders D-1 and D-day at 08:00 (default on). Separate channels and preference keys. | 3.1 Reminders (Milestone 2) | ReminderService and workers. |
| 24 | Idempotency keys: digest/deadline = TYPE:OWNER_TYPE:OWNER_ID:LOCAL_DATE; timed = TASK_TIMED:OWNER_TYPE:OWNER_ID:LOCAL_DATETIME. No epoch. | 3.1 Reminders (Milestone 2) | Key generation per spec. |
| 25 | reminder_state created/updated/cancelled in same transaction as task create,reschedule,complete,cancel,materialization. Completing/cancelling cancels pending reminders. Deleting owner removes its rows. | 3.1 Reminders (Milestone 2) | Transactional coordination. |
| 26 | On timezone/time change recompute trigger_at of pending rows from stored local date/time without changing keys. Never create reminder whose trigger already past. Late worker same local date fires once; later date marks MISSED. Nonexistent local time on spring-forward fires at next valid instant. | 3.1 Reminders (Milestone 2) | Timezone handling, DST logic. |
| 27 | Self-rescheduling: REPLACE from outside worker, APPEND_OR_REPLACE from inside. No exact alarms. | 3.1 Reminders (Milestone 2) | WorkManager scheduling strategy. |
| 28 | Debug only (src/debug never release): persisted clock offset, mutable debug clock; buttons to add one-off task, add weekly recurring task, fast-forward one day, run backlog preview, run task-reminder worker now, run deadline-reminder worker now (WorkManager ignores debug clock). Debug tests in src/testDebug. | 3.2 Debug (Milestone 2) | Debug UI/components and tests. |
| 29 | Milestone 1: schema v3, migration, domain engine (resolver,generator,edit scopes,missed policy,backlog planner,services) and tests. STOP and report before reminder code. Milestone 2: reminders, debug additions, remaining tests. | 4. Milestones | Enforce milestone boundary. |
| 30 | Test-first. Required coverage: DST days, zone where DST skips midnight, zone change, month ends, leap years, weekday/ISO-week, idempotent materialization, no-backfill, split-series no duplicate dates, carried occurrences not re-handled same day, backlog load updated after each move, migration FK behavior (deleting knowledge entry nulls task.knowledge_entry_id keeps task+reflection; deleting task cascades to occurrences/events/history/tags). | 5. Testing Strategy | Comprehensive test plan. |
| 31 | Answers: horizon 90 days forward no backfill; deadline granularity date only; tags use task_tag; daily cap 6 tasks or 240 min; N=3; reminder defaults on as listed. | 6. Confirmed Answers | Documented. |
| 32 | Do not commit or push. Report test counts per class, path of 3.json, every deviation from this document. | 7. Reporting Requirements | Final reporting. |

## 2. Milestone 1 Plan (STOP and report before Milestone 2)

### 2.1 Schema v3 Migration
**Files to create/change:**
- `app/src/main/java/com/preston/codex/data/database/PrestonDatabase.java` - bump version to 3, add MIGRATION_2_3
- `app/src/main/java/com/preston/codex/data/database/migration/Migration2To3.java` - create-only migration
- `app/src/main/java/com/preston/codex/data/entity/Task.java`, `TaskTag.java`, `RecurrenceRule.java`, `TaskOccurrence.java`, `TaskEvent.java`, `RescheduleHistory.java`, `ReminderState.java` - all entities
- `app/src/main/java/com/preston/codex/data/dao/TaskDao.java`, `TaskTagDao.java`, `RecurrenceRuleDao.java`, `TaskOccurrenceDao.java`, `TaskEventDao.java`, `RescheduleHistoryDao.java`, `ReminderStateDao.java`
- `app/schemas/com.preston.codex.data.database.PrestonDatabase/3.json` - exported schema (authority)
- `app/src/androidTest/java/com/preston/codex/data/database/migration/MigrationTestHelper.java` and migration test using 2.json

**Requirements:**
- Create-only migration: create all 7 tables with exact schema, indexes, FKs as specified. No ALTER/DROP of existing tables.
- Migration test loads 2.json fixture, runs MIGRATION_2_3, verifies schema/version, asserts FK pragma ON, validates migrated data preserved.
- Export 3.json via Room schema export; this is the schema authority.
- No SQL DEFAULT clauses - all defaults in Java.

### 2.2 State Model
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/TaskStatus.java`, `OccurrenceStatus.java`, `TaskKind.java`
- `app/src/main/java/com/preston/codex/domain/tasks/TaskState.java` (enum/value object for derived state)
- `app/src/main/java/com/preston/codex/domain/tasks/TaskStateResolver.java` - pure Java, no android.*
- `app/src/test/java/com/preston/codex/domain/tasks/TaskStateResolverTest.java`

**Requirements:**
- Persist only facts as specified (9). Resolver derives states and isOverdue flag (10). SERIES excluded from one-off queries (11). Completion reversible via events (12).

### 2.3 Recurrence Materialization
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/recurrence/RecurrenceType.java`, `MissedPolicy.java`
- `app/src/main/java/com/preston/codex/domain/tasks/recurrence/OccurrenceGenerator.java` - pure Java, uses Clock/ZoneId
- `app/src/main/java/com/preston/codex/domain/tasks/recurrence/RecurrenceService.java`
- `app/src/test/java/com/preston/codex/domain/tasks/recurrence/OccurrenceGeneratorTest.java`

**Requirements:**
- 90-day window, idempotent, no backfill, start at max(rule.start_date,today) (13). ensureMaterialized(today) only creation path (14). ISO weeks Monday, monthly anchor clamped (15). Count-limited includes CANCELLED/SKIPPED.

### 2.4 Recurrence Editing
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/recurrence/RecurrenceEditService.java`
- `app/src/test/java/com/preston/codex/domain/tasks/recurrence/RecurrenceEditServiceTest.java`

**Requirements:**
- THIS_OCCURRENCE: override columns only. THIS_AND_FUTURE: create new task with previous_task_id, end old rule day before, delete only untouched future occurrences of old rule (OPEN, no events, no history), skip dates retained by old series, adjust remaining count, copy knowledge_entry_id not reflection, never rewrite past (16).

### 2.5 Missed Occurrences
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/missed/MissedOccurrenceService.java`
- `app/src/test/java/com/preston/codex/domain/tasks/missed/MissedOccurrenceServiceTest.java`

**Requirements:**
- Maintenance on app open/daily worker, idempotent, uses effective_date < today (17). Three policies as specified, carried not re-handled same day.

### 2.6 Postponement
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/postpone/PostponementService.java`
- `app/src/test/java/com/preston/codex/domain/tasks/postpone/PostponementServiceTest.java`

**Requirements:**
- Manual postponement keeps previous date, appends reschedule_history (18). Deadline rules enforced.

### 2.7 Backlog Review
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/backlog/TaskPlanner.java` (config constants)
- `app/src/main/java/com/preston/codex/domain/tasks/backlog/BacklogReviewService.java`
- `app/src/test/java/com/preston/codex/domain/tasks/backlog/TaskPlannerTest.java`
- `app/src/test/java/com/preston/codex/domain/tasks/backlog/BacklogReviewServiceTest.java`

**Requirements:**
- Explicit action only, candidate rules (19). preview() no writes, apply() single transaction (20). All constants and planner rules per (21).

### 2.8 Knowledge Link
**Files to create/change:**
- Update Task entity/DAO/repo as needed for knowledge_entry_id, reflection
- `app/src/test/java/com/preston/codex/domain/tasks/knowledge/TaskKnowledgeLinkTest.java`

**Requirements:**
- Link survives postponement/completion, deleting entry keeps task+reflection (22).

**Milestone 1 Exit:** All schema/migration/domain services and tests pass. STOP and report before Milestone 2 per (29). Include test counts per class, path of 3.json.

## 3. Milestone 2 Plan (after approval of Milestone 1 report)

### 3.1 Reminders
**Files to create/change:**
- `app/src/main/java/com/preston/codex/domain/tasks/reminder/ReminderType.java`, `ReminderChannel.java`, `ReminderStatus.java`, `OwnerType.java`
- `app/src/main/java/com/preston/codex/domain/tasks/reminder/ReminderIdempotencyKeyGenerator.java`
- `app/src/main/java/com/preston/codex/domain/tasks/reminder/ReminderService.java`
- `app/src/main/java/com/preston/codex/android-integration/reminder/*` (workers, receivers as needed)
- `app/src/test/java/com/preston/codex/domain/tasks/reminder/*Test.java`

**Requirements:**
- All reminder types with defaults (23). Idempotency keys per (24). Transactional coordination (25). Timezone/DST handling (26). WorkManager scheduling strategy (27).

### 3.2 Debug (src/debug only)
**Files to create/change:**
- `app/src/debug/java/com/preston/codex/debug/DebugClock.java`, `DebugPreferences.java`
- `app/src/debug/java/com/preston/codex/debug/DebugActions.java` (UI buttons)
- `app/src/testDebug/java/com/preston/codex/debug/*Test.java`

**Requirements:**
- All debug features in src/debug, never release (28). Debug tests in src/testDebug.

**Milestone 2 Exit:** All reminder and debug functionality complete with tests.

## 4. Milestones
As specified in (29). Hard stop between milestones requiring report and approval.

## 5. Testing Strategy
Follow (30). Key test areas:
- DST (spring forward/fall back), zone where DST skips midnight
- Zone change
- Month ends, leap years
- Weekday/ISO-week rules
- Idempotent materialization, no-backfill
- Split-series no duplicate dates
- Carried occurrences not re-handled same day
- Backlog load updated after each move
- Migration FK behavior (knowledge entry delete nulls task.knowledge_entry_id keeps task+reflection; task delete cascades)

All domain logic pure Java unit tests. Room/DAO tests with in-memory DB. Migration tests with fixtures.

## 6. Confirmed Answers
Per (31): horizon 90 days forward no backfill; deadline granularity date only; tags use task_tag; daily cap 6 tasks or 240 min; N=3; reminder defaults on as listed.

## 7. Reporting Requirements
Per (32): Do not commit or push. After each phase/milestone, report: what was built, test results (test counts per class), deviations from this document, open questions. At Milestone 1 completion, also report path of `3.json`. List every deviation explicitly.

## Objections
None. All items in PHASE4_DESIGN.md are accepted as binding per instructions. If during implementation any item proves unimplementable as specified, it will be raised under Objections in the report with concrete details - not changed silently.
