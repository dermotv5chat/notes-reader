package com.andriod.reader.ui.list

import com.andriod.reader.domain.Note
import com.andriod.reader.domain.SyncStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class NoteTreeBrowserTest {
    private fun note(fileName: String) = Note(
        id = fileName,
        title = fileName,
        content = "",
        fileName = fileName,
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        syncStatus = SyncStatus.SYNCED,
    )

    @Test
    fun listAt_root_showsImmediateFoldersAndFiles() {
        val entries = NoteTreeBrowser.listAt(
            listOf(
                note("readme.md"),
                note("work/todo.md"),
                note("work/ideas/x.md"),
                note("personal/idea.md"),
            ),
            currentFolder = "",
        )

        assertEquals(
            listOf("personal", "work", "readme.md"),
            entries.map {
                when (it) {
                    is NoteTreeBrowser.Entry.Folder -> it.name
                    is NoteTreeBrowser.Entry.File -> it.note.fileName
                }
            },
        )
    }

    @Test
    fun listAt_nestedFolder_showsChildFolderAndDirectFiles() {
        val entries = NoteTreeBrowser.listAt(
            listOf(
                note("work/todo.md"),
                note("work/ideas/x.md"),
            ),
            currentFolder = "work",
        )

        assertEquals(
            listOf("ideas", "work/todo.md"),
            entries.map {
                when (it) {
                    is NoteTreeBrowser.Entry.Folder -> it.name
                    is NoteTreeBrowser.Entry.File -> it.note.fileName
                }
            },
        )
    }

    @Test
    fun parentFolder_returnsAncestor() {
        assertEquals("work", NoteTreeBrowser.parentFolder("work/ideas"))
        assertEquals("", NoteTreeBrowser.parentFolder("work"))
    }

    @Test
    fun listAt_includesVirtualEmptyFolder() {
        val entries = NoteTreeBrowser.listAt(
            notes = emptyList(),
            currentFolder = "work",
            virtualFolders = setOf("work/empty"),
        )

        assertEquals(
            listOf("empty"),
            entries.map {
                when (it) {
                    is NoteTreeBrowser.Entry.Folder -> it.name
                    is NoteTreeBrowser.Entry.File -> it.note.fileName
                }
            },
        )
    }

    @Test
    fun isSearchMode_whenQueryPresent() {
        assertTrue(NoteTreeBrowser.isSearchMode("todo"))
    }
}
