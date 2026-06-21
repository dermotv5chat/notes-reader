package com.andriod.reader.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsPlaybackEndActionTest {
    @Test
    fun shouldRestartAfterLastSegment_whenLoopEnabled() {
        assertTrue(
            TtsPlaybackEndAction.shouldRestartAfterLastSegment(
                loopEnabled = true,
                sleepTimer = TtsSleepTimerState(),
            ),
        )
    }

    @Test
    fun shouldStopAfterLastSegment_whenLoopDisabled() {
        assertFalse(
            TtsPlaybackEndAction.shouldRestartAfterLastSegment(
                loopEnabled = false,
                sleepTimer = TtsSleepTimerState(),
            ),
        )
    }

    @Test
    fun shouldStopAfterLastSegment_whenSleepTimerAfterNoteEnd() {
        assertFalse(
            TtsPlaybackEndAction.shouldRestartAfterLastSegment(
                loopEnabled = true,
                sleepTimer = TtsSleepTimerState(mode = SleepTimerMode.AfterNoteEnd),
            ),
        )
    }

    @Test
    fun shouldRestartAfterLastSegment_whenFixedSleepTimerActiveAndLoopEnabled() {
        assertTrue(
            TtsPlaybackEndAction.shouldRestartAfterLastSegment(
                loopEnabled = true,
                sleepTimer = TtsSleepTimerState(
                    mode = SleepTimerMode.FixedMinutes,
                    remainingMs = 60_000L,
                ),
            ),
        )
    }

    @Test
    fun shouldContinueForFixedTimerRemaining_whenFixedTimerActive() {
        assertTrue(
            TtsPlaybackEndAction.shouldContinueForFixedTimerRemaining(
                TtsSleepTimerState(
                    mode = SleepTimerMode.FixedMinutes,
                    remainingMs = 60_000L,
                ),
            ),
        )
    }

    @Test
    fun shouldNotContinueForFixedTimerRemaining_whenAfterNoteEnd() {
        assertFalse(
            TtsPlaybackEndAction.shouldContinueForFixedTimerRemaining(
                TtsSleepTimerState(mode = SleepTimerMode.AfterNoteEnd),
            ),
        )
    }

    @Test
    fun shouldContinueAfterLastSegment_whenFixedTimerLongerThanNoteWithoutLoop() {
        assertTrue(
            TtsPlaybackEndAction.shouldContinueAfterLastSegment(
                loopEnabled = false,
                sleepTimer = TtsSleepTimerState(
                    mode = SleepTimerMode.FixedMinutes,
                    remainingMs = 30 * 60_000L,
                ),
            ),
        )
    }

    @Test
    fun shouldStopAfterLastSegment_whenAfterNoteEndMode() {
        assertFalse(
            TtsPlaybackEndAction.shouldContinueAfterLastSegment(
                loopEnabled = true,
                sleepTimer = TtsSleepTimerState(mode = SleepTimerMode.AfterNoteEnd),
            ),
        )
    }
}
