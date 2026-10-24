# Phase 1 — Persistence Layer & Markdown Importer Plan (Revised with Final Amendments)

> Spec scope: §4, §5, §6, §15, §17.1, §17.2.
> Scope strictly restricted to persistence + importer: no UI, no widget, no tasks, no review scheduling.

---

## 1 · Room Schema (Schema Version 1)

Future-phase tables (`task`, `task_occurrence`, `review_record`, `review_schedule`, `daily_insight_selection`, `app_settings`) are **omitted** from Schema v1. Schema v1 contains only the Phase 1 entities.

### 1.1 Tables in Schema Version 1

| # | Entity | Table name | Purpose |
|---|---|---|---|
| 1 | `KnowledgeSourceEntity` | `knowledge_source` | Resource/book metadata with normalized lookup keys |
| 2 | `KnowledgeEntryEntity` | `knowledge_entry` | Quote/insight rows with immutable fingerprint, content_hash, and imported_hash |
| 3 | `KnowledgeTagEntity` | `knowledge_tag` | Canonical tag vocabulary |
| 4 | `KnowledgeEntryTagEntity` | `knowledge_entry_tag` | M:N junction between entries and tags |
| 5 | `KnowledgeRelationshipEntity` | `knowledge_relationship` | Undirected normalized entry links (`from_id < to_id`) |
| 6 | `ImportBatchEntity` | `import_batch` | Record of import runs (completed and failed) |
| 7 | `ImportResultEntity` | `import_result` | Outcome per entry per batch with full payload and incoming_hash for held entries |

*(Note on table count: exactly 7 tables are defined. Bookmark state is persisted as the `is_bookmarked` column on `knowledge_entry` per spec §15).*

---

### 1.2 `knowledge_source`

```sql
CREATE TABLE knowledge_source (
  id           INTEGER PRIMARY KEY AUTOINCREMENT,
  title        TEXT NOT NULL,
  author       TEXT NOT NULL DEFAULT '',
  title_key    TEXT NOT NULL,              -- normalized, lowercased
  author_key   TEXT NOT NULL DEFAULT '',   -- normalized, lowercased
  source_type  TEXT NOT NULL DEFAULT 'BOOK',
  publisher    TEXT,
  year         INTEGER,
  isbn         TEXT,
  created_at   INTEGER NOT NULL,           -- epoch-millis UTC
  updated_at   INTEGER NOT NULL,
  UNIQUE (title_key, author_key)
);
CREATE INDEX idx_ks_author_key ON knowledge_source(author_key);
```

---

### 1.3 `knowledge_entry`

```sql
CREATE TABLE knowledge_entry (
  id               INTEGER PRIMARY KEY AUTOINCREMENT,
  source_id        INTEGER NOT NULL REFERENCES knowledge_source(id)
                     ON DELETE RESTRICT,
  stable_id        TEXT,
  fingerprint      TEXT NOT NULL,          -- immutable identity: SHA-256(title_key + "\0" + quote with outer quotes stripped, case-preserved)
  content_hash     TEXT NOT NULL,          -- current hash over content fields (updated on in-app edit or resolution)
  imported_hash    TEXT NOT NULL,          -- content hash as of last import or accepted resolution (never changed by in-app edits)
  quote_text       TEXT NOT NULL,
  insight_text     TEXT,
  source_ref       TEXT,
  application      TEXT,
  entry_type       TEXT NOT NULL DEFAULT 'QUOTE',
  review_mode      TEXT,
  extra_fields     TEXT,                   -- JSON blob for unknown fields and dangling refs
  raw_markdown     TEXT,                   -- verbatim block for export/diagnostics
  is_bookmarked    INTEGER NOT NULL DEFAULT 0,
  is_archived      INTEGER NOT NULL DEFAULT 0,
  is_trashed       INTEGER NOT NULL DEFAULT 0,
  trashed_at       INTEGER,
  import_batch_id  INTEGER REFERENCES import_batch(id) ON DELETE SET NULL,
  created_at       INTEGER NOT NULL,
  updated_at       INTEGER NOT NULL,
  UNIQUE (fingerprint),
  UNIQUE (stable_id)
);
CREATE INDEX idx_ke_source_id     ON knowledge_entry(source_id);
CREATE INDEX idx_ke_is_archived   ON knowledge_entry(is_archived);
CREATE INDEX idx_ke_is_trashed    ON knowledge_entry(is_trashed);
CREATE INDEX idx_ke_is_bookmarked ON knowledge_entry(is_bookmarked);
```

- Matching logic: incoming hash == `imported_hash` OR incoming hash == `content_hash` -> `UNCHANGED`. Otherwise `CONFLICT` (held).
- Entries without stable IDs whose quote matches but whose metadata/insight/tags changed become `CONFLICT`, not `UNCHANGED`.
- In-app edit updates `content_hash` but leaves `imported_hash` untouched. Reimporting the original file sees incoming == `imported_hash` -> `UNCHANGED`.

---

### 1.4 `knowledge_tag`

```sql
CREATE TABLE knowledge_tag (
  id    INTEGER PRIMARY KEY AUTOINCREMENT,
  name  TEXT NOT NULL,
  UNIQUE (name)
);
```

---

### 1.5 `knowledge_entry_tag`

```sql
CREATE TABLE knowledge_entry_tag (
  entry_id  INTEGER NOT NULL REFERENCES knowledge_entry(id) ON DELETE CASCADE,
  tag_id    INTEGER NOT NULL REFERENCES knowledge_tag(id)   ON DELETE CASCADE,
  PRIMARY KEY (entry_id, tag_id)
);
CREATE INDEX idx_ket_tag_id ON knowledge_entry_tag(tag_id);
```

---

### 1.6 `knowledge_relationship`

```sql
CREATE TABLE knowledge_relationship (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  from_id    INTEGER NOT NULL REFERENCES knowledge_entry(id) ON DELETE CASCADE,
  to_id      INTEGER NOT NULL REFERENCES knowledge_entry(id) ON DELETE CASCADE,
  rel_type   TEXT NOT NULL DEFAULT 'RELATED',
  created_at INTEGER NOT NULL,
  CHECK (from_id < to_id),
  UNIQUE (from_id, to_id, rel_type)
);
CREATE INDEX idx_kr_to_id ON knowledge_relationship(to_id);
```

---

### 1.7 `import_batch`

```sql
CREATE TABLE import_batch (
  id                INTEGER PRIMARY KEY AUTOINCREMENT,
  filename          TEXT NOT NULL,
  content_hash      TEXT NOT NULL,
  started_at        INTEGER NOT NULL,
  finished_at       INTEGER,
  outcome           TEXT NOT NULL DEFAULT 'IN_PROGRESS', -- IN_PROGRESS, COMPLETED, FAILED
  entries_parsed    INTEGER NOT NULL DEFAULT 0,
  entries_new       INTEGER NOT NULL DEFAULT 0,
  entries_unchanged INTEGER NOT NULL DEFAULT 0,
  entries_conflict  INTEGER NOT NULL DEFAULT 0,
  entries_duplicate INTEGER NOT NULL DEFAULT 0,
  entries_invalid   INTEGER NOT NULL DEFAULT 0,
  entries_skipped   INTEGER NOT NULL DEFAULT 0,
  ids_attached      INTEGER NOT NULL DEFAULT 0,
  error_message     TEXT
);
```

---

### 1.8 `import_result`

```sql
CREATE TABLE import_result (
  id                INTEGER PRIMARY KEY AUTOINCREMENT,
  batch_id          INTEGER NOT NULL REFERENCES import_batch(id) ON DELETE CASCADE,
  entry_index       INTEGER NOT NULL,
  stable_id         TEXT,
  fingerprint       TEXT,
  classification    TEXT NOT NULL,
  existing_entry_id INTEGER REFERENCES knowledge_entry(id) ON DELETE SET NULL,
  inserted_entry_id INTEGER REFERENCES knowledge_entry(id) ON DELETE SET NULL,
  incoming_payload  TEXT,                                -- JSON of parsed entry for held rows
  incoming_hash     TEXT,                                -- content hash of incoming entry
  resolution_status TEXT NOT NULL DEFAULT 'PENDING',     -- PENDING, KEPT_EXISTING, APPLIED, SAVED_SEPARATE, SKIPPED
  error_message     TEXT,
  UNIQUE (batch_id, entry_index)
);
CREATE INDEX idx_ir_pending ON import_result(existing_entry_id, incoming_hash)
  WHERE resolution_status = 'PENDING';
```

- When storing held CONFLICT or POTENTIAL_DUPLICATE, if a row with `existing_entry_id` + `incoming_hash` already exists in `PENDING` status, do not create a duplicate PENDING row.

---

### 1.9 Database Configuration & Schema Export

- Schema version: **1**.
- Schema export directory: `app/schemas/` via annotation processor option `"room.schemaLocation": "$projectDir/schemas"`.
- Never use `fallbackToDestructiveMigration` anywhere.
- Foreign key enforcement: `PRAGMA foreign_keys = ON` executed in `RoomDatabase.Callback.onOpen`.

---

## 2 · Pure-Java Importer Architecture

The importer operates with **zero Android dependencies** (`android.*` and `org.json.*` imports prohibited in `domain/` and `importer/`). Uses `com.google.code.gson:gson:2.12.1`.

---

## 3 · Test Plan (Total: 111 New Tests + 2 ClockTests = 113 Tests)

### 3.1 Plain JUnit4 Tests (Zero Android Dependencies) — 73 Tests

- **`MarkdownParserTest` (22 tests):** P-01…P-22
- **`EntryNormalizerTest` (9 tests):** N-01…N-09
- **`FingerprintCalculatorTest` (8 tests):** F-01…F-08 (F-08: same quote under "Deep Work" and "deep work" yields same fingerprint)
- **`ContentHashCalculatorTest` (5 tests):** CH-01…CH-05
- **`JaccardCalculatorTest` (5 tests):** J-01…J-05
- **`EntryMatcherTest` (16 tests):** M-01…M-16 (M-07: edit in app + reimport = UNCHANGED; M-08: stable_id matches X, fingerprint matches Y = CONFLICT; M-06: file edited without ID = CONFLICT)
- **`EntryClassifierTest` (7 tests):** CL-01…CL-07
- **`ArchitectureRulesTest` (1 test):** ARCH-01 (asserts zero `android.*` or `org.json.*` imports in domain/ and importer/)

### 3.2 Robolectric & Room Tests (`ImportCommitterTest`) — 38 Tests

- IC-01…IC-05
- IC-06: user edits, archived state, and trashed state survive reimport of original file
- IC-07…IC-33
- IC-34: fileEditedWithoutId_quoteMatches_metadataChanged_conflict
- IC-35: editInApp_thenReimportUnchangedFile_unchanged (incoming == imported_hash)
- IC-36: dbChangedBetweenPreviewAndCommit_rerunsMatching_noCrashReportMatchesReality
- IC-37: importConflictingFileTwice_doesNotCreateSecondPendingRow
- IC-38: stableIdMatchesEntryX_fingerprintMatchesEntryY_conflictNoWriteNoConstraintViolation

---

## 4 · Build Order

1. Toolchain & config: Add Gson, Robolectric, Room compiler args, remove `returnDefaultValues`, add `robolectric.properties`.
2. Write test fixtures in `app/src/test/resources/fixtures/`.
3. Implement `MarkdownParser` test-first.
4. Implement `EntryNormalizer` test-first.
5. Implement `FingerprintCalculator` and `ContentHashCalculator` test-first.
6. Implement `JaccardCalculator` test-first.
7. Implement `EntryMatcher` and `EntryClassifier` test-first.
8. Architecture check test.
9. Room entities, DAOs, and `AppDatabase`.
10. Implement `ImportCommitter` test-first with Robolectric.
11. Run `./gradlew test` and `./gradlew assembleDebug`.
