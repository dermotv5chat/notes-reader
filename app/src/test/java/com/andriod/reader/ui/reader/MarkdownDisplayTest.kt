package com.andriod.reader.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownDisplayTest {
    private val baseStyle = TextStyle(fontSize = 16.sp, color = Color.Black)

    @Test
    fun buildMarkdown_showsBoldWithoutMarkers() {
        val result = buildMarkdownAnnotatedString("**胜饯**", baseStyle)
        assertEquals("胜饯", result.text)
        assertTrue(result.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
    }

    @Test
    fun buildMarkdown_rendersHeadingWithoutHash() {
        val result = buildMarkdownAnnotatedString("## 滕王阁序", baseStyle)
        assertEquals("滕王阁序", result.text)
    }

    @Test
    fun buildMarkdown_rendersCheckboxGlyph() {
        val result = buildMarkdownAnnotatedString("- [ ] 买牛奶", baseStyle)
        assertEquals("☐ 买牛奶", result.text)
    }
}
