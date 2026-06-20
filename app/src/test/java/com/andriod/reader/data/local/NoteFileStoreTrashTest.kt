package com.andriod.reader.data.local

import com.andriod.reader.domain.SyncFileState
import com.andriod.reader.domain.SyncStatus
import com.andriod.reader.domain.TrashEntry
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Instant
import java.util.UUID

class NoteFileStoreTrashTest {
    @Test
    fun trashFile_moveAndRestore_preservesContent() {
        val notesDir = createTempDir("notes")
        val trashDir = createTempDir("trash")
        val originalPath = "folder/note.md"
        val noteFile = File(notesDir, originalPath)
        noteFile.parentFile?.mkdirs()
        val raw = "---\ntitle: Test\n---\n\nBody"
        noteFile.writeText(raw)

        val id = UUID.randomUUID().toString()
        val trashFile = File(trashDir, "$id.md")
        trashFile.writeText(noteFile.readText())
        noteFile.delete()
        cleanupEmptyParents(noteFile.parentFile, notesDir)

        assertFalse(noteFile.exists())
        assertTrue(trashFile.exists())

        noteFile.parentFile?.mkdirs()
        noteFile.writeText(trashFile.readText())
        trashFile.delete()

        assertTrue(noteFile.exists())
        assertFalse(trashFile.exists())
        assertEquals(raw, noteFile.readText())

        notesDir.deleteRecursively()
        trashDir.deleteRecursively()
    }

    @Test
    fun trashEntry_syncState_roundTripInJson() {
        val gson = Gson()
        val entry = TrashEntry(
            id = "abc",
            originalPath = "a/b.md",
            deletedAt = Instant.parse("2026-06-20T10:00:00Z"),
            syncState = SyncFileState(
                githubSha = "sha123",
                remotePath = "notes/b.md",
                syncStatus = SyncStatus.SYNCED,
                pendingDelete = false,
            ),
        )
        val dto = TrashEntryJson.from(entry)
        val restored = dto.toEntry()

        assertEquals(entry.id, restored.id)
        assertEquals(entry.originalPath, restored.originalPath)
        assertEquals(entry.deletedAt, restored.deletedAt)
        assertEquals(entry.syncState.githubSha, restored.syncState.githubSha)
        assertEquals(entry.syncState.remotePath, restored.syncState.remotePath)
        assertEquals(entry.syncState.syncStatus, restored.syncState.syncStatus)

        val json = gson.toJson(dto)
        val parsed = gson.fromJson(json, TrashEntryJson::class.java)
        assertEquals(entry.originalPath, parsed.toEntry().originalPath)
    }

    private fun createTempDir(prefix: String): File {
        val dir = File(System.getProperty("java.io.tmpdir"), "reader-test-$prefix-${UUID.randomUUID()}")
        check(dir.mkdirs()) { "Failed to create ${dir.path}" }
        return dir
    }

    private fun cleanupEmptyParents(dir: File?, root: File) {
        var current = dir ?: return
        while (current.path != root.path && current.exists() && current.listFiles()?.isEmpty() == true) {
            current.delete()
            current = current.parentFile ?: break
        }
    }

    /** Mirrors TrashStore serialization for unit testing without Android Context. */
    private data class TrashEntryJson(
        val id: String,
        val originalPath: String,
        val deletedAt: String,
        val githubSha: String? = null,
        val remotePath: String? = null,
        val syncStatus: String = SyncStatus.LOCAL_ONLY.name,
        val pendingDelete: Boolean = false,
    ) {
        fun toEntry(): TrashEntry = TrashEntry(
            id = id,
            originalPath = originalPath,
            deletedAt = Instant.parse(deletedAt),
            syncState = SyncFileState(
                githubSha = githubSha,
                remotePath = remotePath,
                syncStatus = runCatching { SyncStatus.valueOf(syncStatus) }
                    .getOrDefault(SyncStatus.LOCAL_ONLY),
                pendingDelete = pendingDelete,
            ),
        )

        companion object {
            fun from(entry: TrashEntry): TrashEntryJson = TrashEntryJson(
                id = entry.id,
                originalPath = entry.originalPath,
                deletedAt = entry.deletedAt.toString(),
                githubSha = entry.syncState.githubSha,
                remotePath = entry.syncState.remotePath,
                syncStatus = entry.syncState.syncStatus.name,
                pendingDelete = entry.syncState.pendingDelete,
            )
        }
    }
}
