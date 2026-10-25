package com.preston.codex.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.preston.codex.data.dao.KnowledgeEntryDao;
import com.preston.codex.data.dao.KnowledgeSourceDao;
import com.preston.codex.data.database.AppDatabase;
import com.preston.codex.data.entity.KnowledgeEntryEntity;
import com.preston.codex.data.entity.KnowledgeSourceEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class RoomBookmarkIntegrityTest {

    private AppDatabase db;
    private KnowledgeEntryDao entryDao;
    private KnowledgeSourceDao sourceDao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
        entryDao = db.knowledgeEntryDao();
        sourceDao = db.knowledgeSourceDao();
    }

    @After
    public void tearDown() {
        if (db != null) {
            db.close();
        }
    }

    @Test
    public void bookmarkUpdate_provesFingerprintStableIdContentHashImportedHashAndUpdatedAtUnchanged() {
        KnowledgeSourceEntity source = new KnowledgeSourceEntity();
        source.title = "Meditations";
        source.author = "Marcus Aurelius";
        source.titleKey = "meditations";
        source.authorKey = "marcus aurelius";
        source.createdAt = 1000L;
        source.updatedAt = 1000L;
        long sourceId = sourceDao.insert(source);

        KnowledgeEntryEntity entry = new KnowledgeEntryEntity();
        entry.sourceId = sourceId;
        entry.stableId = "MA-001";
        entry.fingerprint = "fp-abc-123";
        entry.contentHash = "hash-content-xyz";
        entry.importedHash = "hash-imported-xyz";
        entry.quoteText = "Waste no more time arguing about what a good man should be. Be one.";
        entry.isBookmarked = false;
        entry.createdAt = 2000L;
        entry.updatedAt = 3000L;
        long entryId = entryDao.insert(entry);

        KnowledgeEntryEntity before = entryDao.findById(entryId);
        assertFalse(before.isBookmarked);

        // Update bookmark status
        entryDao.updateBookmark(entryId, true);

        KnowledgeEntryEntity after = entryDao.findById(entryId);
        assertTrue(after.isBookmarked);

        // Prove strictly that none of the identity, hash, or timestamp fields were modified
        assertEquals("Fingerprint must be unchanged", before.fingerprint, after.fingerprint);
        assertEquals("Stable ID must be unchanged", before.stableId, after.stableId);
        assertEquals("Content hash must be unchanged", before.contentHash, after.contentHash);
        assertEquals("Imported hash must be unchanged", before.importedHash, after.importedHash);
        assertEquals("Updated at timestamp must be unchanged", before.updatedAt, after.updatedAt);
        assertEquals("Created at timestamp must be unchanged", before.createdAt, after.createdAt);
    }
}
