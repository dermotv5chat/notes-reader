package com.andriod.reader.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NotePathNamesTest {
    @Test
    fun validateSegment_rejectsSlashes() {
        assertThrows(IllegalArgumentException::class.java) {
            NotePathNames.validateSegment("a/b")
        }
    }

    @Test
    fun buildNotePath_keepsParentFolder() {
        assertEquals("work/note.md", NotePathNames.buildNotePath("work", "note"))
    }

    @Test
    fun buildNotePath_forNewNoteInFolder() {
        assertEquals(
            "work/2026-06-20-abc12345.md",
            NotePathNames.buildNotePath("work", "2026-06-20-abc12345"),
        )
    }

    @Test
    fun newNoteFileName_atRoot_hasNoFolderPrefix() {
        val path = NotePathNames.newNoteFileName("")
        assert(path.endsWith(".md"))
        assertFalse(path.contains('/'))
    }

    @Test
    fun newNoteFileName_inFolder_hasFolderPrefix() {
        val path = NotePathNames.newNoteFileName("work/inbox")
        assertTrue(path.startsWith("work/inbox/"))
        assertTrue(path.endsWith(".md"))
    }

    @Test
    fun buildFolderPath_atRoot() {
        assertEquals("inbox", NotePathNames.buildFolderPath("", "inbox"))
    }
}
