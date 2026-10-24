package com.preston.codex.importer;

import com.preston.codex.importer.model.ClassifiedEntry;
import com.preston.codex.importer.model.ImportClassification;
import com.preston.codex.importer.model.ImportPreview;
import com.preston.codex.importer.model.ParsedFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the ImportPreview summary for UI review prior to commit.
 */
public class ImportPreviewBuilder {

    public ImportPreview build(ParsedFile parsedFile, List<ClassifiedEntry> classifiedEntries, List<String> sourceWarnings) {
        int countNew = 0;
        int countUnchanged = 0;
        int countConflict = 0;
        int countDuplicate = 0;
        int countInvalid = 0;
        int countPotentialDuplicate = 0;
        int countIdAttached = 0;

        List<String> warnings = new ArrayList<>();
        if (sourceWarnings != null) {
            warnings.addAll(sourceWarnings);
        }

        if (classifiedEntries != null) {
            for (ClassifiedEntry ce : classifiedEntries) {
                if (ce.getClassification() == ImportClassification.NEW) countNew++;
                else if (ce.getClassification() == ImportClassification.UNCHANGED) countUnchanged++;
                else if (ce.getClassification() == ImportClassification.CONFLICT) countConflict++;
                else if (ce.getClassification() == ImportClassification.DUPLICATE) countDuplicate++;
                else if (ce.getClassification() == ImportClassification.INVALID) countInvalid++;
                else if (ce.getClassification() == ImportClassification.POTENTIAL_DUPLICATE) countPotentialDuplicate++;
                else if (ce.getClassification() == ImportClassification.ID_ATTACHED) countIdAttached++;

                if (ce.getParsedEntry().getWarnings() != null) {
                    warnings.addAll(ce.getParsedEntry().getWarnings());
                }
            }
        }

        if (parsedFile != null && parsedFile.getErrors() != null) {
            countInvalid += parsedFile.getErrors().size();
        }

        int totalParsed = (classifiedEntries != null ? classifiedEntries.size() : 0)
                + (parsedFile != null && parsedFile.getErrors() != null ? parsedFile.getErrors().size() : 0);

        return new ImportPreview(
                totalParsed,
                countNew,
                countUnchanged,
                countConflict,
                countDuplicate,
                countInvalid,
                countPotentialDuplicate,
                countIdAttached,
                warnings,
                classifiedEntries
        );
    }
}
