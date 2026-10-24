package com.preston.codex.importer;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Calculates Jaccard token similarity over entry quotes.
 * Locked decision #2: Potential duplicate = normalized-token Jaccard similarity >= 0.85.
 * Tokens are lowercased with punctuation stripped (Amendment 7).
 */
public class JaccardCalculator {

    public static final double DUPLICATE_THRESHOLD = 0.85;

    public double similarity(String text1, String text2) {
        Set<String> set1 = tokenize(text1);
        Set<String> set2 = tokenize(text2);

        if (set1.isEmpty() && set2.isEmpty()) {
            return 1.0;
        }
        if (set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / (double) union.size();
    }

    public boolean isPotentialDuplicate(String text1, String text2) {
        return similarity(text1, text2) >= DUPLICATE_THRESHOLD;
    }

    public Set<String> tokenize(String text) {
        if (text == null) {
            return Collections.emptySet();
        }
        String cleaned = text.toLowerCase(Locale.ROOT)
                             .replaceAll("[^a-z0-9\\s]", "")
                             .trim();
        if (cleaned.isEmpty()) {
            return Collections.emptySet();
        }
        String[] parts = cleaned.split("\\s+");
        Set<String> tokens = new HashSet<>();
        for (String p : parts) {
            if (!p.isEmpty()) {
                tokens.add(p);
            }
        }
        return tokens;
    }
}
