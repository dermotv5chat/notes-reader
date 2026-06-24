package com.andriod.reader.service.edge

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File

/**
 * Plays synthesized MP3 files for one utterance at a time (Phase 1 online backend).
 */
class ExoPlayerSpeechPlayer(context: Context) {
    private val appContext = context.applicationContext
    private var player: ExoPlayer? = null
    private var onDone: (() -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    private var currentFile: File? = null
    private var paused = false
    private var pendingFile: File? = null
    private var pendingUtteranceId: String? = null

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                val done = onDone
                cleanupCurrentFile()
                done?.invoke()
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            onError?.invoke("播放失败：${error.message ?: "未知错误"}")
        }
    }

    fun ensurePlayer(): ExoPlayer {
        val existing = player
        if (existing != null) return existing
        return ExoPlayer.Builder(appContext).build().also { exo ->
            exo.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                    .build(),
                true,
            )
            exo.addListener(listener)
            player = exo
        }
    }

    fun play(file: File, utteranceId: String, onDone: () -> Unit, onError: (String) -> Unit) {
        this.onDone = onDone
        this.onError = onError
        paused = false
        pendingFile = null
        pendingUtteranceId = null
        cleanupCurrentFile()
        currentFile = file
        val exo = ensurePlayer()
        exo.stop()
        exo.clearMediaItems()
        exo.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        exo.prepare()
        exo.playWhenReady = true
        exo.play()
    }

    fun pause() {
        paused = true
        player?.pause()
    }

    fun resume() {
        paused = false
        player?.play()
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
