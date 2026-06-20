package com.andriod.reader.data.repository

import com.andriod.reader.data.local.MarkdownParser
import com.andriod.reader.data.local.NoteFileStore
import com.andriod.reader.data.local.SyncStateStore
import com.andriod.reader.domain.Note
import com.andriod.reader.domain.TrashEntry
import com.andriod.reader.domain.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteFileStore: NoteFileStore,
    private val syncStateStore: SyncStateStore,
) {
    fun listNotes(query: String = ""): List<Note> {
        val notes = noteFileStore.listNotes()
        if (query.isBlank()) return notes
        return notes.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true)
        }
    }

    fun getNote(fileName: String): Note? = noteFileStore.getNote(fileName)

    fun createNote(title: String, content: String): Note = noteFileStore.createNote(title, content)

    fun saveNote(note: Note): Note = noteFileStore.saveNote(note)

    fun moveToTrash(fileName: String): TrashEntry = noteFileStore.moveToTrash(fileName)

    fun restoreFromTrash(entryId: String) = noteFileStore.restoreFromTrash(entryId)

    fun permanentDeleteFromTrash(entryId: String) = noteFileStore.permanentDeleteFromTrash(entryId)

    fun listTrash(): List<TrashEntry> = noteFileStore.listTrashEntries()

    fun getTrashNoteTitle(entry: TrashEntry): String {
        val raw = noteFileStore.readTrashRaw(entry.id) ?: return entry.originalPath
        return MarkdownParser.parse(entry.originalPath, raw).title
    }

    fun listVirtualFolders(): Set<String> = noteFileStore.listVirtualFolders()

    fun createFolder(parentFolder: String, folderName: String): String =
        noteFileStore.createFolder(parentFolder, folderName)

    fun renameNoteFile(fileName: String, newBaseName: String): String =
        noteFileStore.renameNoteFile(fileName, newBaseName)

    fun renameFolder(folderPath: String, newFolderName: String): String =
        noteFileStore.renameFolder(folderPath, newFolderName)

    fun countNotesUnderFolder(folderPath: String): Int =
        noteFileStore.countNotesUnderFolder(folderPath)

    fun markSynced(fileName: String, sha: String) {
        val states = syncStateStore.readAll().toMutableMap()
        states[fileName] = com.andriod.reader.domain.SyncFileState(
            githubSha = sha,
            syncStatus = SyncStatus.SYNCED,
            pendingDelete = false,
        )
        syncStateStore.writeAll(states)
    }

    fun getSyncStates() = syncStateStore.readAll()
}
