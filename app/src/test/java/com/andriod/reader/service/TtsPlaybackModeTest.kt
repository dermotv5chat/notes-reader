package com.andriod.reader.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtsPlaybackModeTest {
    @Test
    fun displayLabel_presynth() {
        assertEquals("整篇预合成", TtsPlaybackMode.Presynth.displayLabel())
    }

    @Test
    fun displayLabel_segmentModes() {
        assertEquals("逐段 · 在线", TtsPlaybackMode.SegmentOnline.displayLabel())
        assertEquals("逐段 · 离线", TtsPlaybackMode.SegmentSherpa.displayLabel())
        assertEquals("逐段 · 系统", TtsPlaybackMode.SegmentSystem.displayLabel())
    }

    @Test
    fun displayLabel_noneHidden() {
        assertNull(TtsPlaybackMode.None.displayLabel())
    }
}
