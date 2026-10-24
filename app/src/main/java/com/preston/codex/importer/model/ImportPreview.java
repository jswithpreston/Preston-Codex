package com.preston.codex.importer.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Preview summary presented prior to transactional commit.
 */
public final class ImportPreview {
    private final int totalParsed;
    private final int countNew;
    private final int countUnchanged;
    private final int countConflict;
    private final int countDuplicate;
    private final int countInvalid;
    private final int countPotentialDuplicate;
    private final int countIdAttached;
    private final List<String> warnings;
    private final List<ClassifiedEntry> classifiedEntries;

    public ImportPreview(int totalParsed,
                         int countNew,
                         int countUnchanged,
                         int countConflict,
                         int countDuplicate,
                         int countInvalid,
                         int countPotentialDuplicate,
                         int countIdAttached,
                         List<String> warnings,
                         List<ClassifiedEntry> classifiedEntries) {
        this.totalParsed = totalParsed;
        this.countNew = countNew;
        this.countUnchanged = countUnchanged;
        this.countConflict = countConflict;
        this.countDuplicate = countDuplicate;
        this.countInvalid = countInvalid;
        this.countPotentialDuplicate = countPotentialDuplicate;
        this.countIdAttached = countIdAttached;
        this.warnings = warnings == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(warnings));
        this.classifiedEntries = classifiedEntries == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(classifiedEntries));
    }

    public int getTotalParsed() { return totalParsed; }
    public int getCountNew() { return countNew; }
    public int getCountUnchanged() { return countUnchanged; }
    public int getCountConflict() { return countConflict; }
    public int getCountDuplicate() { return countDuplicate; }
    public int getCountInvalid() { return countInvalid; }
    public int getCountPotentialDuplicate() { return countPotentialDuplicate; }
    public int getCountIdAttached() { return countIdAttached; }
    public List<String> getWarnings() { return warnings; }
    public List<ClassifiedEntry> getClassifiedEntries() { return classifiedEntries; }
}
