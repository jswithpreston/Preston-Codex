package com.preston.codex.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class JaccardCalculatorTest {

    private JaccardCalculator calculator;

    @Before
    public void setUp() {
        calculator = new JaccardCalculator();
    }

    @Test
    public void J01_jaccard_identicalSets_returns1() {
        double sim = calculator.similarity("The quick brown fox", "The quick brown fox");
        assertEquals(1.0, sim, 0.0001);
    }

    @Test
    public void J02_jaccard_disjointSets_returns0() {
        double sim = calculator.similarity("apple banana cherry", "dog elephant frog");
        assertEquals(0.0, sim, 0.0001);
    }

    @Test
    public void J03_jaccard_partialOverlap_correctRatio() {
        // A = {a, b, c}, B = {b, c, d} -> intersection = 2, union = 4 -> 0.5
        double sim = calculator.similarity("a b c", "b c d");
        assertEquals(0.5, sim, 0.0001);
    }

    @Test
    public void J04_jaccard_caseAndPunctuationInsensitive() {
        // "Don't stop!" vs "dont stop"
        double sim = calculator.similarity("Don't stop!", "dont stop");
        assertEquals(1.0, sim, 0.0001);
    }

    @Test
    public void J05_jaccard_thresholdComparison_identifiesPotentialDuplicate() {
        String s1 = "Clarity about what matters provides clarity about what does not";
        String s2 = "Clarity about what matters provides extreme clarity about what does not";
        // Tokens s1: {clarity, about, what, matters, provides, does, not} = 7 tokens
        // Tokens s2: {clarity, about, what, matters, provides, extreme, does, not} = 8 tokens
        // Intersection = 7, Union = 8 -> 7/8 = 0.875 >= 0.85
        double sim = calculator.similarity(s1, s2);
        assertTrue(sim >= 0.85);
        assertTrue(calculator.isPotentialDuplicate(s1, s2));

        String s3 = "Completely unrelated text about gardening and plants in spring";
        assertFalse(calculator.isPotentialDuplicate(s1, s3));
    }
}
