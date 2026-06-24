package com.andriod.reader.service

import com.andriod.reader.domain.TtsPlaylistItem
import com.andriod.reader.domain.TtsQueueRepeatMode

sealed class NoteFinishedAction {
    data object Stop : NoteFinishedAction()
    data class PlayAtIndex(val index: Int) : NoteFinishedAction()
}

object TtsPlaylistPolicy {
    fun resolveAfterNoteFinished(
        repeatMode: TtsQueueRepeatMode,
        sleepTimer: TtsSleepTimerState,
        currentFileName: String,
        items: List<TtsPlaylistItem>,
        playingIndex: Int?,
    ): NoteFinishedAction {
        if (sleepTimer.isAfterNoteEnd) return NoteFinishedAction.Stop
        if (repeatMode == TtsQueueRepeatMode.REPEAT_ONE) {
            return NoteFinishedAction.Stop
        }
        if (items.isEmpty()) return NoteFinishedAction.Stop

        val currentInQueue = items.indexOfFirst { it.fileName == currentFileName }
        val baseIndex = playingIndex ?: currentInQueue

        return when (repeatMode) {
            TtsQueueRepeatMode.REPEAT_ALL -> {
                val next = when {
                    baseIndex >= 0 -> (baseIndex + 1) % items.size
                    else -> 0
                }
                NoteFinishedAction.PlayAtIndex(next)
            }
            TtsQueueRepeatMode.OFF -> {
                val next = when {
                    baseIndex >= 0 && baseIndex + 1 < items.size -> baseIndex + 1
                    baseIndex < 0 -> 0
                    else -> return NoteFinishedAction.Stop
                }
                NoteFinishedAction.PlayAtIndex(next)
            }
            TtsQueueRepeatMode.REPEAT_ONE -> NoteFinishedAction.Stop
        }
    }

    fun canSelectRepeatAll(items: List<TtsPlaylistItem>): Boolean = items.isNotEmpty()
}
