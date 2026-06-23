package com.andriod.reader.domain

import com.andriod.reader.data.local.MarkdownBlockParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteBlockDisplayTest {
    @Test
    fun shouldDisplayInReader_blankParagraphIsFalse() {
        val block = MarkdownBlockParser.parseLine("n.md", 1, "")
        assertTrue(block is NoteBlock.Paragraph)
        assertFalse(block.shouldDisplayInReader())
    }

    @Test
    fun shouldDisplayInReader_whitespaceOnlyParagraphIsFalse() {
        val block = MarkdownBlockParser.parseLine("n.md", 1, "   ")
        assertFalse(block.shouldDisplayInReader())
    }

    @Test
    fun shouldDisplayInReader_trackableBlockWithTextIsTrue() {
        val block = MarkdownBlockParser.parseLine("n.md", 0, "> [!rule] 测试")
        assertTrue(block.shouldDisplayInReader())
    }

    @Test
    fun parse_filtersBlankLinesForDisplay() {
        val content = "# 标题\n\n> [!rule] 准则\n\n"
        val visible = MarkdownBlockParser.parse(content, "n.md", listOf("callout-id")).filter { it.shouldDisplayInReader() }
        assertEquals(2, visible.size)
    }
}
