package com.andriod.reader.ui.reader

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.andriod.reader.domain.MuyuSoundPreset
import com.andriod.reader.service.TtsServiceEntryPoint
import dagger.hilt.android.EntryPointAccessors

object MuyuKnockFeedback {
    @Volatile
    private var soundPool: SoundPool? = null
    @Volatile
    private var knockSoundId: Int = 0
    @Volatile
    private var loadedPreset: MuyuSoundPreset? = null

    fun preload(context: Context) {
        val appContext = context.applicationContext
        val preset = readPreset(appContext)
        ensureSoundLoaded(appContext, preset)
    }

    fun play(context: Context) {
        val appContext = context.applicationContext
        val (soundEnabled, vibrationEnabled, preset) = runCatching {
            val settingsStore = EntryPointAccessors.fromApplication(
                appContext,
                TtsServiceEntryPoint::class.java,
            ).settingsStore()
            Triple(
                settingsStore.isMuyuSoundEnabled(),
                settingsStore.isMuyuVibrationEnabled(),
                settingsStore.getMuyuSoundPreset(),
            )
        }.getOrDefault(Triple(true, true, MuyuSoundPreset.DEFAULT))
        if (soundEnabled) {
            playSound(appContext, preset)
        }
        if (vibrationEnabled) {
            vibrate(appContext)
        }
    }

    fun previewSound(context: Context) {
        val appContext = context.applicationContext
        val preset = readPreset(appContext)
        playSound(appContext, preset)
    }

    private fun readPreset(context: Context): MuyuSoundPreset = runCatching {
        EntryPointAccessors.fromApplication(
            context,
            TtsServiceEntryPoint::class.java,
        ).settingsStore().getMuyuSoundPreset()
    }.getOrDefault(MuyuSoundPreset.DEFAULT)

    private fun ensureSoundPool(context: Context): SoundPool {
        soundPool?.let { return it }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        return SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attributes)
            .build()
            .also { pool -> soundPool = pool }
    }

    private fun ensureSoundLoaded(context: Context, preset: MuyuSoundPreset) {
        if (loadedPreset == preset && knockSoundId != 0) return
        val pool = ensureSoundPool(context)
        if (knockSoundId != 0) {
            pool.unload(knockSoundId)
            knockSoundId = 0
        }
        knockSoundId = pool.load(context, preset.rawResId, 1)
        loadedPreset = preset
    }

    private fun playSound(context: Context, preset: MuyuSoundPreset) {
        ensureSoundLoaded(context, preset)
        val pool = soundPool ?: return
        if (knockSoundId != 0) {
            pool.play(knockSoundId, 1f, 1f, 1, 0, 1f)
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java) ?: return
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE),
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(45)
        }
    }
}
