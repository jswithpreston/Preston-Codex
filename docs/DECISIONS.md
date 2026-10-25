# Preston Codex — Locked Decisions Log

This file records all architectural decisions that require explicit approval to change.
See AGENTS.md for the full rule set and the process for amending these decisions.

---

## Decision 1 — Import Identity

Import identity resolution order:
1. Explicit `ID` field (highest confidence).
2. SHA-256 fingerprint of normalized source title + normalized quote text.
3. Fuzzy match (Jaccard similarity fallback).

**Normalization:** trim leading/trailing whitespace, collapse internal whitespace to single
spaces, unify line endings (CRLF → LF), unify curly/smart quotes to straight quotes.
**Must not:** strip words or meaningful punctuation.

---

## Decision 2 — Duplicate Detection

A potential duplicate is defined as:
- Normalized-token Jaccard similarity ≥ 0.85, AND
- No exact ID match or fingerprint match.

Flagging applies regardless of source (same source or different source).
Similar quotes from **different sources are never auto-merged.**
All potential duplicates are flagged for user review; the system never auto-merges.

---

## Decision 3 — Unknown Markdown Fields

Unknown fields encountered during parsing are preserved verbatim in an `extraFields`
JSON column on the knowledge entry. No data is silently discarded.
Unknown fields are reported in the import preview.

---

## Decision 4 — Daily Insight Selection

- One canonical row per local date in the `DailyInsightSelection` table.
- The app and widget read the same row.
- Shuffle replaces that day's selection and appends to a selection-history table.
- No consecutive-day repeats when ≥ 2 eligible entries exist.
- If exactly 1 eligible entry exists, it is shown every day (repeat allowed).
- Empty state is shown only when 0 eligible entries exist.

---

## Decision 5 — Spaced Repetition

Algorithm: deterministic, interval-ladder based (zero-based indexing).

| Rating | Interval effect |
|---|---|
| Forgot | Reset to step 0; next interval = ladder[0] (1 day) |
| Difficult | Repeat at max(0, step − 1) ladder position |
| Remembered | Advance 1 step: min(lastStep, step + 1) |
| Easy | Advance 2 steps: min(lastStep, step + 2) |

Default ladder (days): **1, 3, 7, 14, 30, 60, 120** — configurable.
Default daily review limit: **20** — configurable.
Overdue items sorted by: days overdue descending, then lowest step.
Full review history is retained; changing the ladder never rewrites history.

---

## Decision 6 — Recurrence Edits

When editing a recurring task, the user is always asked to choose scope:
- "This occurrence only."
- "This and all future occurrences."

Past occurrences are **never rewritten.**
Each occurrence has its own independent completion status.

---

## Decision 7 — Backup and Restore

**Format:** versioned ZIP containing `manifest.json` (schemaVersion + timestamps)
plus JSON/Markdown payloads for all data domains.

**Restore behavior:** replace-after-confirmation.
Before restoring: validate the backup, show its contents to the user, snapshot the
current database. On failure: roll back to the pre-restore snapshot.
The system never silently merges incompatible records.

---

## Decision 8 — Deletion and Trash

Deleted entries enter a recoverable **trash** state retained for **30 days**,
after which they are permanently deleted.
**Archive** is a distinct state — archived entries are excluded from the default
library view but are not deleted and are not in trash.

---

## Decision 9 — Task Rescheduling

Automatic rescheduling applies **only** to tasks flagged as **flexible**.
Deadline-bound tasks remain **overdue** until the user explicitly reschedules or
completes them. The system never silently moves a deadline.

---

*Last updated: Phase 0 scaffold*
