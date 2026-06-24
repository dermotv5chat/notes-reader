package com.andriod.reader.service.synthesis

import android.content.Context
import com.andriod.reader.data.local.AppDiagnosticLog
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.domain.TtsSpeechBackend
import com.andriod.reader.service.edge.ExoPlayerSpeechPlayer
import com.andriod.reader.service.edge.NetworkAvailability

/**
 * Plays presynthesized audio files (Edge / Sherpa) via ExoPlayer.
 */
class PresynthPlaybackBackend(
    context: Context,
    private val settingsStore: SettingsStore,
    private val diagnosticLog: AppDiagnosticLog,
) {
    private val appContext = context.applicationContext
    private val player = ExoPlayerSpeechPlayer(appContext, diagnosticLog)
    private var speechRate = 1.0f

    fun isBackendActive(): Boolean {
        val backend = settingsStore.getTtsSpeechBackend()
        return backend == TtsSpeechBackend.ONLINE_EDGE || backend == TtsSpeechBackend.OFFLINE_SHERPA
    }

    fun canPrepare(): Boolean = when (settingsStore.getTtsSpeechBackend()) {
        TtsSpeechBackend.ONLINE_EDGE -> NetworkAvailability.isConnected(appContext)
        TtsSpeechBackend.OFFLINE_SHERPA -> true
        TtsSpeechBackend.SYSTEM -> false
    }

    fun playResult(
        result: TtsPreSynthResult,
        onChunkChanged: (Int, Int) -> Unit,
        onAllDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        player.setSpeechRate(speechRate)
        if (result.audioFiles.size == 1) {
            player.play(
                file = result.audioFiles.first(),
                utteranceId = "presynth-${result.cacheKey}",
                onDone = onAllDone,
                onError = onError,
                deleteAfterComplete = false,
            )
        } else {
            player.playPlaylist(
                files = result.audioFiles,
                utteranceId = "presynth-${result.cacheKey}",
                onDone = onAllDone,
                onError = onError,
                onChunkAdvanced = onChunkChanged,
                deleteAfterComplete = false,
            )
        }
    }

    fun pause() = player.pause()

    fun resume() = player.resume()

    fun stop() = player.stop()

    fun setSpeechRate(rate: Float) {
        speechRate = rate
        player.setSpeechRate(rate)
    }

    fun isPaused(): Boolean = player.isPaused()

    fun release() = player.release()
}
