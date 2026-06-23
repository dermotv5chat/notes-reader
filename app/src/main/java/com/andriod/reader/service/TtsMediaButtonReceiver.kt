package com.andriod.reader.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Forwards AVRCP / steering-wheel media keys to [TtsPlaybackService] when the session
 * is registered via [MediaSessionCompat.setMediaButtonReceiver].
 */
class TtsMediaButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_MEDIA_BUTTON != intent.action) return
        if (!TtsPlaybackManager.session.value.hasActiveSession) return
        val serviceIntent = Intent(context, TtsPlaybackService::class.java).apply {
            action = Intent.ACTION_MEDIA_BUTTON
            val keyEvent = TtsMediaButtonHandler.keyEventFrom(intent)
            if (keyEvent != null) {
                putExtra(Intent.EXTRA_KEY_EVENT, keyEvent)
            }
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
