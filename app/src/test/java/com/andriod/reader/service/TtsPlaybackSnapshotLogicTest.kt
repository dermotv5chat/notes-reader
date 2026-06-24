package com.andriod.reader.service

import org.junit.Assert.assertEquals
import org.junit.Test

class TtsPlaybackSnapshotLogicTest {
    @Test
    fun presynthActive_usesPresynthSegmentTotal() {
        assertEquals(1, TtsPlaybackSnapshotLogic.segmentTotal(true, 1, 0))
        assertEquals(3, TtsPlaybackSnapshotLogic.segmentTotal(true, 3, 0))
    }

    @Test
    fun presynthActive_coercesZeroToOne() {
        assertEquals(1, TtsPlaybackSnapshotLogic.segmentTotal(true, 0, 0))
    }

    @Test
    fun systemMode_usesSegmentsSize() {
        assertEquals(5, TtsPlaybackSnapshotLogic.segmentTotal(false, 0, 5))
        assertEquals(0, TtsPlaybackSnapshotLogic.segmentTotal(false, 3, 0))
    }
}
