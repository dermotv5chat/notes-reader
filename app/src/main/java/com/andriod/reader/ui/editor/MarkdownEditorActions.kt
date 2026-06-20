package com.andriod.reader.ui.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

enum class FormatAction {
    Bold,
    Italic,
    Strikethrough,
    BulletList,
    NumberedList,
    Checkbox,
    Heading,
}

object MarkdownEditorActions {
    fun apply(value: TextFieldValue, action: FormatAction): TextFieldValue {
        val result = when (action) {
            FormatAction.Bold -> wrap(value.text, value.selection, "**", "**")
            FormatAction.Italic -> wrap(value.text, value.selection, "*", "*")
            FormatAction.Strikethrough -> wrap(value.text, value.selection, "~~", "~~")
            FormatAction.BulletList -> toggleLinePrefix(value.text, value.selection, "- ")
            FormatAction.NumberedList -> toggleLinePrefix(value.text, value.selection, "1. ")
            FormatAction.Checkbox -> toggleLinePrefix(value.text, value.selection, "- [ ] ")
            FormatAction.Heading -> toggleLinePrefix(value.text, value.selection, "## ")
        }
        return TextFieldValue(result.text, result.selection)
    }

    internal data class EditResult(
        val text: String,
        val selection: TextRange,
    )

    internal fun wrap(
        text: String,
        selection: TextRange,
        prefix: String,
        suffix: String,
    ): EditResult {
        if (selection.collapsed) {
            val insert = prefix + suffix
            val newText = text.replaceRange(selection.start, selection.end, insert)
            val cursor = selection.start + prefix.length
            return EditResult(newText, TextRange(cursor, cursor))
        }

        val selected = text.substring(selection.min, selection.max)
        val insert = prefix + selected + suffix
        val newText = text.replaceRange(selection.min, selection.max, insert)
        val newEnd = selection.min + insert.length
        return EditResult(newText, TextRange(selection.min, newEnd))
    }

    internal fun toggleLinePrefix(
        text: String,
        selection: TextRange,
        prefix: String,
    ): EditResult {
        val (regionStart, regionEnd) = affectedLineBounds(text, selection)
        val region = text.substring(regionStart, regionEnd)
        val lines = if (region.isEmpty()) listOf("") else region.split('\n')

        val allHavePrefix = lines.all { line -> line.startsWith(prefix) }
        val newLines = lines.map { line ->
            if (allHavePrefix) {
                if (line.startsWith(prefix)) line.removePrefix(prefix) else line
            } else {
                if (line.startsWith(prefix)) line else prefix + line
            }
        }
        val newRegion = newLines.joinToString("\n")
        val newText = text.substring(0, regionStart) + newRegion + text.substring(regionEnd)
        return EditResult(newText, TextRange(regionStart, regionStart + newRegion.length))
    }

    private fun affectedLineBounds(text: String, selection: TextRange): Pair<Int, Int> {
        val start = selection.min
        val end = selection.max

        val regionStart = text.lastIndexOf('\n', start - 1).let { if (it == -1) 0 else it + 1 }
        val anchor = if (selection.collapsed) start else (end - 1).coerceAtLeast(start)
        val regionEnd = text.indexOf('\n', anchor).let { if (it == -1) text.length else it }
        return regionStart to regionEnd
    }
}
