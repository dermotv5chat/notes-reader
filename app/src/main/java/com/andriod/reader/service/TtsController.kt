package com.andriod.reader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import com.andriod.reader.MainActivity
import com.andriod.reader.R
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class TtsController(
    private val context: android.content.Context,
    private val onSegmentChanged: (Int, Int) -> Unit,
    private val onPlaybackStateChanged: (Boolean) -> Unit,
) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ready = false
    private val segments = mutableListOf<String>()
    private var currentIndex = 0
    private var speechRate = 1.0f
    private val isPlaying = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    fun init() {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext, this)
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val engine = tts ?: return
        val voice = selectChineseVoice(engine)
        if (voice != null) {
            engine.voice = voice
        } else {
            engine.language = Locale.SIMPLIFIED_CHINESE
        }
        engine.setSpeechRate(speechRate)
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit

            override fun onDone(utteranceId: String?) {
                if (!isPlaying.get() || isPaused.get()) return
                if (currentIndex < segments.lastIndex) {
                    currentIndex++
                    onSegmentChanged(currentIndex, segments.size)
                    speakCurrent()
                } else {
                    stop()
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = Unit
        })
        ready = true
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
    }

    fun start(text: String) {
        init()
        segments.clear()
        segments.addAll(splitSegments(text))
        currentIndex = 0
        isPlaying.set(true)
        isPaused.set(false)
        onSegmentChanged(currentIndex, segments.size)
        onPlaybackStateChanged(true)
        speakCurrent()
    }

    fun pause() {
        if (!isPlaying.get()) return
        tts?.stop()
        isPaused.set(true)
        onPlaybackStateChanged(false)
    }

    fun resume() {
        if (!isPlaying.get() || !isPaused.get()) return
        isPaused.set(false)
        onPlaybackStateChanged(true)
        speakCurrent()
    }

    fun stop() {
        tts?.stop()
        isPlaying.set(false)
        isPaused.set(false)
        currentIndex = 0
        onPlaybackStateChanged(false)
    }

    fun nextSegment() {
        if (currentIndex < segments.lastIndex) {
            tts?.stop()
            currentIndex++
            onSegmentChanged(currentIndex, segments.size)
            if (isPlaying.get() && !isPaused.get()) speakCurrent()
        }
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        ready = false
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying.get() && !isPaused.get()

    private fun speakCurrent() {
        if (!ready || segments.isEmpty()) return
        val segment = segments[currentIndex]
        tts?.speak(segment, TextToSpeech.QUEUE_FLUSH, null, "seg-$currentIndex")
    }

    private fun splitSegments(text: String): List<String> {
        return text.split(Regex("(?<=[。！？.!?\\n])"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(text) }
    }

    private fun selectChineseVoice(engine: TextToSpeech): Voice? {
        return engine.voices
            ?.filter { it.locale.language == "zh" }
            ?.sortedByDescending { it.quality }
            ?.firstOrNull { it.quality >= Voice.QUALITY_HIGH }
            ?: engine.voices?.firstOrNull { it.locale.language == "zh" }
    }
}

object TtsPlaybackManager {
    private var controller: TtsController? = null

    fun getOrCreate(
        context: android.content.Context,
        onSegmentChanged: (Int, Int) -> Unit,
        onPlaybackStateChanged: (Boolean) -> Unit,
    ): TtsController {
        return controller ?: TtsController(context, onSegmentChanged, onPlaybackStateChanged).also {
            controller = it
            it.init()
        }
    }

    fun release() {
        controller?.shutdown()
        controller = null
    }
}

object TtsNotificationHelper {
    const val CHANNEL_ID = "tts_playback"
    const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: android.content.Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "语音朗读",
                NotificationManager.IMPORTANCE_LOW,
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(context: android.content.Context, title: String, isPlaying: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "暂停",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    android.view.KeyEvent.KEYCODE_MEDIA_PAUSE.toLong(),
                ),
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "播放",
                MediaButtonReceiver.buildMediaButtonPendingIntent(
                    context,
                    android.view.KeyEvent.KEYCODE_MEDIA_PLAY.toLong(),
                ),
            )
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (isPlaying) "正在朗读" else "已暂停")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentIntent)
            .addAction(playPauseAction)
            .setOngoing(isPlaying)
            .build()
    }
}
