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
 * Plays synthesized MP3 files for one utterance at a time (Phase 1 online backend).
 */
class ExoPlayerSpeechPlayer(
    context: Context,
    private val diagnosticLog: AppDiagnosticLog? = null,
) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private var onDone: (() -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var currentFile: File? = null
    private var paused = false

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_READY -> diagnosticLog?.d("ExoPlayer", "state=READY")
                Player.STATE_ENDED -> {
                    diagnosticLog?.i("ExoPlayer", "state=ENDED")
                    val done = onDone
                    onDone = null
                    onError = null
                    cleanupCurrentFile()
                    done?.invoke()
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            diagnosticLog?.e("ExoPlayer", "playback error: ${error.message}", error)
            val err = onError
            onDone = null
            onError = null
            cleanupCurrentFile()
            player?.stop()
            player?.clearMediaItems()
            err?.invoke("播放失败：${error.message ?: "未知错误"}")
        }
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

    fun play(file: File, utteranceId: String, onDone: () -> Unit, onError: (String) -> Unit) {
        if (!file.exists() || file.length() < 100L) {
            diagnosticLog?.e("ExoPlayer", "invalid file utterance=$utteranceId size=${file.length()}")
            onError("播放失败：音频文件无效")
            return
        }
        this.onDone = onDone
        this.onError = onError
        paused = false
        cleanupCurrentFile()
        currentFile = file
        diagnosticLog?.i("ExoPlayer", "play utterance=$utteranceId bytes=${file.length()}")
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
        cleanupCurrentFile()
        onDone = null
        onError = null
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

    private fun cleanupCurrentFile() {
        currentFile?.delete()
        currentFile = null
    }
}
