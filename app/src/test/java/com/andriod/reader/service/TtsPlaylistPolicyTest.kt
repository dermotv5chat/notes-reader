package com.andriod.reader.service

import com.andriod.reader.domain.TtsPlaylistItem
import com.andriod.reader.domain.TtsQueueRepeatMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class TtsPlaylistPolicyTest {
    private val items = listOf(
        TtsPlaylistItem("a.md", "A", Instant.EPOCH),
        TtsPlaylistItem("b.md", "B", Instant.EPOCH),
    )

    @Test
    fun resolveAfterNoteFinished_offAdvancesToNextInQueue() {
        val action = TtsPlaylistPolicy.resolveAfterNoteFinished(
            repeatMode = TtsQueueRepeatMode.OFF,
            sleepTimer = TtsSleepTimerState(),
            currentFileName = "a.md",
            items = items,
            playingIndex = 0,
        )
        assertEquals(NoteFinishedAction.PlayAtIndex(1), action)
    }

    @Test
    fun resolveAfterNoteFinished_repeatAllWrapsToHead() {
        val action = TtsPlaylistPolicy.resolveAfterNoteFinished(
            repeatMode = TtsQueueRepeatMode.REPEAT_ALL,
            sleepTimer = TtsSleepTimerState(),
            currentFileName = "b.md",
            items = items,
            playingIndex = 1,
        )
        assertEquals(NoteFinishedAction.PlayAtIndex(0), action)
    }

    @Test
    fun resolveAfterNoteFinished_sleepTimerAfterNoteEndStops() {
        val action = TtsPlaylistPolicy.resolveAfterNoteFinished(
            repeatMode = TtsQueueRepeatMode.OFF,
            sleepTimer = TtsSleepTimerState(mode = SleepTimerMode.AfterNoteEnd),
            currentFileName = "a.md",
            items = items,
            playingIndex = 0,
        )
        assertEquals(NoteFinishedAction.Stop, action)
    }

    @Test
    fun resolveAfterNoteFinished_tempPlaybackStartsQueueWhenOff() {
        val action = TtsPlaylistPolicy.resolveAfterNoteFinished(
            repeatMode = TtsQueueRepeatMode.OFF,
            sleepTimer = TtsSleepTimerState(),
            currentFileName = "temp.md",
            items = items,
            playingIndex = null,
        )
        assertEquals(NoteFinishedAction.PlayAtIndex(0), action)
    }

    @Test
    fun canSelectRepeatAll_requiresNonEmptyQueue() {
        assertTrue(TtsPlaylistPolicy.canSelectRepeatAll(items))
        assertEquals(false, TtsPlaylistPolicy.canSelectRepeatAll(emptyList()))
    }
}
