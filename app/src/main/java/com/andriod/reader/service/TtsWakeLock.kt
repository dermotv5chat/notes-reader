package com.andriod.reader.service

import android.content.Context
import android.os.PowerManager

/**
 * Keeps the CPU awake while TTS is actively playing so playback continues with the screen off.
 */
internal class TtsWakeLock(context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquire() {
        if (wakeLock?.isHeld == true) return
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
            setReferenceCounted(false)
            acquire(MAX_WAKE_LOCK_MS)
        }
    }

    fun release() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    companion object {
        private const val WAKE_LOCK_TAG = "andriod.reader:TtsPlayback"
        private const val MAX_WAKE_LOCK_MS = 10 * 60 * 60 * 1000L
    }
}
