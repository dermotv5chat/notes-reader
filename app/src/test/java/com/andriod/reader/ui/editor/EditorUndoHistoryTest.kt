package com.andriod.reader.ui.editor

import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorUndoHistoryTest {
    private fun snapshot(title: String, body: String) =
        EditorSnapshot(title, TextFieldValue(body))

    @Test
    fun undo_redo_roundTrip() {
        val history = EditorUndoHistory()
        val initial = snapshot("标题", "第一段")
        val edited = snapshot("标题", "第二段")

        history.recordBeforeChange(initial)
        val undone = history.undo(edited)
        assertEquals(initial, undone)
        assertTrue(history.canRedo)

        val redone = history.redo(initial)
        assertEquals(edited, redone)
        assertFalse(history.canRedo)
    }

    @Test
    fun record_clearsRedoStack() {
        val history = EditorUndoHistory()
        val initial = snapshot("标题", "a")
        val edited = snapshot("标题", "ab")

        history.recordBeforeChange(initial)
        history.undo(edited)
        assertTrue(history.canRedo)

        history.recordBeforeChange(snapshot("标题", "a"))
        assertFalse(history.canRedo)
    }

    @Test
    fun undo_whenEmpty_returnsNull() {
        val history = EditorUndoHistory()
        assertEquals(null, history.undo(snapshot("标题", "正文")))
    }
}
