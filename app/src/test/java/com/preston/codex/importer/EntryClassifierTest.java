package com.preston.codex.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.preston.codex.importer.model.ClassifiedEntry;
import com.preston.codex.importer.model.ExistingEntry;
import com.preston.codex.importer.model.ImportClassification;
import com.preston.codex.importer.model.ParsedEntry;
import com.preston.codex.importer.model.ParsedSource;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntryClassifierTest {

    private InMemoryMatchLookup lookup;
    private EntryClassifier classifier;
    private ParsedSource source;

    @Before
    public void setUp() {
        lookup = new InMemoryMatchLookup();
        EntryNormalizer normalizer = new EntryNormalizer();
        FingerprintCalculator fpCalc = new FingerprintCalculator(normalizer);
        ContentHashCalculator hashCalc = new ContentHashCalculator(normalizer);
        JaccardCalculator jaccard = new JaccardCalculator();
        EntryMatcher matcher = new EntryMatcher(lookup, jaccard);
        classifier = new EntryClassifier(matcher, fpCalc, hashCalc);
        source = new ParsedSource("Deep Work", "Cal Newport", "BOOK", null, null, null);
    }

    private ParsedEntry createEntry(int index, String id, String quote, String insight) {
        return new ParsedEntry(index, id, quote, insight, null, null, "QUOTE", null,
                Collections.emptyList(), Collections.emptyList(), Collections.emptyMap(), quote, Collections.emptyList());
    }

    @Test
    public void CL01_classify_newEntry() {
        ParsedEntry entry = createEntry(0, null, "Brand new quote", "Insight");
        List<ClassifiedEntry> result = classifier.classify(source, Collections.singletonList(entry));

        assertEquals(1, result.size());
        assertEquals(ImportClassification.NEW, result.get(0).getClassification());
        assertNotNull(result.get(0).getFingerprint());
        assertNotNull(result.get(0).getContentHash());
    }

    @Test
    public void CL02_classify_unchanged() {
        EntryNormalizer norm = new EntryNormalizer();
        FingerprintCalculator fpCalc = new FingerprintCalculator(norm);
        ContentHashCalculator hashCalc = new ContentHashCalculator(norm);

        String quote = "Clarity about what matters.";
        String fp = fpCalc.calculate("Deep Work", quote);
        String hash = hashCalc.calculate(quote, null, null, null, "QUOTE", Collections.emptyList());

        lookup.add(new ExistingEntry(1L, 10L, "id-1", fp, hash, hash, quote, false, false));

        ParsedEntry entry = createEntry(0, "id-1", quote, null);
        List<ClassifiedEntry> result = classifier.classify(source, Collections.singletonList(entry));

        assertEquals(1, result.size());
        assertEquals(ImportClassification.UNCHANGED, result.get(0).getClassification());
    }

    @Test
    public void CL03_classify_conflict() {
        EntryNormalizer norm = new EntryNormalizer();
        FingerprintCalculator fpCalc = new FingerprintCalculator(norm);
        ContentHashCalculator hashCalc = new ContentHashCalculator(norm);

        String originalQuote = "Original quote text.";
        String fp = fpCalc.calculate("Deep Work", originalQuote);
        String originalHash = hashCalc.calculate(originalQuote, null, null, null, "QUOTE", Collections.emptyList());

        lookup.add(new ExistingEntry(1L, 10L, "id-conflict", fp, originalHash, originalHash, originalQuote, false, false));

        ParsedEntry editedEntry = createEntry(0, "id-conflict", "Edited quote text in file", null);
        List<ClassifiedEntry> result = classifier.classify(source, Collections.singletonList(editedEntry));

        assertEquals(1, result.size());
        assertEquals(ImportClassification.CONFLICT, result.get(0).getClassification());
    }

    @Test
    public void CL04_classify_potentialDuplicate() {
        lookup.add(new ExistingEntry(1L, 10L, "id-old", "fp-old", "h-old", "h-old",
                "The very quick brown fox jumps over the lazy dog in the green peaceful summer meadow", false, false));

        ParsedEntry entry = createEntry(0, null,
                "The very quick brown fox jumps over the lazy dog in the green peaceful sunny meadow", null);
        List<ClassifiedEntry> result = classifier.classify(source, Collections.singletonList(entry));

        assertEquals(1, result.size());
        assertEquals(ImportClassification.POTENTIAL_DUPLICATE, result.get(0).getClassification());
    }

    @Test
    public void CL05_classify_invalid() {
        ParsedEntry entry1 = createEntry(0, "duplicate-id", "Quote 1", null);
        ParsedEntry entry2 = createEntry(1, "duplicate-id", "Quote 2", null);

        List<ClassifiedEntry> result = classifier.classify(source, Arrays.asList(entry1, entry2));

        assertEquals(2, result.size());
        assertEquals(ImportClassification.NEW, result.get(0).getClassification());
        assertEquals(ImportClassification.INVALID, result.get(1).getClassification());
    }

    @Test
    public void CL06_classify_idAttached() {
        EntryNormalizer norm = new EntryNormalizer();
        FingerprintCalculator fpCalc = new FingerprintCalculator(norm);
        ContentHashCalculator hashCalc = new ContentHashCalculator(norm);

        String quote = "Quote with missing ID originally.";
        String fp = fpCalc.calculate("Deep Work", quote);
        String hash = hashCalc.calculate(quote, null, null, null, "QUOTE", Collections.emptyList());

        // Existing entry has null stable_id
        lookup.add(new ExistingEntry(1L, 10L, null, fp, hash, hash, quote, false, false));

        // Incoming entry provides ID
        ParsedEntry entry = createEntry(0, "newly-assigned-id", quote, null);
        List<ClassifiedEntry> result = classifier.classify(source, Collections.singletonList(entry));

        assertEquals(1, result.size());
        assertEquals(ImportClassification.ID_ATTACHED, result.get(0).getClassification());
    }

    @Test
    public void CL07_classify_batchMixedResults() {
        EntryNormalizer norm = new EntryNormalizer();
        FingerprintCalculator fpCalc = new FingerprintCalculator(norm);
        ContentHashCalculator hashCalc = new ContentHashCalculator(norm);

        String quote1 = "Unchanged quote.";
        String fp1 = fpCalc.calculate("Deep Work", quote1);
        String hash1 = hashCalc.calculate(quote1, null, null, null, "QUOTE", Collections.emptyList());
        lookup.add(new ExistingEntry(1L, 10L, "id-unchanged", fp1, hash1, hash1, quote1, false, false));

        ParsedEntry entry1 = createEntry(0, "id-unchanged", quote1, null);
        ParsedEntry entry2 = createEntry(1, null, "Brand new quote 2", null);
        ParsedEntry entry3 = createEntry(2, "id-unchanged", "Duplicate stable id in batch", null);

        List<ClassifiedEntry> result = classifier.classify(source, Arrays.asList(entry1, entry2, entry3));

        assertEquals(3, result.size());
        assertEquals(ImportClassification.UNCHANGED, result.get(0).getClassification());
        assertEquals(ImportClassification.NEW, result.get(1).getClassification());
        assertEquals(ImportClassification.INVALID, result.get(2).getClassification());
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
