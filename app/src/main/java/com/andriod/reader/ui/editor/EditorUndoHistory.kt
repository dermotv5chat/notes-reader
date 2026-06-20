package com.andriod.reader.ui.editor

import androidx.compose.ui.text.input.TextFieldValue

data class EditorSnapshot(
    val title: String,
    val body: TextFieldValue,
)

class EditorUndoHistory(
    private val maxSize: Int = 50,
) {
    private val undoStack = ArrayDeque<EditorSnapshot>()
    private val redoStack = ArrayDeque<EditorSnapshot>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    fun recordBeforeChange(snapshot: EditorSnapshot) {
        if (undoStack.lastOrNull() == snapshot) return
        undoStack.addLast(snapshot)
        while (undoStack.size > maxSize) {
            undoStack.removeFirst()
        }
        redoStack.clear()
    }

    fun undo(current: EditorSnapshot): EditorSnapshot? {
        if (undoStack.isEmpty()) return null
        redoStack.addLast(current)
        return undoStack.removeLast()
    }

    fun redo(current: EditorSnapshot): EditorSnapshot? {
        if (redoStack.isEmpty()) return null
        undoStack.addLast(current)
        return redoStack.removeLast()
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }
}
