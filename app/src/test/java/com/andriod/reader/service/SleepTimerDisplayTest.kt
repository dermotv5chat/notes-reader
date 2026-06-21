package com.andriod.reader.service

import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerDisplayTest {
    @Test
    fun formatFixedRemainingMs_roundsUpToSecond() {
        assertEquals("59:59", SleepTimerDisplay.formatFixedRemainingMs(3_599_000))
    }

    @Test
    fun formatFixedRemainingMs_minutesAndSeconds() {
        assertEquals("1:01", SleepTimerDisplay.formatFixedRemainingMs(61_000))
    }

    @Test
    fun formatFixedRemainingMs_zero() {
        assertEquals("0:00", SleepTimerDisplay.formatFixedRemainingMs(0))
    }

    @Test
    fun fixedCountdownLabel() {
        assertEquals("29:45 后关闭", SleepTimerDisplay.fixedCountdownLabel(1_785_000))
    }

    @Test
    fun afterNoteEndStatusLabel_withEstimate() {
        assertEquals(
            "本篇结束后关闭 · 约 35 分钟",
            SleepTimerDisplay.afterNoteEndStatusLabel(35),
        )
    }

    @Test
    fun afterNoteEndStatusLabel_withoutEstimate() {
        assertEquals(
            "本篇结束后关闭",
            SleepTimerDisplay.afterNoteEndStatusLabel(0),
        )
    }
}
