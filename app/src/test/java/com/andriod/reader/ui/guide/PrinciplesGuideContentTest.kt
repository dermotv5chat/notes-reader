package com.andriod.reader.ui.guide

import org.junit.Assert.assertTrue
import org.junit.Test

class PrinciplesGuideContentTest {
    @Test
    fun sections_includeCoreTopics() {
        val titles = PrinciplesGuideContent.sections.map { it.title }
        assertTrue(titles.contains("功能概览"))
        assertTrue(titles.contains("哪些块可点击"))
        assertTrue(titles.contains("- [ ] 与 - [x]"))
        assertTrue(titles.contains("[!rule] 与 [!habit]"))
        assertTrue(titles.contains("一行一条准则"))
        assertTrue(titles.contains("隐式块 ID"))
        assertTrue(titles.contains("历史记录"))
    }

    @Test
    fun sections_mentionToolbarAndSync() {
        val body = PrinciplesGuideContent.sections.flatMap { it.paragraphs }.joinToString("\n")
        assertTrue(body.contains("待办"))
        assertTrue(body.contains("准则"))
        assertTrue(body.contains("GitHub"))
        assertTrue(body.contains(".meta"))
        assertTrue(body.contains("自动折行"))
        assertTrue(body.contains("历史记录"))
    }
}
