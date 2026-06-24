package com.andriod.reader.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MarkdownPlainTextTest {
    @Test
    fun stripForSpeech_removesBoldMarkers() {
        val result = MarkdownPlainText.stripForSpeech("豫章**故郡**，洪都新府。")
        assertEquals("豫章故郡，洪都新府。", result)
    }

    @Test
    fun stripForSpeech_skipsStrikethroughContent() {
        val result = MarkdownPlainText.stripForSpeech("保留~~删除线~~继续。")
        assertEquals("保留继续。", result)
        assertFalse(result.contains("删除线"))
    }

    @Test
    fun stripForSpeech_stripsHeadingPrefix() {
        val result = MarkdownPlainText.stripForSpeech("## 滕王阁序\n正文。")
        assertEquals("滕王阁序\n正文。", result)
    }

    @Test
    fun stripForSpeech_stripsCheckboxPrefix() {
        val result = MarkdownPlainText.stripForSpeech("- [ ] 买牛奶\n- [x] 已完成")
        assertEquals("买牛奶\n已完成", result)
    }

    @Test
    fun stripForSpeech_stripsStarListPrefix() {
        val result = MarkdownPlainText.stripForSpeech("* 第一项\n- 第二项")
        assertEquals("第一项\n第二项", result)
    }

    @Test
    fun stripForSpeech_stripsListPrefixes() {
        val result = MarkdownPlainText.stripForSpeech("- 第一项\n1. 第二项")
        assertEquals("第一项\n第二项", result)
    }

    @Test
    fun stripForSpeech_stripsHabitCallout() {
        val result = MarkdownPlainText.stripForSpeech("> [!habit]  11点睡觉，你没有晚睡的资本；")
        assertEquals("11点睡觉，你没有晚睡的资本；", result)
    }

    @Test
    fun stripForSpeech_stripsRuleCalloutAndBlockAnchor() {
        val result = MarkdownPlainText.stripForSpeech("> [!rule] 别诉苦 ^no-complain")
        assertEquals("别诉苦", result)
    }

    @Test
    fun stripForSpeech_handlesMixedFormatting() {
        val raw = """
            ## 标题
            **加粗**与*斜体*，~~不读我~~结束。
            - [ ] 待办事项
        """.trimIndent()

        val result = MarkdownPlainText.stripForSpeech(raw)
        assertEquals(
            """
            标题
            加粗与斜体，结束。
            待办事项
            """.trimIndent(),
            result,
        )
    }
}
