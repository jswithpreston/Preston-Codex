package com.preston.codex.importer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.preston.codex.data.database.AppDatabase;
import com.preston.codex.data.entity.ImportBatchEntity;
import com.preston.codex.data.entity.ImportResultEntity;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeEntryTagEntity;
import com.preston.codex.data.entity.KnowledgeRelationshipEntity;
import com.preston.codex.data.entity.KnowledgeSourceEntity;
import com.preston.codex.data.entity.KnowledgeTagEntity;
import com.preston.codex.data.repository.RoomMatchLookup;
import com.preston.codex.importer.model.ImportClassification;
import com.preston.codex.importer.model.ImportReport;
import com.preston.codex.importer.model.MatchResult;
import com.preston.codex.importer.model.ParsedEntry;
import com.preston.codex.importer.model.ParsedFile;
import com.preston.codex.importer.model.ParsedSource;

import java.lang.reflect.Type;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes the transactional database commit for an imported Markdown file.
 * Re-runs matching inside the transaction to avoid race conditions.
 */
public class ImportCommitter {

    private final AppDatabase db;
    private final Clock clock;
    private final EntryNormalizer normalizer;
    private final FingerprintCalculator fingerprintCalculator;
    private final ContentHashCalculator contentHashCalculator;
    private final JaccardCalculator jaccardCalculator;
    private final Gson gson = new Gson();

    public ImportCommitter(AppDatabase db,
                           Clock clock,
                           EntryNormalizer normalizer,
                           FingerprintCalculator fingerprintCalculator,
                           ContentHashCalculator contentHashCalculator,
                           JaccardCalculator jaccardCalculator) {
        this.db = db;
        this.clock = clock;
        this.normalizer = normalizer;
        this.fingerprintCalculator = fingerprintCalculator;
        this.contentHashCalculator = contentHashCalculator;
        this.jaccardCalculator = jaccardCalculator;
    }

    public ImportReport commit(ParsedFile parsedFile, String fileContentHash) throws Exception {
        long now = clock.millis();
        long[] batchIdHolder = new long[1];
        int[] counts = new int[7]; // [new, unchanged, conflict, duplicate, invalid, skipped, ids_attached]

        try {
            db.runInTransaction(() -> {
                // 1. Re-instantiate matcher inside the transaction (Amendment 4)
                RoomMatchLookup txLookup = new RoomMatchLookup(db.knowledgeEntryDao());
                EntryMatcher txMatcher = new EntryMatcher(txLookup, jaccardCalculator);

                // 2. Resolve KnowledgeSource
                ParsedSource parsedSource = parsedFile.getSource();
                long sourceId = resolveSource(parsedSource, now);

                // 3. Create ImportBatch (IN_PROGRESS)
                ImportBatchEntity batch = new ImportBatchEntity();
                batch.filename = parsedFile.getFilename();
                batch.contentHash = fileContentHash != null ? fileContentHash : "";
                batch.startedAt = now;
                batch.outcome = "IN_PROGRESS";
                batch.entriesParsed = parsedFile.getEntries().size() + parsedFile.getErrors().size();
                batchIdHolder[0] = db.importBatchDao().insert(batch);
                long batchId = batchIdHolder[0];

                // 4. Record errors from parsing as INVALID
                for (int i = 0; i < parsedFile.getErrors().size(); i++) {
                    ImportResultEntity errorResult = new ImportResultEntity();
                    errorResult.batchId = batchId;
                    errorResult.entryIndex = parsedFile.getErrors().get(i).getEntryIndex();
                    errorResult.classification = "INVALID";
                    errorResult.resolutionStatus = "SKIPPED";
                    errorResult.errorMessage = parsedFile.getErrors().get(i).getMessage();
                    db.importResultDao().insert(errorResult);
                    counts[4]++; // invalid
                }

                String sourceTitle = parsedSource != null ? parsedSource.getTitle() : "";

                // 5. Process entries
                for (ParsedEntry entry : parsedFile.getEntries()) {
                    String fp = fingerprintCalculator.calculate(sourceTitle, entry.getQuoteText());
                    String hash = contentHashCalculator.calculate(
                            entry.getQuoteText(),
                            entry.getInsightText(),
                            entry.getSourceRef(),
                            entry.getApplication(),
                            entry.getEntryType(),
                            entry.getTags()
                    );

                    MatchResult match = txMatcher.match(entry.getStableId(), fp, hash, entry.getQuoteText());
                    ImportClassification classification = match.getClassification();

                    ImportResultEntity result = new ImportResultEntity();
                    result.batchId = batchId;
                    result.entryIndex = entry.getEntryIndex();
                    result.stableId = entry.getStableId();
                    result.fingerprint = fp;
                    result.classification = classification.name();
                    result.incomingHash = hash;
                    result.errorMessage = match.getErrorMessage();

                    if (classification == ImportClassification.NEW) {
                        long entryId = insertNewEntry(sourceId, entry, fp, hash, batchId, now);
                        result.insertedEntryId = entryId;
                        result.resolutionStatus = "APPLIED";
                        db.importResultDao().insert(result);
                        counts[0]++; // new
                    } else if (classification == ImportClassification.ID_ATTACHED) {
                        Long existingId = match.getExistingEntryId();
                        if (existingId != null && entry.getStableId() != null) {
                            db.knowledgeEntryDao().updateStableId(existingId, entry.getStableId(), now);
                        }
                        result.existingEntryId = existingId;
                        result.resolutionStatus = "APPLIED";
                        db.importResultDao().insert(result);
                        counts[6]++; // ids_attached
                    } else if (classification == ImportClassification.UNCHANGED) {
                        result.existingEntryId = match.getExistingEntryId();
                        result.resolutionStatus = "KEPT_EXISTING";
                        db.importResultDao().insert(result);
                        counts[1]++; // unchanged
                    } else if (classification == ImportClassification.DUPLICATE) {
                        result.existingEntryId = match.getExistingEntryId();
                        result.resolutionStatus = "SKIPPED";
                        db.importResultDao().insert(result);
                        counts[3]++; // duplicate
                    } else if (classification == ImportClassification.INVALID) {
                        result.resolutionStatus = "SKIPPED";
                        db.importResultDao().insert(result);
                        counts[4]++; // invalid
                    } else if (classification == ImportClassification.CONFLICT || classification == ImportClassification.POTENTIAL_DUPLICATE) {
                        // Held entries (Amendment 3, 5, 11)
                        result.existingEntryId = match.getExistingEntryId();
                        result.incomingPayload = gson.toJson(entry);
                        result.resolutionStatus = "PENDING";

                        // Amendment 5: do not create a second PENDING row for the same existing_entry_id + incoming_hash
                        boolean duplicatePending = false;
                        if (match.getExistingEntryId() != null) {
                            ImportResultEntity pending = db.importResultDao().findPendingByExistingEntryAndHash(match.getExistingEntryId(), hash);
                            if (pending != null) {
                                duplicatePending = true;
                            }
                        }

                        if (!duplicatePending) {
                            db.importResultDao().insert(result);
                        }
                        if (classification == ImportClassification.CONFLICT) {
                            counts[2]++; // conflict
                        } else {
                            counts[5]++; // potential duplicate treated as skipped/held in count
                        }
                    }
                }

                // 6. Resolve all dangling related references across entries (Q-D)
                resolveDanglingRelationships(now);

                // 7. Update batch outcome to COMPLETED
                batch.id = batchId;
                batch.finishedAt = clock.millis();
                batch.outcome = "COMPLETED";
                batch.entriesNew = counts[0];
                batch.entriesUnchanged = counts[1];
                batch.entriesConflict = counts[2];
                batch.entriesDuplicate = counts[3];
                batch.entriesInvalid = counts[4];
                batch.entriesSkipped = counts[5];
                batch.idsAttached = counts[6];
                db.importBatchDao().update(batch);
            });

            return new ImportReport(
                    batchIdHolder[0],
                    "COMPLETED",
                    parsedFile.getEntries().size() + parsedFile.getErrors().size(),
                    counts[0],
                    counts[1],
                    counts[2],
                    counts[3],
                    counts[4],
                    counts[5],
                    counts[6]
            );

        } catch (Exception ex) {
            // Amendment 6: Record FAILED import_batch row in separate transaction after rollback
            try {
                ImportBatchEntity failedBatch = new ImportBatchEntity();
                failedBatch.filename = parsedFile.getFilename();
                failedBatch.contentHash = fileContentHash != null ? fileContentHash : "";
                failedBatch.startedAt = now;
                failedBatch.finishedAt = clock.millis();
                failedBatch.outcome = "FAILED";
                failedBatch.errorMessage = ex.getMessage();
                db.importBatchDao().insert(failedBatch);
            } catch (Exception ignored) {
            }
            throw ex;
        }
    }

    private long resolveSource(ParsedSource parsedSource, long now) {
        if (parsedSource == null) {
            return 0L;
        }
        String titleKey = normalizer.toKey(parsedSource.getTitle());
        String authorKey = normalizer.toKey(parsedSource.getAuthor());

        KnowledgeSourceEntity existing = db.knowledgeSourceDao().findByKeys(titleKey, authorKey);
        if (existing != null) {
            // Q-C: leave existing row unchanged on reimport
            return existing.id;
        }

        KnowledgeSourceEntity newSource = new KnowledgeSourceEntity();
        newSource.title = parsedSource.getTitle();
        newSource.author = parsedSource.getAuthor();
        newSource.titleKey = titleKey;
        newSource.authorKey = authorKey;
        newSource.sourceType = parsedSource.getSourceType();
        newSource.publisher = parsedSource.getPublisher();
        newSource.year = parsedSource.getYear();
        newSource.isbn = parsedSource.getIsbn();
        newSource.createdAt = now;
        newSource.updatedAt = now;

        return db.knowledgeSourceDao().insert(newSource);
    }

    private long insertNewEntry(long sourceId, ParsedEntry entry, String fingerprint, String contentHash, long batchId, long now) {
        KnowledgeEntryEntity entity = new KnowledgeEntryEntity();
        entity.sourceId = sourceId;
        entity.stableId = entry.getStableId();
        entity.fingerprint = fingerprint;
        entity.contentHash = contentHash;
        entity.importedHash = contentHash; // Amendment 1: imported_hash matches content_hash on import
        entity.quoteText = entry.getQuoteText();
        entity.insightText = entry.getInsightText();
        entity.sourceRef = entry.getSourceRef();
        entity.application = entry.getApplication();
        entity.entryType = entry.getEntryType();
        entity.reviewMode = entry.getReviewMode();
        entity.rawMarkdown = entry.getRawMarkdown();
        entity.importBatchId = batchId;
        entity.createdAt = now;
        entity.updatedAt = now;

        // Process extraFields and dangling related refs (Q-D)
        Map<String, String> extra = new LinkedHashMap<>(entry.getExtraFields());
        List<String> dangling = new ArrayList<>();
        if (entry.getRelated() != null) {
            for (String relatedId : entry.getRelated()) {
                KnowledgeEntryEntity target = db.knowledgeEntryDao().findByStableId(relatedId);
                if (target == null) {
                    dangling.add(relatedId);
                }
            }
        }
        if (!dangling.isEmpty()) {
            extra.put("_dangling_related", gson.toJson(dangling));
        }

        entity.extraFields = extra.isEmpty() ? null : gson.toJson(extra);
        long entryId = db.knowledgeEntryDao().insert(entity);

        // Tags
        if (entry.getTags() != null) {
            for (String tag : entry.getTags()) {
                String normTag = normalizer.toKey(tag);
                if (!normTag.isEmpty()) {
                    KnowledgeTagEntity tagEntity = db.knowledgeTagDao().findByName(normTag);
                    long tagId;
                    if (tagEntity == null) {
                        tagId = db.knowledgeTagDao().insert(new KnowledgeTagEntity(normTag));
                        if (tagId <= 0) {
                            KnowledgeTagEntity existingTag = db.knowledgeTagDao().findByName(normTag);
                            tagId = existingTag != null ? existingTag.id : 0L;
                        }
                    } else {
                        tagId = tagEntity.id;
                    }
                    if (tagId > 0) {
                        db.knowledgeTagDao().insertEntryTag(new KnowledgeEntryTagEntity(entryId, tagId));
                    }
                }
            }
        }

        // Direct relationships to already-existing entries
        if (entry.getRelated() != null) {
            for (String relatedId : entry.getRelated()) {
                KnowledgeEntryEntity target = db.knowledgeEntryDao().findByStableId(relatedId);
                if (target != null && target.id != entryId) {
                    long fromId = Math.min(entryId, target.id);
                    long toId = Math.max(entryId, target.id);
                    db.knowledgeRelationshipDao().insert(new KnowledgeRelationshipEntity(fromId, toId, "RELATED", now));
                }
            }
        }

        return entryId;
    }

    private void resolveDanglingRelationships(long now) {
        List<KnowledgeEntryEntity> allEntries = db.knowledgeEntryDao().getAllEntries();
        Type listType = new TypeToken<List<String>>() {}.getType();

        for (KnowledgeEntryEntity entry : allEntries) {
            if (entry.extraFields == null || !entry.extraFields.contains("_dangling_related")) {
                continue;
            }
            Map<String, String> extra = gson.fromJson(entry.extraFields, new TypeToken<Map<String, String>>() {}.getType());
            if (extra == null || !extra.containsKey("_dangling_related")) {
                continue;
            }
            List<String> danglingList = gson.fromJson(extra.get("_dangling_related"), listType);
            if (danglingList == null || danglingList.isEmpty()) {
                continue;
            }

            List<String> remainingDangling = new ArrayList<>();
            boolean modified = false;

            for (String targetStableId : danglingList) {
                KnowledgeEntryEntity target = db.knowledgeEntryDao().findByStableId(targetStableId);
                if (target != null && target.id != entry.id) {
                    long fromId = Math.min(entry.id, target.id);
                    long toId = Math.max(entry.id, target.id);
                    db.knowledgeRelationshipDao().insert(new KnowledgeRelationshipEntity(fromId, toId, "RELATED", now));
                    modified = true;
                } else {
                    remainingDangling.add(targetStableId);
                }
            }

            if (modified) {
                if (remainingDangling.isEmpty()) {
                    extra.remove("_dangling_related");
                } else {
                    extra.put("_dangling_related", gson.toJson(remainingDangling));
                }
                entry.extraFields = extra.isEmpty() ? null : gson.toJson(extra);
                entry.updatedAt = now;
                db.knowledgeEntryDao().update(entry);
            }
        }
    }
}
