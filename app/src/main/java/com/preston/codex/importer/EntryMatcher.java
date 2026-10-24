package com.preston.codex.importer;

import com.preston.codex.importer.model.ExistingEntry;
import com.preston.codex.importer.model.ImportClassification;
import com.preston.codex.importer.model.MatchResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Matches incoming entries against batch history and existing database entries.
 * Pure Java, zero Android dependencies.
 */
public class EntryMatcher {

    private final MatchLookup lookup;
    private final JaccardCalculator jaccardCalculator;

    private final Set<String> seenStableIds = new HashSet<>();
    private final Set<String> seenFingerprints = new HashSet<>();

    public EntryMatcher(MatchLookup lookup, JaccardCalculator jaccardCalculator) {
        this.lookup = lookup;
        this.jaccardCalculator = jaccardCalculator;
    }

    public void resetBatch() {
        seenStableIds.clear();
        seenFingerprints.clear();
    }

    public MatchResult match(String stableId, String fingerprint, String incomingHash, String quoteText) {
        // 1. Intra-batch duplicate checks (Amendment 1)
        if (stableId != null && !stableId.trim().isEmpty()) {
            if (seenStableIds.contains(stableId)) {
                return MatchResult.invalid("Duplicate stable_id in same file: " + stableId);
            }
        }
        if (fingerprint != null && seenFingerprints.contains(fingerprint)) {
            return MatchResult.of(ImportClassification.DUPLICATE);
        }

        // 2. Query DB lookups (includes active, archived, and trashed entries per Amendment 5)
        ExistingEntry byId = (stableId != null && !stableId.trim().isEmpty())
                ? lookup.findByStableId(stableId)
                : null;
        ExistingEntry byFp = (fingerprint != null)
                ? lookup.findByFingerprint(fingerprint)
                : null;

        // 3. Amendment 6: stable_id matches entry X, fingerprint matches entry Y
        if (byId != null && byFp != null && byId.getId() != byFp.getId()) {
            recordSeen(stableId, fingerprint);
            return new MatchResult(ImportClassification.CONFLICT, byId.getId(),
                    "Incoming stable_id matches entry " + byId.getId() + " but fingerprint matches entry " + byFp.getId(), null);
        }

        // 4. Stable ID match
        if (byId != null) {
            recordSeen(stableId, fingerprint);
            if (incomingHash.equals(byId.getContentHash()) || incomingHash.equals(byId.getImportedHash())) {
                String note = byId.isTrashed() ? "exists in trash" : null;
                return MatchResult.of(ImportClassification.UNCHANGED, byId.getId(), note);
            } else {
                return new MatchResult(ImportClassification.CONFLICT, byId.getId(), "Content hash mismatch on stable ID match", null);
            }
        }

        // 5. Fingerprint match
        if (byFp != null) {
            recordSeen(stableId, fingerprint);
            // Q-E: fingerprint match, existing stable_id is NULL, incoming has ID
            if (byFp.getStableId() == null && stableId != null && !stableId.trim().isEmpty()) {
                if (incomingHash.equals(byFp.getContentHash()) || incomingHash.equals(byFp.getImportedHash())) {
                    return MatchResult.of(ImportClassification.ID_ATTACHED, byFp.getId());
                } else {
                    return new MatchResult(ImportClassification.CONFLICT, byFp.getId(), "Content mismatch during ID attachment", null);
                }
            }

            // Fingerprint match, but existing has different stable ID
            if (byFp.getStableId() != null && !byFp.getStableId().equals(stableId)) {
                return new MatchResult(ImportClassification.CONFLICT, byFp.getId(),
                        "Existing entry has different stable_id: " + byFp.getStableId(), null);
            }

            // Fingerprint match, check hash (Amendment 1)
            if (incomingHash.equals(byFp.getContentHash()) || incomingHash.equals(byFp.getImportedHash())) {
                String note = byFp.isTrashed() ? "exists in trash" : null;
                return MatchResult.of(ImportClassification.UNCHANGED, byFp.getId(), note);
            } else {
                // Entries without stable IDs whose quote matches but whose metadata/insight/tags changed = CONFLICT
                return new MatchResult(ImportClassification.CONFLICT, byFp.getId(),
                        "Entry without stable ID had metadata changed", null);
            }
        }

        // 6. Fuzzy Jaccard match (Locked decision #2)
        List<ExistingEntry> candidates = lookup.findAllCandidates();
        if (candidates != null) {
            for (ExistingEntry candidate : candidates) {
                if (jaccardCalculator.isPotentialDuplicate(candidate.getQuoteText(), quoteText)) {
                    recordSeen(stableId, fingerprint);
                    return MatchResult.of(ImportClassification.POTENTIAL_DUPLICATE, candidate.getId(), "Potential duplicate (Jaccard >= 0.85)");
                }
            }
        }

        // 7. Brand new entry
        recordSeen(stableId, fingerprint);
        return MatchResult.of(ImportClassification.NEW);
    }

    private void recordSeen(String stableId, String fingerprint) {
        if (stableId != null && !stableId.trim().isEmpty()) {
            seenStableIds.add(stableId);
        }
        if (fingerprint != null) {
            seenFingerprints.add(fingerprint);
        }
    }
}
