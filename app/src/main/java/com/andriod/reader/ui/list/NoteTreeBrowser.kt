package com.andriod.reader.ui.list

import com.andriod.reader.domain.Note

object NoteTreeBrowser {
    sealed interface Entry {
        data class Folder(val name: String, val path: String) : Entry
        data class File(val note: Note) : Entry
    }

    fun listAt(notes: List<Note>, currentFolder: String, virtualFolders: Set<String> = emptySet()): List<Entry> {
        if (notes.isEmpty() && virtualFolders.isEmpty()) return emptyList()

        val current = normalizeFolder(currentFolder)
        val folders = linkedSetOf<String>()
        val files = mutableListOf<Note>()

        notes.forEach { note ->
            collectEntry(note.fileName.replace('\\', '/'), current, folders, files, note)
        }

        virtualFolders.forEach { folderPath ->
            collectVirtualFolder(folderPath.replace('\\', '/'), current, folders)
        }

        val folderEntries = folders.sorted().map { name ->
            val path = if (current.isEmpty()) name else "$current/$name"
            Entry.Folder(name, path)
        }
        val fileEntries = files
            .sortedByDescending { it.updatedAt }
            .map { Entry.File(it) }

        return folderEntries + fileEntries
    }

    fun parentFolder(currentFolder: String): String {
        val current = normalizeFolder(currentFolder)
        if (current.isEmpty()) return ""
        return current.substringBeforeLast('/', "")
    }

    fun displayFolderTitle(currentFolder: String): String {
        val current = normalizeFolder(currentFolder)
        return if (current.isEmpty()) "我的笔记" else current.substringAfterLast('/')
    }

    fun isSearchMode(query: String): Boolean = query.isNotBlank()

    private fun collectEntry(
        path: String,
        current: String,
        folders: MutableSet<String>,
        files: MutableList<Note>,
        note: Note,
    ) {
        if (current.isNotEmpty() && !path.startsWith("$current/")) return

        val relative = if (current.isEmpty()) path else path.removePrefix("$current/")
        if (relative.isEmpty()) return

        val slashIndex = relative.indexOf('/')
        if (slashIndex < 0) {
            files += note
        } else {
            folders += relative.substring(0, slashIndex)
        }
    }

    private fun collectVirtualFolder(
        folderPath: String,
        current: String,
        folders: MutableSet<String>,
    ) {
        if (current.isNotEmpty() && !folderPath.startsWith("$current/")) return

        val relative = if (current.isEmpty()) folderPath else folderPath.removePrefix("$current/")
        if (relative.isEmpty()) return

        val slashIndex = relative.indexOf('/')
        if (slashIndex < 0) {
            folders += relative
        } else {
            folders += relative.substring(0, slashIndex)
        }
    }

    private fun normalizeFolder(folder: String): String =
        folder.trim().trim('/').replace('\\', '/')
}
