package com.andriod.reader.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsPlaybackEndActionTest {
    @Test
    fun shouldRestartAfterLastSegment_whenLoopEnabled() {
        assertTrue(TtsPlaybackEndAction.shouldRestartAfterLastSegment(loopEnabled = true))
    }

    @Test
    fun shouldStopAfterLastSegment_whenLoopDisabled() {
        assertFalse(TtsPlaybackEndAction.shouldRestartAfterLastSegment(loopEnabled = false))
    }
}
