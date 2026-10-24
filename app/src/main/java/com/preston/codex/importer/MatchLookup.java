package com.preston.codex.importer;

import com.preston.codex.importer.model.ExistingEntry;

import java.util.List;

/**
 * Interface decoupling database querying from matching logic.
 * Zero android.* dependencies.
 */
public interface MatchLookup {
    ExistingEntry findByStableId(String stableId);
    ExistingEntry findByFingerprint(String fingerprint);
    List<ExistingEntry> findAllCandidates();
}
