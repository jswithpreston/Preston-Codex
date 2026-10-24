package com.preston.codex.importer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Calculates the immutable fingerprint of a knowledge entry.
 * Fingerprint = SHA-256(title_key + "\0" + quote with outer quote pair stripped, case-preserved).
 */
public class FingerprintCalculator {

    private final EntryNormalizer normalizer;

    public FingerprintCalculator(EntryNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public String calculate(String sourceTitle, String quoteText) {
        String titleKey = normalizer.toKey(sourceTitle);
        String normQuote = normalizer.normalize(quoteText);
        String strippedQuote = normalizer.stripWrappingQuotes(normQuote);

        String payload = titleKey + "\0" + strippedQuote;
        return sha256Hex(payload);
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported by JVM", e);
        }
    }
}
