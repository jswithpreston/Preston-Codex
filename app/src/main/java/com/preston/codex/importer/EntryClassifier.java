package com.preston.codex.importer;

import com.preston.codex.importer.model.ClassifiedEntry;
import com.preston.codex.importer.model.MatchResult;
import com.preston.codex.importer.model.ParsedEntry;
import com.preston.codex.importer.model.ParsedSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classifies all parsed entries in a file by computing their hashes and evaluating match rules.
 */
public class EntryClassifier {

    private final EntryMatcher matcher;
    private final FingerprintCalculator fingerprintCalculator;
    private final ContentHashCalculator contentHashCalculator;

    public EntryClassifier(EntryMatcher matcher,
                           FingerprintCalculator fingerprintCalculator,
                           ContentHashCalculator contentHashCalculator) {
        this.matcher = matcher;
        this.fingerprintCalculator = fingerprintCalculator;
        this.contentHashCalculator = contentHashCalculator;
    }

    public List<ClassifiedEntry> classify(ParsedSource source, List<ParsedEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        matcher.resetBatch();
        String sourceTitle = source != null ? source.getTitle() : "";
        List<ClassifiedEntry> classified = new ArrayList<>(entries.size());

        for (ParsedEntry entry : entries) {
            String fingerprint = fingerprintCalculator.calculate(sourceTitle, entry.getQuoteText());
            String contentHash = contentHashCalculator.calculate(
                    entry.getQuoteText(),
                    entry.getInsightText(),
                    entry.getSourceRef(),
                    entry.getApplication(),
                    entry.getEntryType(),
                    entry.getTags()
            );

            MatchResult matchResult = matcher.match(entry.getStableId(), fingerprint, contentHash, entry.getQuoteText());
            classified.add(new ClassifiedEntry(entry, matchResult, fingerprint, contentHash));
        }

        return classified;
    }
}
