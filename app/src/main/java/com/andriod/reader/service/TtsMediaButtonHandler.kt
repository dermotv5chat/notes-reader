package com.andriod.reader.service

import android.content.Intent
import android.os.Build
import android.view.KeyEvent

/**
 * Parses Bluetooth / steering-wheel [Intent.ACTION_MEDIA_BUTTON] events.
 * [Intent.EXTRA_KEY_EVENT] is a [KeyEvent] parcelable, not a raw key code int.
 */
object TtsMediaButtonHandler {

    fun keyCodeFrom(mediaButtonIntent: Intent?): Int? {
        val event = keyEventFrom(mediaButtonIntent) ?: return null
        if (event.action != KeyEvent.ACTION_DOWN) return null
        return event.keyCode
    }

    fun keyEventFrom(mediaButtonIntent: Intent?): KeyEvent? {
        if (mediaButtonIntent == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
        } else {
            @Suppress("DEPRECATION")
            mediaButtonIntent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
        }
    }

    fun handleKeyCode(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_MEDIA_PLAY -> {
            TtsPlaybackManager.resumePlayback()
            true
        }
        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
            TtsPlaybackManager.pausePlayback()
            true
        }
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
            TtsPlaybackManager.togglePlayPause()
            true
        }
        KeyEvent.KEYCODE_MEDIA_STOP -> {
            TtsPlaybackManager.stopPlayback()
            true
        }
        else -> false
    }

    fun handleMediaButtonIntent(mediaButtonIntent: Intent?): Boolean {
        val keyCode = keyCodeFrom(mediaButtonIntent) ?: return false
        return handleKeyCode(keyCode)
    }
}
