package com.preston.codex.importer.model;

/**
 * Classification category of an imported entry during staging.
 */
public enum ImportClassification {
    NEW,
    UNCHANGED,
    DUPLICATE,
    POTENTIAL_DUPLICATE,
    CONFLICT,
    INVALID,
    SKIPPED,
    ID_ATTACHED
}
