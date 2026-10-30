# Phase 4 Design (binding) — Task & Scheduling Engine

This document is the decided design for Phase 4. Every numbered item is mandatory
and overrides any default the implementer would otherwise choose. If an item is
believed to be wrong, raise it under "Objections" in the plan; never change it
silently.

Scope: spec section 3, the task parts of section 9, and the task parts of
section 11. No full app UI. Do not touch the importer or the insight engine.

Environment: Java 17, Room, domain logic in pure Java (no `android.*`), driven by
an injected `Clock` and `Supplier<ZoneId>`, tested on the JVM. No new dependency
without reporting its verified version.

## A. Schema

1. Schema v3 is create-only: a real `MIGRATION_2_3` (no alter/drop of existing
   tables), a `MigrationTestHelper` test starting from `2.json` with populated
   rows, the FK pragma asserted ON, and an exported `3.json`. The Room-generated
   `3.json` is the schema authority; any DDL text in a plan is documentation.
   Never use destructive migration.
2. No SQL `DEFAULT` clauses anywhere (Java supplies every value), so Room schema
   validation matches exactly.
3. Tables: `task`, `task_tag`, `recurrence_rule`, `task_occurrence`,
   `task_event`, `reschedule_history`, `reminder_state`.
4. `task` includes: `task_kind` (`ONE_OFF` | `SERIES`), `previous_task_id`
   (nullable self-FK, ON DELETE SET NULL), `knowledge_entry_id` (nullable FK to
   `knowledge_entry`, ON DELETE SET NULL), `reflection`, `is_flexible`,
   `priority`, `deadline` (date only), `estimated_duration_minutes`,
   `scheduled_date`, `scheduled_time`, `auto_reschedule_count`, `completed_at`,
   `cancelled_at`, timestamps.
5. `task_tag(task_id, tag)`: composite primary key, ON DELETE CASCADE, tags stored
   lowercase. Do NOT store tags as a column on `task`.
6. `recurrence_rule`: type (`DAILY`, `WEEKLY`, `MONTHLY`, `CUSTOM_INTERVAL`),
   interval, weekdays, start_date, end_date, occurrence_count,
   `anchor_day_of_month`, `missed_policy`.
7. `task_occurrence`: `task_id`, `rule_id`, `occurrence_date` (the rule's identity
   for this occurrence; never changes), `effective_date` (the only date that
   postponement or carry-forward changes), `status`, `title_override`,
   `time_override`, `note_override`, `reflection`, `auto_reschedule_count`,
   `completed_at`, `cancelled_at`, timestamps. Unique on
   (`task_id`, `occurrence_date`). Children CASCADE from `task`.
8. `reminder_state`: `reminder_type`, `owner_type`, `owner_id`, `trigger_at`,
   `trigger_local_date`, `trigger_local_time` (nullable), `channel`, `status`
   (`PENDING`, `FIRED`, `CANCELLED`, `MISSED`), `fired_at`, `idempotency_key`
   (UNIQUE), `created_at`. No FK exists to owners, so cleanup on owner deletion is
   explicit and tested.

## B. State model

9. Persist only facts. Task status: `OPEN`, `IN_PROGRESS`, `COMPLETED`,
   `CANCELLED`. Occurrence status: the same plus `SKIPPED`. `POSTPONED`,
   `OVERDUE` and the "unscheduled" state are NEVER stored.
10. One `TaskStateResolver` derives `NO_DATE` (UI label "Unscheduled"),
    `SCHEDULED`, `POSTPONED` and returns an `isOverdue` flag next to the state.
    `IN_PROGRESS` with a past date is state `IN_PROGRESS` with `isOverdue=true`.
11. `SERIES` rows are excluded from one-off queries and from derived-state
    listings; a series appears only through its occurrences.
12. Completion is reversible. Events record both transitions and the original
    scheduling history is kept.

## C. Recurrence

13. Materialization uses a 90-day forward window, is idempotent, and never
    modifies existing rows. It starts at `max(rule.start_date, today)` and does
    NOT backfill the past. Count-limited rules count all existing occurrences,
    including `CANCELLED` and `SKIPPED`.
14. `ensureMaterialized(today)` is the only way occurrences are created. It is
    called from app open, the daily worker, and rule create/edit. Read queries
    never mutate.
15. Weekly intervals use ISO weeks (Monday start). Monthly rules use
    `anchor_day_of_month`, clamped to the last day of shorter months (leap
    years tested).
16. Edit scope is always explicit.
    - `THIS_OCCURRENCE`: sets the override columns on that occurrence only.
    - `THIS_AND_FUTURE`: creates a NEW `task` row (new series) with
      `previous_task_id`; ends the old rule the day before; deletes only
      untouched future occurrences of the old rule (status `OPEN`, no events, no
      history); the new rule's generator skips dates the old series retained.
      Remaining count = original count minus every occurrence kept in the old
      series. Copy `knowledge_entry_id` to the new series but NOT reflection
      text. Past occurrences are never rewritten.
    - Whole-series editing is out of scope.

## D. Missed occurrences

17. Maintenance runs on app open and in the daily worker, is idempotent, uses
    `effective_date < today` (never `occurrence_date`), and exposes a
    `recentlyHandled()` query.
    - `KEEP_OVERDUE` (default): no change. The global preference is copied into
      the rule at creation.
    - `SKIP_AUTOMATICALLY`: occurrence becomes `SKIPPED` with a `POLICY` event.
    - `CARRY_FORWARD_FLEXIBLE_ONLY`: flexible occurrences only, limited by N
      (below): set `effective_date` to today, increment the counter, log
      `reschedule_history`. Anything else behaves as `KEEP_OVERDUE`. A carried
      occurrence is not re-handled on the day it was carried.

## E. Postponement and backlog review

18. Manual postponement keeps the previous date and appends `reschedule_history`
    (from, to, reason, time). Deadline tasks are never auto-moved; a manual move
    past the deadline is allowed but flagged.
19. Backlog review is an explicit user action only, never silent or background.
    Candidates: flexible AND incomplete AND effective/scheduled date < today.
    Non-flexible tasks are never moved. A flexible task with a deadline may move
    but never past the deadline; if its deadline has passed it is
    "needs attention" with no move.
20. Two steps: `preview()` proposes moves and writes nothing; `apply(approved)`
    writes in one transaction (dates, counters, `reschedule_history`, events).
21. Constants live in one config class: 6 tasks per day, 240 minutes per day,
    14-day horizon, N = 3 auto-reschedules, default estimate 30 minutes. Sort:
    priority desc, deadline asc (nulls last), days overdue desc, id asc. The
    planner updates its in-memory per-day load after each proposed move, checks
    caps after adding, does not count a candidate against its own old day, and
    counts tasks and occurrences by effective date. A task at N is "needs
    attention" and is not moved.

## F. Knowledge link

22. A task may reference a knowledge entry (nullable, ON DELETE SET NULL) plus an
    optional short reflection. The link survives postponement and completion.
    Deleting the entry keeps the task and its reflection.

## G. Reminders (Milestone 2; reuse the Phase 3 notification infrastructure)

23. Types: daily digest at 08:00 (default on), timed reminders for tasks with a
    scheduled time (default on; one next-trigger chain worker waking at the
    earliest pending `reminder_state` row), deadline reminders on D-1 and D-day
    at 08:00 (default on). Separate channels and preference keys.
24. Idempotency keys: digest and deadline = `TYPE:OWNER_TYPE:OWNER_ID:LOCAL_DATE`;
    timed = `TASK_TIMED:OWNER_TYPE:OWNER_ID:LOCAL_DATETIME`. No epoch in a key.
25. `reminder_state` rows are created, updated and cancelled in the same
    transaction as task create, reschedule, complete, cancel and materialization.
    Completing or cancelling cancels pending reminders. Deleting an owner removes
    its rows.
26. On timezone or time change, recompute `trigger_at` of pending rows from the
    stored local date/time without changing keys. Never create a reminder whose
    trigger time is already past. A late worker on the same local date fires once;
    a later date marks it `MISSED`. A nonexistent local time on a spring-forward
    day fires at the next valid instant.
27. Self-rescheduling one-time work: `REPLACE` from outside a worker,
    `APPEND_OR_REPLACE` from inside. No exact alarms.

## H. Debug only (src/debug, never in release)

28. A persisted clock offset and a mutable debug clock; buttons to add a one-off
    task, add a weekly recurring task, fast-forward one day, run the backlog
    preview, run the task-reminder worker now and run the deadline-reminder
    worker now (WorkManager ignores the debug clock). Debug tests live in
    `src/testDebug`.

## I. Milestones and process

29. Milestone 1: schema v3, migration, domain engine (resolver, generator, edit
    scopes, missed policy, backlog planner, services) and their tests. STOP and
    report before any reminder code. Milestone 2: reminders, debug additions,
    remaining tests.
30. Test-first. Required coverage includes DST days, a zone where DST skips
    midnight, zone change, month ends, leap years, weekday and ISO-week rules,
    idempotent materialization, no-backfill, split-series without duplicate dates,
    carried occurrences not re-handled the same day, backlog load updated after
    each move, and migration FK behavior (deleting a knowledge entry nulls
    `task.knowledge_entry_id` and keeps the task and reflection; deleting a task
    cascades to occurrences, events, history and tags).
31. Answers to questions raised earlier: horizon is 90 days forward with no
    backfill; deadline granularity is date only; tags use `task_tag`; the daily
    cap is 6 tasks or 240 minutes; N = 3; reminder defaults are on as listed in
    item 23.
32. Do not commit or push. Report test counts per class, the path of `3.json`,
    and every deviation from this document.