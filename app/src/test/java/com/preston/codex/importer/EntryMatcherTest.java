package com.preston.codex.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.preston.codex.importer.model.ExistingEntry;
import com.preston.codex.importer.model.ImportClassification;
import com.preston.codex.importer.model.MatchResult;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntryMatcherTest {

    private InMemoryMatchLookup lookup;
    private EntryMatcher matcher;

    @Before
    public void setUp() {
        lookup = new InMemoryMatchLookup();
        matcher = new EntryMatcher(lookup, new JaccardCalculator());
    }

    @Test
    public void M01_stableId_exactMatch_sameContent_unchanged() {
        ExistingEntry existing = new ExistingEntry(1L, 10L, "id-1", "fp-1", "hash-1", "hash-1", "Quote text", false, false);
        lookup.add(existing);

        MatchResult result = matcher.match("id-1", "fp-1", "hash-1", "Quote text");
        assertEquals(ImportClassification.UNCHANGED, result.getClassification());
        assertEquals(Long.valueOf(1L), result.getExistingEntryId());
    }

    @Test
    public void M02_stableId_exactMatch_differentContent_conflict() {
        ExistingEntry existing = new ExistingEntry(1L, 10L, "id-1", "fp-1", "hash-original", "hash-original", "Quote text", false, false);
        lookup.add(existing);

        MatchResult result = matcher.match("id-1", "fp-1", "hash-edited-file", "Quote text edited");
        assertEquals(ImportClassification.CONFLICT, result.getClassification());
        assertEquals(Long.valueOf(1L), result.getExistingEntryId());
    }

    @Test
    public void M03_fingerprint_exactMatch_sameContent_unchanged() {
        ExistingEntry existing = new ExistingEntry(1L, 10L, null, "fp-1", "hash-1", "hash-1", "Quote text", false, false);
        lookup.add(existing);

        MatchResult result = matcher.match(null, "fp-1", "hash-1", "Quote text");
        assertEquals(ImportClassification.UNCHANGED, result.getClassification());
        assertEquals(Long.valueOf(1L), result.getExistingEntryId());
    }

    @Test
    public void M04_fingerprintMatch_existingStableIdNull_incomingHasId_attachesId() {
        ExistingEntry existing = new ExistingEntry(1L, 10L, null, "fp-1", "hash-1", "hash-1", "Quote text", false, false);
        lookup.add(existing);

        MatchResult result = matcher.match("incoming-id-1", "fp-1", "hash-1", "Quote text");
        assertEquals(ImportClassification.ID_ATTACHED, result.getClassification());
        assertEquals(Long.valueOf(1L), result.getExistingEntryId());
    }

    @Test
    public void M05_fingerprintMatch_existingHasDifferentStableId_conflict() {
        ExistingEntry existing = new ExistingEntry(1L, 10L, "existing-id", "fp-1", "hash-1", "hash-1", "Quote text", false, false);
        lookup.add(existing);

        MatchResult result = matcher.match("different-incoming-id", "fp-1", "hash-1", "Quote text");
        assertEquals(ImportClassification.CONFLICT, result.getClassification());
        assertEquals(Long.valueOf(1L), result.getExistingEntryId());
    }

    @Test
    public void M06_fingerprintMatch_quoteMatches_metadataChanged_conflict() {
        // Amendment 1: Entry without stable ID whose quote matches but metadata changed -> CONFLICT
        ExistingEntry existing = new ExistingEntry(1L, 10L, null, "fp-1", "hash-original", "hash-original", "Quote text", false, false);
        lookup.add(existing);

        MatchResult result = matcher.match(null, "fp-1", "hash-metadata-changed", "Quote text");
        assertEquals(ImportClassification.CONFLICT, result.getClassification());
    }

    @Test
    public void M07_fingerprintOrStableIdMatch_userEditedInApp_reimportUnchangedFile_unchanged() {
        // Amendment 1: user edited in app, so content_hash is updated, but imported_hash remains hash-original.
        // Incoming hash == imported_hash -> UNCHANGED!
        ExistingEntry existing = new ExistingEntry(1L, 10L, "id-1", "fp-1", "hash-in-app-edit", "hash-original", "Edited quote", false, false);
        lookup.add(existing);

        MatchResult result = matcher.match("id-1", "fp-1", "hash-original", "Original quote");
        assertEquals(ImportClassification.UNCHANGED, result.getClassification());
    }

    @Test
    public void M08_incomingStableIdMatchesEntryX_fingerprintMatchesEntryY_conflict() {
        // Amendment 6: stable_id matches entry 1, fingerprint matches entry 2 -> CONFLICT
        ExistingEntry entryX = new ExistingEntry(1L, 10L, "shared-id", "fp-x", "hash-x", "hash-x", "Quote X", false, false);
        ExistingEntry entryY = new ExistingEntry(2L, 10L, "other-id", "fp-y", "hash-y", "hash-y", "Quote Y", false, false);
        lookup.add(entryX);
        lookup.add(entryY);

        MatchResult result = matcher.match("shared-id", "fp-y", "hash-incoming", "Quote Y");
        assertEquals(ImportClassification.CONFLICT, result.getClassification());
    }

    @Test
    public void M09_highJaccard_noFingerprintMatch_potentialDuplicate() {
        ExistingEntry existing = new ExistingEntry(1L, 10L, "id-1", "fp-old", "hash-old", "hash-old",
                "Clarity about what matters provides clarity about what does not", false, false);
        lookup.add(existing);

        // Near duplicate quote (Jaccard >= 0.85)
        String incomingQuote = "Clarity about what matters provides extreme clarity about what does not";
        MatchResult result = matcher.match(null, "fp-different", "hash-new", incomingQuote);
        assertEquals(ImportClassification.POTENTIAL_DUPLICATE, result.getClassification());
        assertEquals(Long.valueOf(1L), result.getExistingEntryId());
    }

    @Test
    public void M10_lowJaccard_returnsNew() {
        ExistingEntry existing = new ExistingEntry(1L, 10L, "id-1", "fp-old", "hash-old", "hash-old",
                "Apples and oranges are fruit", false, false);
        lookup.add(existing);

        MatchResult result = matcher.match(null, "fp-new", "hash-new", "Deep work is valuable");
        assertEquals(ImportClassification.NEW, result.getClassification());
        assertNull(result.getExistingEntryId());
    }

    @Test
    public void M11_differentSources_highJaccard_potentialDuplicate() {
        ExistingEntry existing = new ExistingEntry(1L, 10L, "id-1", "fp-source-1", "hash-1", "hash-1",
                "The very quick brown fox jumps over the lazy dog in the green peaceful summer meadow", false, false);
        lookup.add(existing);

        String incomingQuote = "The very quick brown fox jumps over the lazy dog in the green peaceful sunny meadow";
        MatchResult result = matcher.match(null, "fp-source-2", "hash-2", incomingQuote);
        assertEquals(ImportClassification.POTENTIAL_DUPLICATE, result.getClassification());
    }

    @Test
    public void M12_emptyDatabase_alwaysNew() {
        MatchResult result = matcher.match("new-id", "fp-new", "hash-new", "Brand new quote");
        assertEquals(ImportClassification.NEW, result.getClassification());
    }

    @Test
    public void M13_intraBatch_duplicateFingerprint_firstNewSecondDuplicate() {
        // Amendment 1: identical entries in one file -> DUPLICATE (only first can be NEW)
        MatchResult first = matcher.match(null, "fp-same", "hash-same", "Quote");
        assertEquals(ImportClassification.NEW, first.getClassification());

        MatchResult second = matcher.match(null, "fp-same", "hash-same", "Quote");
        assertEquals(ImportClassification.DUPLICATE, second.getClassification());
    }

    @Test
    public void M14_intraBatch_duplicateStableId_producesInvalidWithClearMessage() {
        // Amendment 1: repeated stable_id in one file -> INVALID with clear message
        MatchResult first = matcher.match("id-dupe", "fp-1", "hash-1", "Quote 1");
        assertEquals(ImportClassification.NEW, first.getClassification());

        MatchResult second = matcher.match("id-dupe", "fp-2", "hash-2", "Quote 2");
        assertEquals(ImportClassification.INVALID, second.getClassification());
        assertTrue(second.getErrorMessage().contains("id-dupe"));
    }

    @Test
    public void M15_matchesArchivedEntry_returnsUnchanged() {
        // Amendment 5: matching includes archived entries
        ExistingEntry archived = new ExistingEntry(1L, 10L, "id-1", "fp-1", "hash-1", "hash-1", "Quote", true, false);
        lookup.add(archived);

        MatchResult result = matcher.match("id-1", "fp-1", "hash-1", "Quote");
        assertEquals(ImportClassification.UNCHANGED, result.getClassification());
    }

    @Test
    public void M16_matchesTrashedEntry_returnsUnchangedWithNote_neverResurrects() {
        // Amendment 5: match to trashed entry is UNCHANGED with note "exists in trash"
        ExistingEntry trashed = new ExistingEntry(1L, 10L, "id-1", "fp-1", "hash-1", "hash-1", "Quote", false, true);
        lookup.add(trashed);

        MatchResult result = matcher.match("id-1", "fp-1", "hash-1", "Quote");
        assertEquals(ImportClassification.UNCHANGED, result.getClassification());
        assertNotNull(result.getNote());
        assertTrue(result.getNote().contains("exists in trash"));
    }

    private static class InMemoryMatchLookup implements MatchLookup {
        private final Map<String, ExistingEntry> byStableId = new HashMap<>();
        private final Map<String, ExistingEntry> byFingerprint = new HashMap<>();
        private final List<ExistingEntry> all = new ArrayList<>();

        void add(ExistingEntry entry) {
            if (entry.getStableId() != null) {
                byStableId.put(entry.getStableId(), entry);
            }
            if (entry.getFingerprint() != null) {
                byFingerprint.put(entry.getFingerprint(), entry);
            }
            all.add(entry);
        }

        @Override
        public ExistingEntry findByStableId(String stableId) {
            return byStableId.get(stableId);
        }

        @Override
        public ExistingEntry findByFingerprint(String fingerprint) {
            return byFingerprint.get(fingerprint);
        }

        @Override
        public List<ExistingEntry> findAllCandidates() {
            return all;
        }
    }
}
