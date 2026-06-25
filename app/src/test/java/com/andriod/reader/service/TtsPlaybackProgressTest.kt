package com.andriod.reader.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsPlaybackProgressTest {
    @Test
    fun formatPlaybackTime_zeroAndShort() {
        assertEquals("0:00", TtsPlaybackProgress.formatPlaybackTime(0L))
        assertEquals("1:05", TtsPlaybackProgress.formatPlaybackTime(65_000L))
    }

    @Test
    fun formatPlaybackTime_overOneHour() {
        assertEquals("1:01:05", TtsPlaybackProgress.formatPlaybackTime(3_665_000L))
    }

    @Test
    fun formatPlaybackTime_negativeShowsPlaceholder() {
        assertEquals("--:--", TtsPlaybackProgress.formatPlaybackTime(-1L))
    }

    @Test
    fun formatProgressRange() {
        assertEquals("0:42 / 5:30", TtsPlaybackProgress.formatProgressRange(42_000L, 330_000L))
    }

    @Test
    fun computeNoteDurationMs_fromSegments() {
        val segments = listOf("a".repeat(150))
        val duration = TtsPlaybackProgress.computeNoteDurationMs(segments, speechRate = 1f)
        assertTrue(duration >= 60_000L)
    }

    @Test
    fun overallExoPositionMs_addsCompletedAndCurrent() {
        val position = TtsPlaybackProgress.overallExoPositionMs(
            completedMs = 30_000L,
            currentSegmentPositionMs = 12_000L,
            isPaused = false,
            frozenPositionMs = 0L,
        )
        assertEquals(42_000L, position)
    }

    @Test
    fun overallExoPositionMs_whenPausedUsesFrozen() {
        val position = TtsPlaybackProgress.overallExoPositionMs(
            completedMs = 30_000L,
            currentSegmentPositionMs = 12_000L,
            isPaused = true,
            frozenPositionMs = 35_000L,
        )
        assertEquals(35_000L, position)
    }

    @Test
    fun computeNotePositionMs_systemSegmentCapsAtEstimate() {
        val position = TtsPlaybackProgress.computeNotePositionMs(
            completedMs = 10_000L,
            currentSegmentPositionMs = 0L,
            segmentStartElapsedMs = 90_000L,
            segmentEstimateMs = 60_000L,
            isPaused = false,
            frozenPositionMs = 0L,
        )
        assertEquals(70_000L, position)
    }

    @Test
    fun progressFraction_boundaries() {
        assertEquals(0f, TtsPlaybackProgress.progressFraction(0L, 100L), 0.001f)
        assertEquals(0.5f, TtsPlaybackProgress.progressFraction(50L, 100L), 0.001f)
        assertEquals(1f, TtsPlaybackProgress.progressFraction(150L, 100L), 0.001f)
        assertEquals(0f, TtsPlaybackProgress.progressFraction(10L, 0L), 0.001f)
    }

    @Test
    fun sessionProgressFraction_matchesPositionAndDuration() {
        val session = TtsPlaybackSession(
            fileName = "note.md",
            title = "测试",
            segmentTotal = 3,
            isPlaying = true,
            positionMs = 30_000L,
            durationMs = 120_000L,
        )
        assertEquals(0.25f, session.progressFraction, 0.001f)
    }

    @Test
    fun estimateTotalDurationMs_emptySegments() {
        assertEquals(0L, TtsRemainingEstimate.estimateTotalDurationMs(emptyList(), 1f))
    }

    @Test
    fun estimateSegmentDurationMs_minimumOneSecond() {
        assertTrue(TtsRemainingEstimate.estimateSegmentDurationMs("x", 1f) >= 1_000L)
    }
}
