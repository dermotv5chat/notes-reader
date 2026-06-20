package com.andriod.reader.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownEditorActionsTest {
    @Test
    fun wrap_withSelection_addsBoldMarkers() {
        val value = TextFieldValue("hello world", TextRange(0, 5))
        val result = MarkdownEditorActions.apply(value, FormatAction.Bold)
        assertEquals("**hello** world", result.text)
        assertEquals(TextRange(0, 9), result.selection)
    }

    @Test
    fun wrap_withoutSelection_insertsMarkersWithCursorInside() {
        val value = TextFieldValue("hello", TextRange(2, 2))
        val result = MarkdownEditorActions.apply(value, FormatAction.Bold)
        assertEquals("he****llo", result.text)
        assertEquals(TextRange(4, 4), result.selection)
    }

    @Test
    fun italic_wrapsSelectedText() {
        val value = TextFieldValue("abc", TextRange(1, 2))
        val result = MarkdownEditorActions.apply(value, FormatAction.Italic)
        assertEquals("a*b*c", result.text)
        assertEquals(TextRange(1, 4), result.selection)
    }

    @Test
    fun bulletList_addsPrefixToCurrentLine() {
        val value = TextFieldValue("line one\nline two", TextRange(10, 10))
        val result = MarkdownEditorActions.apply(value, FormatAction.BulletList)
        assertEquals("line one\n- line two", result.text)
    }

    @Test
    fun bulletList_toggleRemovesPrefix() {
        val value = TextFieldValue("- item", TextRange(2, 2))
        val result = MarkdownEditorActions.apply(value, FormatAction.BulletList)
        assertEquals("item", result.text)
    }

    @Test
    fun bulletList_appliesToAllLinesInSelection() {
        val value = TextFieldValue("a\nb\nc", TextRange(0, 3))
        val result = MarkdownEditorActions.apply(value, FormatAction.BulletList)
        assertEquals("- a\n- b\nc", result.text)
    }

    @Test
    fun checkbox_addsTodoPrefix() {
        val value = TextFieldValue("buy milk", TextRange(0, 0))
        val result = MarkdownEditorActions.apply(value, FormatAction.Checkbox)
        assertEquals("- [ ] buy milk", result.text)
    }

    @Test
    fun heading_addsHashPrefix() {
        val value = TextFieldValue("Section", TextRange(0, 0))
        val result = MarkdownEditorActions.apply(value, FormatAction.Heading)
        assertEquals("## Section", result.text)
    }

    @Test
    fun strikethrough_wrapsSelection() {
        val value = TextFieldValue("delete me", TextRange(0, 6))
        val result = MarkdownEditorActions.apply(value, FormatAction.Strikethrough)
        assertEquals("~~delete~~ me", result.text)
    }
}
