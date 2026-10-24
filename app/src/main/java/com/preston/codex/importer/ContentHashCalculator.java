package com.preston.codex.importer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculates a content hash over entry fields for change detection.
 */
public class ContentHashCalculator {

    private final EntryNormalizer normalizer;

    public ContentHashCalculator(EntryNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public String calculate(String quoteText,
                            String insightText,
                            String sourceRef,
                            String application,
                            String entryType,
                            List<String> tags) {
        String normQuote = normalizer.normalize(quoteText);
        String normInsight = normalizer.normalize(insightText);
        String normSourceRef = normalizer.normalize(sourceRef);
        String normApp = normalizer.normalize(application);
        String normType = normalizer.normalize(entryType);

        List<String> tagKeys = new ArrayList<>();
        if (tags != null) {
            for (String tag : tags) {
                String key = normalizer.toKey(tag);
                if (!key.isEmpty()) {
                    tagKeys.add(key);
                }
            }
        }
        Collections.sort(tagKeys);
        String tagString = String.join(",", tagKeys);

        String payload = normQuote + "|" + normInsight + "|" + normSourceRef + "|" +
                normApp + "|" + normType + "|" + tagString;
        return FingerprintCalculator.sha256Hex(payload);
    }
}
