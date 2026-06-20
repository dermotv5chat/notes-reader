package com.andriod.reader.data.local

import android.content.Context
import com.andriod.reader.domain.Note
import com.andriod.reader.domain.SyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncStateStore: SyncStateStore,
) {
    private val notesDir: File
        get() = File(context.filesDir, "notes").also { it.mkdirs() }

    fun listNotes(): List<Note> {
        val files = notesDir.listFiles { file -> file.isFile && file.name.endsWith(".md") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

        val syncStates = syncStateStore.readAll()

        return files.map { file ->
            val parsed = MarkdownParser.parse(file.name, file.readText())
            val state = syncStates[file.name]
            Note(
                id = parsed.id,
                title = parsed.title,
                content = parsed.content,
                fileName = file.name,
                updatedAt = parsed.updatedAt,
                syncStatus = state?.syncStatus ?: SyncStatus.LOCAL_ONLY,
            )
        }
    }

    fun getNote(fileName: String): Note? {
        val file = File(notesDir, fileName)
        if (!file.exists()) return null
        val parsed = MarkdownParser.parse(fileName, file.readText())
        val state = syncStateStore.readAll()[fileName]
        return Note(
            id = parsed.id,
            title = parsed.title,
            content = parsed.content,
            fileName = fileName,
            updatedAt = parsed.updatedAt,
            syncStatus = state?.syncStatus ?: SyncStatus.LOCAL_ONLY,
        )
    }

    fun saveNote(note: Note): Note {
        val file = File(notesDir, note.fileName)
        val updated = note.copy(updatedAt = Instant.now())
        file.writeText(MarkdownParser.serialize(updated))

        val states = syncStateStore.readAll().toMutableMap()
        val current = states[note.fileName]
        val newStatus = when (current?.syncStatus) {
            SyncStatus.SYNCED, SyncStatus.PENDING -> SyncStatus.PENDING
            else -> SyncStatus.LOCAL_ONLY
        }
        states[note.fileName] = (current ?: com.andriod.reader.domain.SyncFileState()).copy(
            syncStatus = newStatus,
            pendingDelete = false,
        )
        syncStateStore.writeAll(states)
        return updated.copy(syncStatus = newStatus)
    }

    fun createNote(title: String, content: String): Note {
        val fileName = MarkdownParser.newFileName()
        val note = Note(
            id = fileName.removeSuffix(".md"),
            title = title.ifBlank { "无标题" },
            content = content,
            fileName = fileName,
            updatedAt = Instant.now(),
            syncStatus = SyncStatus.LOCAL_ONLY,
        )
        File(notesDir, fileName).writeText(MarkdownParser.serialize(note))
        val states = syncStateStore.readAll().toMutableMap()
        states[fileName] = com.andriod.reader.domain.SyncFileState(syncStatus = SyncStatus.LOCAL_ONLY)
        syncStateStore.writeAll(states)
        return note
    }

    fun deleteNote(fileName: String) {
        val states = syncStateStore.readAll().toMutableMap()
        val state = states[fileName]
        if (state?.githubSha != null) {
            states[fileName] = state.copy(pendingDelete = true, syncStatus = SyncStatus.PENDING)
            syncStateStore.writeAll(states)
        } else {
            states.remove(fileName)
            syncStateStore.writeAll(states)
            File(notesDir, fileName).delete()
        }
    }

    fun writeRawFile(fileName: String, rawContent: String) {
        File(notesDir, fileName).writeText(rawContent)
    }

    fun readRawFile(fileName: String): String? {
        val file = File(notesDir, fileName)
        return if (file.exists()) file.readText() else null
    }

    fun deleteLocalFile(fileName: String) {
        File(notesDir, fileName).delete()
    }

    fun notesDirectory(): File = notesDir
}
