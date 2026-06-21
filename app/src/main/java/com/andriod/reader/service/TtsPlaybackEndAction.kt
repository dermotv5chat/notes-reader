package com.andriod.reader.service

object TtsPlaybackEndAction {
    fun shouldRestartAfterLastSegment(
        loopEnabled: Boolean,
        sleepTimer: TtsSleepTimerState,
    ): Boolean = loopEnabled && !sleepTimer.isAfterNoteEnd

    fun shouldContinueForFixedTimerRemaining(sleepTimer: TtsSleepTimerState): Boolean =
        sleepTimer.isFixedActive && (sleepTimer.remainingMs ?: 0L) > 0L

    fun shouldContinueAfterLastSegment(
        loopEnabled: Boolean,
        sleepTimer: TtsSleepTimerState,
    ): Boolean = when {
        sleepTimer.isAfterNoteEnd -> false
        shouldContinueForFixedTimerRemaining(sleepTimer) -> true
        shouldRestartAfterLastSegment(loopEnabled, sleepTimer) -> true
        else -> false
    }
}
