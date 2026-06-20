package com.andriod.reader.data.local

import android.content.Context
import com.andriod.reader.domain.Note
import com.andriod.reader.domain.SyncStatus
import com.andriod.reader.domain.TrashEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncStateStore: SyncStateStore,
    private val trashStore: TrashStore,
) {
    private val notesDir: File
        get() = File(context.filesDir, "notes").also { it.mkdirs() }

    private val trashDir: File
        get() = File(context.filesDir, "trash").also { it.mkdirs() }

    fun listNotes(): List<Note> {
        val files = listMarkdownFiles().sortedByDescending { it.lastModified() }
        val syncStates = syncStateStore.readAll()

        return files.map { file ->
            val relativePath = toRelativePath(file)
            val parsed = MarkdownParser.parse(relativePath, file.readText())
            val state = syncStates[relativePath]
            Note(
                id = parsed.id,
                title = parsed.title,
                content = parsed.content,
                fileName = relativePath,
                updatedAt = parsed.updatedAt,
                syncStatus = state?.syncStatus ?: SyncStatus.LOCAL_ONLY,
            )
        }
    }

    fun getNote(fileName: String): Note? {
        val file = resolveFile(fileName)
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
        val file = resolveFile(note.fileName)
        val updated = note.copy(updatedAt = Instant.now())
        file.parentFile?.mkdirs()
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
        resolveFile(fileName).writeText(MarkdownParser.serialize(note))
        val states = syncStateStore.readAll().toMutableMap()
        states[fileName] = com.andriod.reader.domain.SyncFileState(syncStatus = SyncStatus.LOCAL_ONLY)
        syncStateStore.writeAll(states)
        return note
    }

    fun moveToTrash(fileName: String): TrashEntry {
        val file = resolveFile(fileName)
        require(file.exists()) { "Note not found: $fileName" }
        val raw = file.readText()
        val states = syncStateStore.readAll().toMutableMap()
        val syncState = states[fileName] ?: com.andriod.reader.domain.SyncFileState()

        val id = UUID.randomUUID().toString()
        val trashFile = File(trashDir, "$id.md")
        trashFile.writeText(raw)
        file.delete()
        cleanupEmptyParents(file.parentFile)
        states.remove(fileName)
        syncStateStore.writeAll(states)

        val entry = TrashEntry(
            id = id,
            originalPath = fileName,
            deletedAt = Instant.now(),
            syncState = syncState,
        )
        trashStore.add(entry)
        return entry
    }

    fun restoreFromTrash(entryId: String) {
        val entry = trashStore.get(entryId) ?: return
        val trashFile = File(trashDir, "${entry.id}.md")
        require(trashFile.exists()) { "Trash file missing: $entryId" }

        val target = resolveFile(entry.originalPath)
        target.parentFile?.mkdirs()
        target.writeText(trashFile.readText())
        trashFile.delete()

        val states = syncStateStore.readAll().toMutableMap()
        states[entry.originalPath] = entry.syncState.copy(pendingDelete = false)
        syncStateStore.writeAll(states)
        trashStore.remove(entryId)
    }

    fun permanentDeleteFromTrash(entryId: String) {
        val entry = trashStore.get(entryId) ?: return
        val trashFile = File(trashDir, "${entry.id}.md")
        trashFile.delete()
        trashStore.remove(entryId)

        if (entry.syncState.githubSha != null) {
            val states = syncStateStore.readAll().toMutableMap()
            states[entry.originalPath] = entry.syncState.copy(
                pendingDelete = true,
                syncStatus = SyncStatus.PENDING,
            )
            syncStateStore.writeAll(states)
        }
    }

    fun listTrashEntries(): List<TrashEntry> = trashStore.listAll()

    fun readTrashRaw(entryId: String): String? {
        val entry = trashStore.get(entryId) ?: return null
        val trashFile = File(trashDir, "${entry.id}.md")
        return if (trashFile.exists()) trashFile.readText() else null
    }

    @Deprecated("Use moveToTrash", ReplaceWith("moveToTrash(fileName)"))
    fun deleteNote(fileName: String) {
        moveToTrash(fileName)
    }

    fun writeRawFile(fileName: String, rawContent: String) {
        val file = resolveFile(fileName)
        file.parentFile?.mkdirs()
        file.writeText(rawContent)
    }

    fun readRawFile(fileName: String): String? {
        val file = resolveFile(fileName)
        return if (file.exists()) file.readText() else null
    }

    fun deleteLocalFile(fileName: String) {
        val file = resolveFile(fileName)
        file.delete()
        cleanupEmptyParents(file.parentFile)
    }

    fun notesDirectory(): File = notesDir

    private fun listMarkdownFiles(): List<File> {
        val results = mutableListOf<File>()
        fun walk(dir: File) {
            dir.listFiles()?.forEach { file ->
                when {
                    file.isFile && file.name.endsWith(".md", ignoreCase = true) -> results.add(file)
                    file.isDirectory -> walk(file)
                }
            }
        }
        walk(notesDir)
        return results
    }

    private fun resolveFile(relativePath: String): File {
        val normalized = relativePath.replace('\\', '/').trimStart('/')
        val file = File(notesDir, normalized)
        val notesCanonical = notesDir.canonicalFile
        val targetCanonical = file.canonicalFile
        require(
            targetCanonical.path == notesCanonical.path ||
                targetCanonical.path.startsWith(notesCanonical.path + File.separator),
        ) { "Invalid note path: $relativePath" }
        return file
    }

    private fun toRelativePath(file: File): String =
        file.relativeTo(notesDir).path.replace('\\', '/')

    private fun cleanupEmptyParents(dir: File?) {
        var current = dir ?: return
        while (current.path != notesDir.path && current.exists() && current.listFiles()?.isEmpty() == true) {
            current.delete()
            current = current.parentFile ?: break
        }
    }
}
