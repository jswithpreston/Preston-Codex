package com.preston.codex.importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class FingerprintCalculatorTest {

    private FingerprintCalculator calculator;

    @Before
    public void setUp() {
        calculator = new FingerprintCalculator(new EntryNormalizer());
    }

    @Test
    public void F01_fingerprint_deterministic() {
        String fp1 = calculator.calculate("Deep Work", "Clarity about what matters.");
        String fp2 = calculator.calculate("Deep Work", "Clarity about what matters.");
        assertEquals(fp1, fp2);
    }

    @Test
    public void F02_fingerprint_quoteVariation_differs() {
        String fp1 = calculator.calculate("Deep Work", "Clarity about what matters.");
        String fp2 = calculator.calculate("Deep Work", "Clarity about what matters!");
        assertNotEquals(fp1, fp2);
    }

    @Test
    public void F03_fingerprint_sourceVariation_differs() {
        String fp1 = calculator.calculate("Deep Work", "Clarity about what matters.");
        String fp2 = calculator.calculate("Essentialism", "Clarity about what matters.");
        assertNotEquals(fp1, fp2);
    }

    @Test
    public void F04_fingerprint_format_64hexChars() {
        String fp = calculator.calculate("Deep Work", "Sample quote.");
        assertEquals(64, fp.length());
        assertTrue(fp.matches("^[0-9a-f]{64}$"));
    }

    @Test
    public void F05_fingerprint_whitespaceInsensitive() {
        String fp1 = calculator.calculate("Deep Work", "Clarity   about\t\twhat   matters.");
        String fp2 = calculator.calculate("Deep Work", "Clarity about what matters.");
        assertEquals(fp1, fp2);
    }

    @Test
    public void F06_fingerprint_casePreserving() {
        String fp1 = calculator.calculate("Deep Work", "Focus matters.");
        String fp2 = calculator.calculate("Deep Work", "focus matters.");
        assertNotEquals(fp1, fp2);
    }

    @Test
    public void F07_fingerprint_ignoresWrappingQuotes() {
        String fp1 = calculator.calculate("Deep Work", "Clarity about what matters.");
        String fp2 = calculator.calculate("Deep Work", "\"Clarity about what matters.\"");
        String fp3 = calculator.calculate("Deep Work", "\u201CClarity about what matters.\u201D");
        assertEquals(fp1, fp2);
        assertEquals(fp1, fp3);
    }

    @Test
    public void F08_fingerprint_usesTitleKey_caseInsensitiveBookTitle() {
        // Amendment 2: Fingerprint = SHA-256(title_key + "\0" + quote with outer quote pair stripped, case-preserved)
        String fp1 = calculator.calculate("Deep Work", "Clarity about what matters.");
        String fp2 = calculator.calculate("deep work", "Clarity about what matters.");
        assertEquals(fp1, fp2);
    }
}
