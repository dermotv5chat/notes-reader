package com.andriod.reader.service.edge

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.andriod.reader.data.local.AppDiagnosticLog
import java.io.File

/**
 * Plays synthesized audio files (single utterance or presynth playlist).
 */
class ExoPlayerSpeechPlayer(
    context: Context,
    private val diagnosticLog: AppDiagnosticLog? = null,
) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private var onDone: (() -> Unit)? = null
    private var onChunkAdvanced: ((Int, Int) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var deleteAfterComplete = true
    private var playlistFiles: List<File> = emptyList()
    private var playlistIndex = 0
    private var chunkDurationsMs: List<Long> = emptyList()
    private var paused = false

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> diagnosticLog?.d("ExoPlayer", "state=READY")
                Player.STATE_ENDED -> handleEnded()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            diagnosticLog?.e("ExoPlayer", "playback error: ${error.message}", error)
            val err = onError
            onDone = null
            onError = null
            onChunkAdvanced = null
            playlistFiles = emptyList()
            player?.stop()
            player?.clearMediaItems()
            err?.invoke("播放失败：${error.message ?: "未知错误"}")
        }
    }

    private fun handleEnded() {
        if (playlistFiles.size > 1 && playlistIndex < playlistFiles.lastIndex) {
            if (deleteAfterComplete) {
                playlistFiles.getOrNull(playlistIndex)?.delete()
            }
            playlistIndex++
            onChunkAdvanced?.invoke(playlistIndex, playlistFiles.size)
            playCurrentInPlaylist()
            return
        }
        if (deleteAfterComplete) {
            playlistFiles.getOrNull(playlistIndex)?.delete()
        }
        diagnosticLog?.i("ExoPlayer", "state=ENDED")
        val done = onDone
        onDone = null
        onError = null
        onChunkAdvanced = null
        playlistFiles = emptyList()
        playlistIndex = 0
        done?.invoke()
    }

    fun ensurePlayer(): ExoPlayer {
        val existing = player
        if (existing != null) return existing
        return ExoPlayer.Builder(appContext).build().also { exo ->
            exo.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                false,
            )
            exo.volume = 1f
            exo.addListener(listener)
            player = exo
        }
    }

    fun play(
        file: File,
        utteranceId: String,
        onDone: () -> Unit,
        onError: (String) -> Unit,
        deleteAfterComplete: Boolean = true,
    ) {
        playPlaylist(
            files = listOf(file),
            utteranceId = utteranceId,
            onDone = onDone,
            onError = onError,
            onChunkAdvanced = null,
            deleteAfterComplete = deleteAfterComplete,
        )
    }

    fun playPlaylist(
        files: List<File>,
        utteranceId: String,
        onDone: () -> Unit,
        onError: (String) -> Unit,
        onChunkAdvanced: ((Int, Int) -> Unit)? = null,
        deleteAfterComplete: Boolean = false,
    ) {
        val valid = files.filter { it.exists() && it.length() > 100L }
        if (valid.isEmpty()) {
            diagnosticLog?.e("ExoPlayer", "invalid playlist utterance=$utteranceId")
            onError("播放失败：音频文件无效")
            return
        }
        this.onDone = onDone
        this.onError = onError
        this.onChunkAdvanced = onChunkAdvanced
        this.deleteAfterComplete = deleteAfterComplete
        playlistFiles = valid
        playlistIndex = 0
        chunkDurationsMs = emptyList()
        paused = false
        diagnosticLog?.i("ExoPlayer", "play playlist utterance=$utteranceId files=${valid.size}")
        playCurrentInPlaylist()
    }

    private fun playCurrentInPlaylist() {
        val file = playlistFiles.getOrNull(playlistIndex) ?: return
        val exo = ensurePlayer()
        exo.stop()
        exo.clearMediaItems()
        exo.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        exo.prepare()
        exo.playWhenReady = true
    }

    fun pause() {
        paused = true
        player?.pause()
    }

    fun resume() {
        paused = false
        player?.playWhenReady = true
    }

    fun stop() {
        paused = false
        player?.stop()
        player?.clearMediaItems()
        if (deleteAfterComplete) {
            playlistFiles.getOrNull(playlistIndex)?.delete()
        }
        playlistFiles = emptyList()
        playlistIndex = 0
        chunkDurationsMs = emptyList()
        onDone = null
        onError = null
        onChunkAdvanced = null
    }

    fun setSpeechRate(rate: Float) {
        player?.setPlaybackSpeed(rate.coerceIn(0.5f, 2.0f))
    }

    fun release() {
        stop()
        player?.removeListener(listener)
        player?.release()
        player = null
    }

    fun isPaused(): Boolean = paused

    fun setChunkDurationsMs(durations: List<Long>) {
        chunkDurationsMs = durations
    }

    fun currentPositionMs(): Long = player?.currentPosition ?: 0L

    fun durationMs(): Long {
        val duration = player?.duration ?: 0L
        return if (duration > 0) duration else 0L
    }

    fun currentChunkIndex(): Int = playlistIndex

    fun overallPositionMs(): Long {
        val prior = chunkDurationsMs.take(playlistIndex).sum()
        return prior + currentPositionMs()
    }

    fun overallDurationMs(): Long {
        val cachedTotal = chunkDurationsMs.sum()
        if (cachedTotal > 0) return cachedTotal
        return if (playlistFiles.size <= 1) durationMs() else 0L
    }
}
