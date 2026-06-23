package com.andriod.reader.data.local

import android.content.Context
import com.andriod.reader.data.repository.SyncPathUtils
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
    private val folderStore: FolderStore,
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
        val cleanedContent = MarkdownCalloutCleaner.stripLegacyAnchors(note.content)
        val updated = note.copy(content = cleanedContent, updatedAt = Instant.now())
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

    /** Replace one line without changing title/front matter. */
    fun updateRawLine(fileName: String, lineIndex: Int, newLine: String) {
        val file = resolveFile(fileName)
        if (!file.exists()) return
        val body = MarkdownParser.parse(fileName, file.readText()).content
        val lines = body.split('\n').toMutableList()
        if (lineIndex !in lines.indices) return
        lines[lineIndex] = newLine
        val note = getNote(fileName) ?: return
        saveNote(note.copy(content = lines.joinToString("\n")))
    }

    fun createNote(title: String, content: String, parentFolder: String = ""): Note {
        val fileName = NotePathNames.newNoteFileName(parentFolder)
        val note = Note(
            id = fileName.removeSuffix(".md").substringAfterLast('/'),
            title = title.ifBlank { "无标题" },
            content = content,
            fileName = fileName,
            updatedAt = Instant.now(),
            syncStatus = SyncStatus.LOCAL_ONLY,
        )
        val file = resolveFile(fileName)
        file.parentFile?.mkdirs()
        file.writeText(MarkdownParser.serialize(note))
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
        if (syncState.githubSha != null) {
            states[fileName] = syncState.copy(
                pendingDelete = true,
                syncStatus = SyncStatus.PENDING,
            )
        } else {
            states.remove(fileName)
        }
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

    fun listVirtualFolders(): Set<String> = folderStore.listAll()

    fun createFolder(parentFolder: String, folderName: String): String {
        val folderPath = NotePathNames.buildFolderPath(parentFolder, folderName)
        require(!isFolderPathTaken(folderPath)) { "同目录下已存在同名文件夹" }
        val parent = NotePathNames.parentPath(folderPath)
        if (parent.isNotEmpty()) {
            folderStore.add(parent)
        }
        folderStore.add(folderPath)
        resolveFile(folderPath).mkdirs()
        return folderPath
    }

    fun renameNoteFile(fileName: String, newBaseName: String): String {
        val newPath = NotePathNames.buildNotePath(
            parentFolder = NotePathNames.parentPath(fileName),
            baseName = newBaseName,
        )
        require(newPath != fileName) { "名称未改变" }
        require(!resolveFile(newPath).exists()) { "同目录下已存在同名文件" }
        val source = resolveFile(fileName)
        require(source.exists()) { "笔记不存在" }

        val dest = resolveFile(newPath)
        dest.parentFile?.mkdirs()
        check(source.renameTo(dest)) { "重命名失败" }

        migrateSyncState(fileName, newPath)
        return newPath
    }

    fun renameFolder(folderPath: String, newFolderName: String): String {
        val oldPath = SyncPathUtils.normalize(folderPath)
        val parent = NotePathNames.parentPath(oldPath)
        val newPath = NotePathNames.buildFolderPath(parent, newFolderName)
        require(newPath != oldPath) { "名称未改变" }
        require(!hasSiblingFolderConflict(newPath, oldPath)) { "同目录下已存在同名文件夹" }

        val affected = listRelativePaths().filter { path ->
            path == oldPath || path.startsWith("$oldPath/")
        }
        val notePaths = affected.filter { it.endsWith(".md", ignoreCase = true) }
        val newNotePath = { path: String ->
            when {
                path == oldPath -> newPath
                path.startsWith("$oldPath/") -> path.replaceFirst(oldPath, newPath)
                else -> path
            }
        }

        notePaths.forEach { oldNotePath ->
            val targetPath = newNotePath(oldNotePath)
            val source = resolveFile(oldNotePath)
            val dest = resolveFile(targetPath)
            dest.parentFile?.mkdirs()
            check(source.renameTo(dest)) { "重命名失败：$oldNotePath" }
            migrateSyncState(oldNotePath, targetPath)
        }

        folderStore.renamePrefix(oldPath, newPath)
        return newPath
    }

    fun countNotesUnderFolder(folderPath: String): Int {
        val prefix = SyncPathUtils.normalize(folderPath)
        return listRelativePaths().count { path ->
            path.endsWith(".md", ignoreCase = true) && path.startsWith("$prefix/")
        }
    }

    private fun isFolderPathTaken(folderPath: String): Boolean {
        val norm = SyncPathUtils.normalize(folderPath)
        if (folderStore.listAll().contains(norm)) return true
        return listRelativePaths().any { it.startsWith("$norm/") }
    }

    private fun hasSiblingFolderConflict(newPath: String, oldPath: String): Boolean {
        val newNorm = SyncPathUtils.normalize(newPath)
        val oldNorm = SyncPathUtils.normalize(oldPath)
        if (newNorm == oldNorm) return false
        if (folderStore.listAll().any { it == newNorm && !it.startsWith("$oldNorm/") }) {
            return true
        }
        return listRelativePaths().any { it.startsWith("$newNorm/") && !it.startsWith("$oldNorm/") }
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

    private fun listRelativePaths(): List<String> =
        listMarkdownFiles().map { toRelativePath(it) }

    private fun migrateSyncState(oldPath: String, newPath: String) {
        val states = syncStateStore.readAll().toMutableMap()
        val state = states.remove(oldPath) ?: return
        val newStatus = when (state.syncStatus) {
            SyncStatus.SYNCED, SyncStatus.PENDING -> SyncStatus.PENDING
            else -> SyncStatus.LOCAL_ONLY
        }
        states[newPath] = state.copy(
            syncStatus = newStatus,
            remotePath = SyncPathUtils.normalize(newPath),
            githubSha = null,
            pendingDelete = false,
        )
        syncStateStore.writeAll(states)
    }

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
