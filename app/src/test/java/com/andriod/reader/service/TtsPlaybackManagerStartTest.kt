package com.andriod.reader.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsPlaybackManagerStartTest {
    @Test
    fun shouldNotStartForegroundWhenPlaybackDidNotStart() {
        assertFalse(
            TtsPlaybackManager.shouldStartForegroundService(
                playbackStarted = false,
                withForegroundService = true,
            ),
        )
    }

    @Test
    fun shouldStartForegroundWhenPlaybackStarted() {
        assertTrue(
            TtsPlaybackManager.shouldStartForegroundService(
                playbackStarted = true,
                withForegroundService = true,
            ),
        )
    }

    @Test
    fun shouldRespectWithForegroundServiceFlag() {
        assertFalse(
            TtsPlaybackManager.shouldStartForegroundService(
                playbackStarted = true,
                withForegroundService = false,
            ),
        )
    }

    @Test
    fun noteHasReadableContent_rejectsBlankMarkdown() {
        assertFalse(TtsPlaybackManager.noteHasReadableContent(null))
        assertFalse(TtsPlaybackManager.noteHasReadableContent(""))
        assertFalse(TtsPlaybackManager.noteHasReadableContent("# \n\n"))
    }

    @Test
    fun noteHasReadableContent_acceptsPlainText() {
        assertTrue(TtsPlaybackManager.noteHasReadableContent("Hello world"))
    }
}
