package com.andriod.reader.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MarkdownParserTest {
    @Test
    fun parse_withFrontMatter() {
        val raw = """
            ---
            id: "abc123"
            title: "测试标题"
            updatedAt: "2026-06-20T10:30:00Z"
            ---
            第一段。

            第二段。
        """.trimIndent()

        val parsed = MarkdownParser.parse("2026-06-20-abc123.md", raw)
        assertEquals("abc123", parsed.id)
        assertEquals("测试标题", parsed.title)
        assertTrue(parsed.content.contains("第一段"))
        assertEquals(Instant.parse("2026-06-20T10:30:00Z"), parsed.updatedAt)
    }

    @Test
    fun serialize_roundTrip() {
        val note = com.andriod.reader.domain.Note(
            id = "id1",
            title = "Hello",
            content = "World",
            fileName = "note.md",
            updatedAt = Instant.parse("2026-06-20T10:30:00Z"),
            syncStatus = com.andriod.reader.domain.SyncStatus.LOCAL_ONLY,
        )
        val serialized = MarkdownParser.serialize(note)
        val parsed = MarkdownParser.parse("note.md", serialized)
        assertEquals(note.id, parsed.id)
        assertEquals(note.title, parsed.title)
        assertEquals(note.content, parsed.content)
    }
}
