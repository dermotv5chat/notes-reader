package com.andriod.reader.data.local

import com.andriod.reader.domain.NoteBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownBlockParserTest {
    @Test
    fun parseLine_calloutWithVariant() {
        val block = MarkdownBlockParser.parseLine(
            fileName = "principles.md",
            lineIndex = 2,
            rawLine = "> [!rule|when] 冲突时先暂停 ^pause",
        )
        assertTrue(block is NoteBlock.Callout)
        block as NoteBlock.Callout
        assertEquals("rule", block.variant)
        assertEquals("冲突时先暂停", block.text)
        assertEquals("principles.md^pause", block.id)
        assertTrue(block.trackable)
    }

    @Test
    fun parseLine_todoUnchecked() {
        val block = MarkdownBlockParser.parseLine(
            fileName = "note.md",
            lineIndex = 0,
            rawLine = "- [ ] 11 点睡觉",
        )
        assertTrue(block is NoteBlock.Todo)
        block as NoteBlock.Todo
        assertEquals(false, block.checked)
        assertEquals("11 点睡觉", block.text)
        assertTrue(block.trackable)
    }

    @Test
    fun parseLine_headingNotTrackable() {
        val block = MarkdownBlockParser.parseLine(
            fileName = "note.md",
            lineIndex = 0,
            rawLine = "# 健康作息",
        )
        assertTrue(block is NoteBlock.Heading)
        assertEquals(false, block.trackable)
    }

    @Test
    fun parse_parsesMultipleLines() {
        val content = """
            # 标题
            > [!habit] 每天冥想
            - [ ] 任务
        """.trimIndent()
        val blocks = MarkdownBlockParser.parse(content, "n.md")
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is NoteBlock.Heading)
        assertTrue(blocks[1] is NoteBlock.Callout)
        assertTrue(blocks[2] is NoteBlock.Todo)
    }
}
