package com.andriod.reader.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
    fun buildFolderPath_atRoot() {
        assertEquals("inbox", NotePathNames.buildFolderPath("", "inbox"))
    }
}
