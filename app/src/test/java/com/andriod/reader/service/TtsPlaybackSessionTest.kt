package com.andriod.reader.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsPlaybackSessionTest {
    @Test
    fun hasActiveSession_whenPlaying() {
        val session = TtsPlaybackSession(
            fileName = "note.md",
            title = "测试",
            segmentIndex = 0,
            segmentTotal = 5,
            isPlaying = true,
            isPaused = false,
        )
        assertTrue(session.hasActiveSession)
    }

    @Test
    fun hasActiveSession_whenPaused() {
        val session = TtsPlaybackSession(
            fileName = "note.md",
            title = "测试",
            segmentIndex = 2,
            segmentTotal = 5,
            isPlaying = false,
            isPaused = true,
        )
        assertTrue(session.hasActiveSession)
    }

    @Test
    fun noActiveSession_whenStopped() {
        val session = TtsPlaybackSession()
        assertFalse(session.hasActiveSession)
    }

    @Test
    fun noActiveSession_whenFileNameMissing() {
        val session = TtsPlaybackSession(
            segmentTotal = 3,
            isPlaying = true,
        )
        assertFalse(session.hasActiveSession)
    }
}
