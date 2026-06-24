package com.andriod.reader.service.synthesis

import com.andriod.reader.domain.TtsSpeechBackend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsParagraphSplitterTest {
    @Test
    fun shortText_returnsSingleChunk() {
        val text = "这是一段短笔记。"
        assertEquals(listOf(text), TtsParagraphSplitter.split(text))
    }

    @Test
    fun splitsOnParagraphBoundaries() {
        val p1 = "第一段内容。".repeat(400)
        val p2 = "第二段内容。".repeat(400)
        val text = "$p1\n\n$p2"
        val chunks = TtsParagraphSplitter.split(text, maxChars = 4000)
        assertTrue(chunks.size >= 2)
        chunks.forEach { assertTrue(it.length <= 4000) }
    }

    @Test
    fun longParagraph_splitsBySentencesWithoutExceedingMax() {
        val sentence = "这是很长的一句话，用于测试。"
        val text = sentence.repeat(400)
        val chunks = TtsParagraphSplitter.split(text, maxChars = 4000)
        assertTrue(chunks.size > 1)
        chunks.forEach { assertTrue(it.length <= 4000) }
    }

    @Test
    fun emptyText_returnsEmpty() {
        assertTrue(TtsParagraphSplitter.split("   ").isEmpty())
    }
}
