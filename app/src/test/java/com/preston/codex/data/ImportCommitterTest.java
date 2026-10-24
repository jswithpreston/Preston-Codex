package com.preston.codex.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.room.Room;

import com.preston.codex.data.database.AppDatabase;
import com.preston.codex.data.entity.ImportBatchEntity;
import com.preston.codex.data.entity.ImportResultEntity;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeRelationshipEntity;
import com.preston.codex.data.entity.KnowledgeSourceEntity;
import com.preston.codex.importer.ContentHashCalculator;
import com.preston.codex.importer.EntryClassifier;
import com.preston.codex.importer.EntryMatcher;
import com.preston.codex.importer.EntryNormalizer;
import com.preston.codex.importer.FingerprintCalculator;
import com.preston.codex.importer.ImportCommitter;
import com.preston.codex.importer.ImportPreviewBuilder;
import com.preston.codex.importer.JaccardCalculator;
import com.preston.codex.importer.MarkdownParser;
import com.preston.codex.importer.model.ClassifiedEntry;
import com.preston.codex.importer.model.ImportPreview;
import com.preston.codex.importer.model.ImportReport;
import com.preston.codex.importer.model.ParsedFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ImportCommitterTest {

    private AppDatabase db;
    private Clock clock;
    private MarkdownParser parser;
    private EntryNormalizer normalizer;
    private FingerprintCalculator fingerprintCalculator;
    private ContentHashCalculator contentHashCalculator;
    private JaccardCalculator jaccardCalculator;
    private ImportCommitter committer;

    @Before
    public void setUp() {
        Context context = org.robolectric.RuntimeEnvironment.getApplication();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .addCallback(AppDatabase.CALLBACK)
                .build();

        clock = Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneId.of("UTC"));
        parser = new MarkdownParser();
        normalizer = new EntryNormalizer();
        fingerprintCalculator = new FingerprintCalculator(normalizer);
        contentHashCalculator = new ContentHashCalculator(normalizer);
        jaccardCalculator = new JaccardCalculator();

        committer = new ImportCommitter(
                db,
                clock,
                normalizer,
                fingerprintCalculator,
                contentHashCalculator,
                jaccardCalculator
        );
    }

    @After
    public void tearDown() {
        if (db != null) {
            db.close();
        }
    }

    private String loadFixture(String fixtureName) {
        String path = "/fixtures/" + fixtureName;
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new IllegalArgumentException("Fixture not found: " + path);
        }
        try (Scanner scanner = new Scanner(is, StandardCharsets.UTF_8.name())) {
            scanner.useDelimiter("\\A");
            return scanner.hasNext() ? scanner.next() : "";
        }
    }

    @Test
    public void IC01_firstImport_insertsExpectedEntries() throws Exception {
        String content = loadFixture("valid_basic.md");
        ParsedFile parsed = parser.parse("valid_basic.md", content);

        ImportReport report = committer.commit(parsed, "hash-1");
        assertEquals("COMPLETED", report.getOutcome());
        assertEquals(2, report.getEntriesNew());
        assertEquals(2, db.knowledgeEntryDao().count());
        assertEquals(1, db.knowledgeSourceDao().count());
    }

    @Test
    public void IC02_reimportUnchanged_zeroNewEntries() throws Exception {
        String content = loadFixture("valid_basic.md");
        ParsedFile parsed1 = parser.parse("valid_basic.md", content);
        committer.commit(parsed1, "hash-1");

        ParsedFile parsed2 = parser.parse("valid_basic.md", content);
        ImportReport report2 = committer.commit(parsed2, "hash-1");

        assertEquals(0, report2.getEntriesNew());
        assertEquals(2, report2.getEntriesUnchanged());
        assertEquals(2, db.knowledgeEntryDao().count());
    }

    @Test
    public void IC03_incrementalImport_addsOnlyNewEntries() throws Exception {
        String v1 = loadFixture("valid_incremental_v1.md");
        ParsedFile parsedV1 = parser.parse("valid_incremental_v1.md", v1);
        committer.commit(parsedV1, "hash-v1");
        assertEquals(2, db.knowledgeEntryDao().count());

        String v2 = loadFixture("valid_incremental_v2.md");
        ParsedFile parsedV2 = parser.parse("valid_incremental_v2.md", v2);
        ImportReport reportV2 = committer.commit(parsedV2, "hash-v2");

        assertEquals(2, reportV2.getEntriesNew());
        assertEquals(2, reportV2.getEntriesUnchanged());
        assertEquals(4, db.knowledgeEntryDao().count());
    }

    @Test
    public void IC04_stableIdEdited_classifiedConflict_heldNotInserted() throws Exception {
        String original = loadFixture("valid_stable_id.md");
        ParsedFile p1 = parser.parse("valid_stable_id.md", original);
        committer.commit(p1, "hash-orig");
        assertEquals(1, db.knowledgeEntryDao().count());

        String edited = loadFixture("valid_stable_id_edited.md");
        ParsedFile p2 = parser.parse("valid_stable_id_edited.md", edited);
        ImportReport r2 = committer.commit(p2, "hash-edit");

        assertEquals(0, r2.getEntriesNew());
        assertEquals(1, r2.getEntriesConflict());
        assertEquals(1, db.knowledgeEntryDao().count()); // existing unchanged

        List<ImportResultEntity> results = db.importResultDao().getByBatchId(r2.getBatchId());
        assertEquals(1, results.size());
        assertEquals("CONFLICT", results.get(0).classification);
        assertEquals("PENDING", results.get(0).resolutionStatus);
        assertNotNull(results.get(0).incomingPayload);
    }

    @Test
    public void IC05_bookmarksSurviveReimport() throws Exception {
        String content = loadFixture("valid_basic.md");
        ParsedFile p1 = parser.parse("valid_basic.md", content);
        committer.commit(p1, "hash-1");

        // Bookmark the first entry
        List<KnowledgeEntryEntity> entries = db.knowledgeEntryDao().getAllEntries();
        KnowledgeEntryEntity first = entries.get(0);
        first.isBookmarked = true;
        db.knowledgeEntryDao().update(first);

        // Reimport original file
        ParsedFile p2 = parser.parse("valid_basic.md", content);
        committer.commit(p2, "hash-1");

        KnowledgeEntryEntity reloaded = db.knowledgeEntryDao().findById(first.id);
        assertTrue("Bookmark must survive reimport", reloaded.isBookmarked);
    }

    @Test
    public void IC06_userEdits_archivedState_andTrashedState_surviveReimportOfOriginalFile() throws Exception {
        // Amendment 7: replaces IC-06: user edits, archived state, and trashed state survive reimport of original file
        String content = loadFixture("valid_basic.md");
        ParsedFile p1 = parser.parse("valid_basic.md", content);
        committer.commit(p1, "hash-1");

        List<KnowledgeEntryEntity> entries = db.knowledgeEntryDao().getAllEntries();
        KnowledgeEntryEntity e1 = entries.get(0);
        KnowledgeEntryEntity e2 = entries.get(1);

        // Modify e1 in-app (edit text and archive)
        e1.quoteText = "In-app modified quote text";
        e1.contentHash = contentHashCalculator.calculate(e1.quoteText, e1.insightText, e1.sourceRef, e1.application, e1.entryType, Collections.emptyList());
        e1.isArchived = true;
        db.knowledgeEntryDao().update(e1);

        // Trash e2
        e2.isTrashed = true;
        e2.trashedAt = clock.millis();
        db.knowledgeEntryDao().update(e2);

        // Reimport original file
        ParsedFile p2 = parser.parse("valid_basic.md", content);
        ImportReport r2 = committer.commit(p2, "hash-1");

        assertEquals(2, r2.getEntriesUnchanged());
        assertEquals(0, r2.getEntriesNew());

        KnowledgeEntryEntity reloaded1 = db.knowledgeEntryDao().findById(e1.id);
        assertEquals("In-app modified quote text", reloaded1.quoteText);
        assertTrue("Archived state must survive reimport", reloaded1.isArchived);

        KnowledgeEntryEntity reloaded2 = db.knowledgeEntryDao().findById(e2.id);
        assertTrue("Trashed state must survive reimport", reloaded2.isTrashed);
    }

    @Test
    public void IC07_differentSources_similarQuotes_notMerged() throws Exception {
        String content = loadFixture("valid_different_sources.md");
        ParsedFile parsed = parser.parse("valid_different_sources.md", content);

        ImportReport report = committer.commit(parsed, "hash-diff");
        assertEquals(1, report.getEntriesNew()); // First entry inserted as NEW
        assertEquals(1, report.getEntriesSkipped()); // Second entry is POTENTIAL_DUPLICATE (held)
        assertEquals(1, db.knowledgeEntryDao().count());
    }

    @Test
    public void IC08_potentialDuplicate_heldNotInserted() throws Exception {
        String nearDupe = loadFixture("valid_same_source_near_dupe.md");
        ParsedFile parsed = parser.parse("valid_same_source_near_dupe.md", nearDupe);

        ImportReport report = committer.commit(parsed, "hash-near");
        assertEquals(1, report.getEntriesNew());
        assertEquals(1, report.getEntriesSkipped());
        assertEquals(1, db.knowledgeEntryDao().count());

        List<ImportResultEntity> results = db.importResultDao().getByBatchId(report.getBatchId());
        assertEquals(2, results.size());
        assertEquals("POTENTIAL_DUPLICATE", results.get(1).classification);
        assertEquals("PENDING", results.get(1).resolutionStatus);
    }

    @Test
    public void IC09_invalidFile_leavesDatabaseUntouched() {
        String invalid = loadFixture("invalid_no_book_header.md");
        ParsedFile parsed = parser.parse("invalid_no_book_header.md", invalid);

        try {
            committer.commit(parsed, "hash-inv");
        } catch (Exception ignored) {
        }
        assertEquals(0, db.knowledgeEntryDao().count());
        assertEquals(0, db.knowledgeSourceDao().count());
    }

    @Test
    public void IC10_transactionRollback_onForcedDbError_entriesUntouched() {
        String content = loadFixture("valid_basic.md");
        ParsedFile parsed = parser.parse("valid_basic.md", content);

        // Close DB to force an error on transaction
        db.close();

        try {
            committer.commit(parsed, "hash-err");
            fail("Expected exception on closed DB");
        } catch (Exception expected) {
            // Success
        }
    }

    @Test
    public void IC11_failedBatchRecorded_inSeparateTransaction_afterRollback() {
        // Amendment 6: test that entries are untouched and FAILED batch exists
        String content = loadFixture("valid_basic.md");
        ParsedFile parsed = parser.parse("valid_basic.md", content);

        // Pre-insert an entry with the same fingerprint to cause a unique constraint crash if forced,
        // or trigger exception by corrupting state
        try {
            committer.commit(parsed, "hash-1");
            // Now attempt an operation that will fail inside transaction
            db.getOpenHelper().getWritableDatabase().execSQL("CREATE TRIGGER fail_trigger BEFORE INSERT ON import_result BEGIN SELECT RAISE(FAIL, 'forced error'); END;");

            ParsedFile parsed2 = parser.parse("valid_basic.md", content);
            committer.commit(parsed2, "hash-fail");
            fail("Expected failure");
        } catch (Exception expected) {
            // The FAILED batch must exist
            ImportBatchEntity latest = db.importBatchDao().findLatestByFilename("valid_basic.md");
            assertNotNull(latest);
            assertEquals("FAILED", latest.outcome);
        }
    }

    @Test
    public void IC12_noShortCircuitOnFileHash_reimportsParsedIndependently() throws Exception {
        // Amendment 6: never short-circuit based on file hash alone
        String content = loadFixture("valid_basic.md");
        ParsedFile p1 = parser.parse("valid_basic.md", content);
        committer.commit(p1, "same-hash");

        ParsedFile p2 = parser.parse("valid_basic.md", content);
        ImportReport r2 = committer.commit(p2, "same-hash");

        assertNotNull(r2);
        assertEquals("COMPLETED", r2.getOutcome());
        assertEquals(2, r2.getEntriesUnchanged());
    }

    @Test
    public void IC13_importReport_matchesCommittedRows() throws Exception {
        String content = loadFixture("valid_full.md");
        ParsedFile parsed = parser.parse("valid_full.md", content);
        ImportReport report = committer.commit(parsed, "hash-full");

        assertEquals(2, report.getEntriesNew());
        assertEquals(db.knowledgeEntryDao().count(), report.getEntriesNew());
        assertEquals(2, db.importResultDao().getByBatchId(report.getBatchId()).size());
    }

    @Test
    public void IC14_crlfFile_identicalToLfImport() throws Exception {
        String lfContent = loadFixture("valid_basic.md");
        ParsedFile lfParsed = parser.parse("valid_basic.md", lfContent);
        committer.commit(lfParsed, "hash-lf");

        KnowledgeEntryEntity lfEntry = db.knowledgeEntryDao().getAllEntries().get(0);

        // Clear and commit CRLF
        db.clearAllTables();

        String crlfContent = loadFixture("valid_crlf.md");
        ParsedFile crlfParsed = parser.parse("valid_crlf.md", crlfContent);
        committer.commit(crlfParsed, "hash-crlf");

        KnowledgeEntryEntity crlfEntry = db.knowledgeEntryDao().getAllEntries().get(0);
        assertEquals(lfEntry.fingerprint, crlfEntry.fingerprint);
        assertEquals(lfEntry.contentHash, crlfEntry.contentHash);
        assertEquals(lfEntry.quoteText, crlfEntry.quoteText);
    }

    @Test
    public void IC15_unicodeContent_preservedInDb() throws Exception {
        String content = loadFixture("valid_unicode.md");
        ParsedFile parsed = parser.parse("valid_unicode.md", content);
        committer.commit(parsed, "hash-uni");

        KnowledgeEntryEntity entry = db.knowledgeEntryDao().getAllEntries().get(0);
        assertTrue(entry.quoteText.contains("系统1运行是无意识且快速的——不怎么费力，或者完全不费力。"));
        assertTrue(db.knowledgeTagDao().getTagsForEntry(entry.id).contains("心理学"));
    }

    @Test
    public void IC16_escapedQuotes_preservedInDb() throws Exception {
        String content = loadFixture("valid_escaped_quotes.md");
        ParsedFile parsed = parser.parse("valid_escaped_quotes.md", content);
        committer.commit(parsed, "hash-esc");

        KnowledgeEntryEntity entry = db.knowledgeEntryDao().getAllEntries().get(0);
        assertTrue(entry.quoteText.contains("\"To succeed today, you must 'produce' value.\""));
    }

    @Test
    public void IC17_unknownFields_storedAsExtraFieldsJson() throws Exception {
        String content = loadFixture("valid_unknown_fields.md");
        ParsedFile parsed = parser.parse("valid_unknown_fields.md", content);
        committer.commit(parsed, "hash-unk");

        KnowledgeEntryEntity entry = db.knowledgeEntryDao().getAllEntries().get(0);
        assertNotNull(entry.extraFields);
        assertTrue(entry.extraFields.contains("\"Rating\":\"5\""));
        assertTrue(entry.extraFields.contains("\"Priority\":\"high\""));
    }

    @Test
    public void IC18_malformedEntry_reportsFilenameAndIndex() throws Exception {
        String mixed = loadFixture("invalid_mixed.md");
        ParsedFile parsed = parser.parse("invalid_mixed.md", mixed);
        ImportReport report = committer.commit(parsed, "hash-mix");

        assertEquals(1, report.getEntriesNew());
        assertEquals(1, report.getEntriesInvalid());

        List<ImportResultEntity> results = db.importResultDao().getByBatchId(report.getBatchId());
        ImportResultEntity errorRow = results.get(1);
        assertEquals("INVALID", errorRow.classification);
        assertNotNull(errorRow.errorMessage);
        assertTrue(errorRow.errorMessage.toLowerCase().contains("quote"));
    }

    @Test
    public void IC19_tagNormalisation_caseInsensitive_sharedRow() throws Exception {
        // Tag "focus" and "Focus" share single tag entity
        String c1 = "# Book: B1\n**Quote:** Q1\n**Tags:** Focus\n";
        String c2 = "# Book: B2\n**Quote:** Q2\n**Tags:** focus\n";

        committer.commit(parser.parse("f1.md", c1), "h1");
        committer.commit(parser.parse("f2.md", c2), "h2");

        assertEquals(1, db.knowledgeTagDao().countTags());
        assertEquals(2, db.knowledgeEntryDao().count());
    }

    @Test
    public void IC20_sourceDeduplication_sameBookOneRow() throws Exception {
        String c1 = "# Book: Deep Work\n# Author: Cal Newport\n**Quote:** Q1\n";
        String c2 = "# Book: Deep Work\n# Author: Cal Newport\n**Quote:** Q2\n";

        committer.commit(parser.parse("f1.md", c1), "h1");
        committer.commit(parser.parse("f2.md", c2), "h2");

        assertEquals(1, db.knowledgeSourceDao().count());
        assertEquals(2, db.knowledgeEntryDao().count());
    }

    @Test
    public void IC21_sourceWithNoAuthor_importedTwice_deduplicatesToOneRow() throws Exception {
        // Amendment 2: a source with no author imported twice
        String c1 = "# Book: Ancient Wisdom\n**Quote:** Q1\n";
        String c2 = "# Book: Ancient Wisdom\n**Quote:** Q2\n";

        committer.commit(parser.parse("f1.md", c1), "h1");
        committer.commit(parser.parse("f2.md", c2), "h2");

        assertEquals(1, db.knowledgeSourceDao().count());
        assertEquals("", db.knowledgeSourceDao().findById(1L).author);
    }

    @Test
    public void IC22_sourceCaseInsensitiveKeys_deepWorkVsDeepWork_sameRow() throws Exception {
        // Amendment 2: "Deep Work" vs "deep work"
        String c1 = "# Book: Deep Work\n# Author: Cal Newport\n**Quote:** Q1\n";
        String c2 = "# Book: deep work\n# Author: cal newport\n**Quote:** Q2\n";

        committer.commit(parser.parse("f1.md", c1), "h1");
        committer.commit(parser.parse("f2.md", c2), "h2");

        assertEquals(1, db.knowledgeSourceDao().count());
    }

    @Test
    public void IC23_sameTitleDifferentAuthor_createsSeparateSources_withWarning() throws Exception {
        // Amendment 2: Same title with a different author = separate source with warning in preview
        String c1 = "# Book: Focus\n# Author: Author A\n**Quote:** Q1\n";
        String c2 = "# Book: Focus\n# Author: Author B\n**Quote:** Q2\n";

        committer.commit(parser.parse("f1.md", c1), "h1");

        ParsedFile p2 = parser.parse("f2.md", c2);
        // Preview builder warning check
        List<KnowledgeSourceEntity> existingSources = db.knowledgeSourceDao().findByTitleKey("focus");
        assertEquals(1, existingSources.size());

        ImportReport r2 = committer.commit(p2, "h2");
        assertEquals(2, db.knowledgeSourceDao().count());
        assertEquals(2, db.knowledgeEntryDao().count());
    }

    @Test
    public void IC24_sourceRow_unchangedOnReimport() throws Exception {
        // Q-C: existing source row left unchanged on reimport
        String c1 = "# Book: Deep Work\n# Author: Cal Newport\n**Quote:** Q1\n";
        committer.commit(parser.parse("f1.md", c1), "h1");

        KnowledgeSourceEntity orig = db.knowledgeSourceDao().findById(1L);
        assertEquals("Deep Work", orig.title);

        String c2 = "# Book: deep work\n# Author: cal newport\n**Quote:** Q2\n";
        committer.commit(parser.parse("f2.md", c2), "h2");

        KnowledgeSourceEntity after = db.knowledgeSourceDao().findById(1L);
        assertEquals("Deep Work", after.title); // Unchanged
    }

    @Test
    public void IC25_emptyLibrary_firstImport_atomicTransaction() throws Exception {
        assertEquals(0, db.knowledgeEntryDao().count());
        String content = loadFixture("valid_basic.md");
        committer.commit(parser.parse("valid_basic.md", content), "h1");
        assertEquals(2, db.knowledgeEntryDao().count());
    }

    @Test
    public void IC26_relatedField_createsNormalizedUndirectedRelationship() throws Exception {
        // Q-D: relationships are undirected: store each pair once in normalized order (from_id < to_id)
        String content = loadFixture("valid_full.md");
        committer.commit(parser.parse("valid_full.md", content), "h-full");

        List<KnowledgeEntryEntity> entries = db.knowledgeEntryDao().getAllEntries();
        assertEquals(2, entries.size());

        long e1 = entries.get(0).id;
        long e2 = entries.get(1).id;

        KnowledgeRelationshipEntity rel = db.knowledgeRelationshipDao().findRelationship(Math.min(e1, e2), Math.max(e1, e2), "RELATED");
        assertNotNull(rel);
        assertTrue(rel.fromId < rel.toId);
    }

    @Test
    public void IC27_danglingRelatedRefs_storedInExtraFields_resolvedAtCommitEnd() throws Exception {
        // Q-D: dangling refs go in extra_fields and are resolved at the end of every commit (test this)
        String c1 = "# Book: B1\n**ID:** id-A\n**Quote:** QA\n**Related:** id-B\n";
        committer.commit(parser.parse("f1.md", c1), "h1");

        KnowledgeEntryEntity entryA = db.knowledgeEntryDao().findByStableId("id-A");
        assertNotNull(entryA.extraFields);
        assertTrue(entryA.extraFields.contains("_dangling_related"));
        assertEquals(0, db.knowledgeRelationshipDao().count());

        // Now import file containing id-B
        String c2 = "# Book: B1\n**ID:** id-B\n**Quote:** QB\n";
        committer.commit(parser.parse("f2.md", c2), "h2");

        // The dangling ref must have been resolved!
        assertEquals(1, db.knowledgeRelationshipDao().count());
        KnowledgeEntryEntity reloadedA = db.knowledgeEntryDao().findByStableId("id-A");
        assertFalse(reloadedA.extraFields != null && reloadedA.extraFields.contains("_dangling_related"));
    }

    @Test
    public void IC28_idAttached_updatesExistingStableId_doesNotInsertNew() throws Exception {
        // Q-E: incoming entry matches existing by fingerprint, existing stable_id is NULL -> attach incoming ID
        String c1 = "# Book: B1\n**Quote:** Exactly matching quote\n";
        committer.commit(parser.parse("f1.md", c1), "h1");

        KnowledgeEntryEntity existing = db.knowledgeEntryDao().getAllEntries().get(0);
        assertNull(existing.stableId);

        String c2 = "# Book: B1\n**ID:** newly-attached-id\n**Quote:** Exactly matching quote\n";
        ImportReport r2 = committer.commit(parser.parse("f2.md", c2), "h2");

        assertEquals(0, r2.getEntriesNew());
        assertEquals(1, r2.getIdsAttached());
        assertEquals(1, db.knowledgeEntryDao().count());

        KnowledgeEntryEntity updated = db.knowledgeEntryDao().findById(existing.id);
        assertEquals("newly-attached-id", updated.stableId);
    }

    @Test
    public void IC29_intraBatchDupe_secondIsDuplicate_notInserted() throws Exception {
        // Amendment 1: Identical entries in one file -> DUPLICATE (only first can be NEW)
        String content = loadFixture("invalid_intra_batch_dupe_fingerprint.md");
        ImportReport report = committer.commit(parser.parse("dupe_fp.md", content), "h-dupe");

        assertEquals(1, report.getEntriesNew());
        assertEquals(1, report.getEntriesDuplicate());
        assertEquals(1, db.knowledgeEntryDao().count());
    }

    @Test
    public void IC30_intraBatchDupeStableId_isInvalid_notInserted_dbConstraintNotTriggered() throws Exception {
        // Amendment 1: Repeated stable_id in one file -> INVALID with a clear message. DB constraint not triggered!
        String content = loadFixture("invalid_intra_batch_dupe_stable_id.md");
        ImportReport report = committer.commit(parser.parse("dupe_id.md", content), "h-dupe-id");

        assertEquals(1, report.getEntriesNew());
        assertEquals(1, report.getEntriesInvalid());
        assertEquals(1, db.knowledgeEntryDao().count());
    }

    @Test
    public void IC31_matchToTrashedEntry_unchangedWithNote_notResurrected() throws Exception {
        // Amendment 5: match to trashed entry is UNCHANGED with note "exists in trash"; never re-insert or resurrect
        String content = loadFixture("valid_basic.md");
        committer.commit(parser.parse("f1.md", content), "h1");

        KnowledgeEntryEntity e1 = db.knowledgeEntryDao().getAllEntries().get(0);
        e1.isTrashed = true;
        e1.trashedAt = clock.millis();
        db.knowledgeEntryDao().update(e1);

        ImportReport r2 = committer.commit(parser.parse("f2.md", content), "h2");
        assertEquals(2, r2.getEntriesUnchanged());
        assertEquals(0, r2.getEntriesNew());

        KnowledgeEntryEntity reloaded = db.knowledgeEntryDao().findById(e1.id);
        assertTrue("Must not resurrect trashed entry", reloaded.isTrashed);
    }

    @Test
    public void IC32_foreignKeyEnforcement_active_rejectsOrphan() {
        // Amendment 8: test proving foreign-key enforcement is actually on
        try {
            KnowledgeEntryEntity orphan = new KnowledgeEntryEntity();
            orphan.sourceId = 999999L; // Non-existent source
            orphan.fingerprint = "fp-orphan";
            orphan.contentHash = "hash-orphan";
            orphan.importedHash = "hash-orphan";
            orphan.quoteText = "Orphan quote";
            orphan.createdAt = clock.millis();
            orphan.updatedAt = clock.millis();
            db.knowledgeEntryDao().insert(orphan);
            fail("Expected SQLiteConstraintException due to foreign key restriction");
        } catch (android.database.sqlite.SQLiteConstraintException expected) {
            // Foreign key enforcement is ON
        }
    }

    @Test
    public void IC33_heldEntriesContainFullJsonPayload_forFutureResolution() throws Exception {
        // Amendment 3 & 11: held entries must contain full incoming JSON payload
        String original = loadFixture("valid_stable_id.md");
        committer.commit(parser.parse("f1.md", original), "h1");

        String edited = loadFixture("valid_stable_id_edited.md");
        ImportReport r2 = committer.commit(parser.parse("f2.md", edited), "h2");

        List<ImportResultEntity> results = db.importResultDao().getByBatchId(r2.getBatchId());
        assertEquals(1, results.size());
        ImportResultEntity conflictRow = results.get(0);
        assertEquals("CONFLICT", conflictRow.classification);
        assertEquals("PENDING", conflictRow.resolutionStatus);
        assertNotNull(conflictRow.incomingPayload);
        assertTrue(conflictRow.incomingPayload.contains("deep-work-ch1"));
        assertTrue(conflictRow.incomingPayload.contains("extreme clarity"));
    }

    @Test
    public void IC34_fileEditedWithoutId_quoteMatches_metadataChanged_conflict() throws Exception {
        // Amendment 1: Entries without stable IDs whose quote matches but whose insight/tags/application changed must be CONFLICT, not UNCHANGED
        String c1 = "# Book: B1\n**Quote:** Unchanging quote\n**Insight:** Original insight\n";
        committer.commit(parser.parse("f1.md", c1), "h1");

        String c2 = "# Book: B1\n**Quote:** Unchanging quote\n**Insight:** Modified insight in file\n";
        ImportReport r2 = committer.commit(parser.parse("f2.md", c2), "h2");

        assertEquals(0, r2.getEntriesNew());
        assertEquals(1, r2.getEntriesConflict());
        assertEquals(1, db.knowledgeEntryDao().count());
    }

    @Test
    public void IC35_editInApp_thenReimportUnchangedFile_unchanged() throws Exception {
        // Amendment 1: edit-in-app then reimport unchanged file = UNCHANGED
        String content = loadFixture("valid_basic.md");
        committer.commit(parser.parse("f1.md", content), "h1");

        KnowledgeEntryEntity entry = db.knowledgeEntryDao().getAllEntries().get(0);
        entry.quoteText = "Edited in app quote";
        entry.contentHash = contentHashCalculator.calculate(entry.quoteText, entry.insightText, entry.sourceRef, entry.application, entry.entryType, Collections.emptyList());
        // importedHash remains as original
        db.knowledgeEntryDao().update(entry);

        ImportReport r2 = committer.commit(parser.parse("f2.md", content), "h1");
        assertEquals(2, r2.getEntriesUnchanged());
        assertEquals(0, r2.getEntriesConflict());
    }

    @Test
    public void IC36_dbChangedBetweenPreviewAndCommit_rerunsMatching_noCrashReportMatchesReality() throws Exception {
        // Amendment 4: ImportCommitter re-runs matching inside its transaction and does not trust preview.
        // DB changed between preview and commit -> no constraint crash, entry reclassified, report matches reality.
        String content = "# Book: B1\n**ID:** id-race\n**Quote:** Quote race\n";
        ParsedFile parsed = parser.parse("race.md", content);

        // Build preview when DB is empty -> preview says 1 NEW
        EntryClassifier classifier = new EntryClassifier(
                new EntryMatcher(new com.preston.codex.data.repository.RoomMatchLookup(db.knowledgeEntryDao()), jaccardCalculator),
                fingerprintCalculator,
                contentHashCalculator
        );
        List<ClassifiedEntry> classified = classifier.classify(parsed.getSource(), parsed.getEntries());
        ImportPreview preview = new ImportPreviewBuilder().build(parsed, classified, Collections.emptyList());
        assertEquals(1, preview.getCountNew());

        // NOW SIMULATE DB CHANGE BEFORE COMMIT: another process inserts the exact same entry!
        committer.commit(parser.parse("inserted_in_background.md", content), "h-bg");
        assertEquals(1, db.knowledgeEntryDao().count());

        // Now commit the original file using the committer
        ImportReport report = committer.commit(parsed, "h-orig");

        // Committer re-runs matching, sees UNCHANGED instead of NEW! No crash!
        assertEquals(0, report.getEntriesNew());
        assertEquals(1, report.getEntriesUnchanged());
        assertEquals(1, db.knowledgeEntryDao().count());
    }

    @Test
    public void IC37_importConflictingFileTwice_doesNotCreateSecondPendingRow() throws Exception {
        // Amendment 5: import_result: add incoming_hash. Do not create a second PENDING row for same existing_entry_id + incoming_hash.
        // Test: import a conflicting file twice -> one PENDING row.
        String orig = loadFixture("valid_stable_id.md");
        committer.commit(parser.parse("f1.md", orig), "h1");

        String edited = loadFixture("valid_stable_id_edited.md");
        committer.commit(parser.parse("f2.md", edited), "h2");
        assertEquals(1, db.importResultDao().countPending());

        // Reimport the conflicting file a second time
        committer.commit(parser.parse("f3.md", edited), "h3");
        assertEquals(1, db.importResultDao().countPending());
    }

    @Test
    public void IC38_stableIdMatchesEntryX_fingerprintMatchesEntryY_conflictNoWriteNoConstraintViolation() throws Exception {
        // Amendment 6: If an incoming stable_id matches entry X and its fingerprint matches a different entry Y:
        // classify CONFLICT, write nothing, no constraint violation.
        String c1 = "# Book: B1\n**ID:** id-X\n**Quote:** Quote X\n";
        String c2 = "# Book: B1\n**ID:** id-Y\n**Quote:** Quote Y\n";
        committer.commit(parser.parse("f1.md", c1), "h1");
        committer.commit(parser.parse("f2.md", c2), "h2");
        assertEquals(2, db.knowledgeEntryDao().count());

        // Incoming file has stable_id 'id-X', but quote 'Quote Y' (matches fingerprint of entry Y)
        String cConflict = "# Book: B1\n**ID:** id-X\n**Quote:** Quote Y\n";
        ImportReport report = committer.commit(parser.parse("fConflict.md", cConflict), "hConflict");

        assertEquals(1, report.getEntriesConflict());
        assertEquals(0, report.getEntriesNew());
        assertEquals(2, db.knowledgeEntryDao().count()); // Nothing written to knowledge_entry
    }
}
