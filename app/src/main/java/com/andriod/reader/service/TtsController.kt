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
import com.andriod.reader.data.local.MarkdownPlainText
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.domain.TtsVoicePreference
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicBoolean

class TtsController(
    private val context: android.content.Context,
    private val settingsStore: SettingsStore,
    private val onSegmentChanged: (Int, Int) -> Unit,
    private val onPlaybackStateChanged: (Boolean) -> Unit,
) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var selectedVoice: Voice? = null
    private val initDeferred = CompletableDeferred<Boolean>()
    private val segments = mutableListOf<String>()
    private var currentIndex = 0
    private var speechRate = 1.0f
    private var pitch = 1.0f
    private val isPlaying = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private val usesGoogleEngine = TtsHelper.isGoogleTtsInstalled(context)

    fun init() {
        if (tts == null) {
            if (!initDeferred.isCompleted) {
                // noop - will complete in onInit
            }
            tts = TtsHelper.createTextToSpeech(context, this)
        }
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            if (!initDeferred.isCompleted) initDeferred.complete(false)
            return
        }
        val engine = tts ?: run {
            if (!initDeferred.isCompleted) initDeferred.complete(false)
            return
        }
        val setup = TtsHelper.setupChineseVoice(
            engine = engine,
            preferredVoiceName = settingsStore.getSelectedVoiceId(),
            preference = readVoicePreference(),
        )
        selectedVoice = setup.voice
        engine.setSpeechRate(speechRate)
        engine.setPitch(pitch)
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
        if (!initDeferred.isCompleted) initDeferred.complete(true)
    }

    suspend fun awaitReady(): Boolean {
        init()
        return TtsHelper.awaitEngineReady(initDeferred)
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
    }

    fun setPitch(value: Float) {
        pitch = value
        tts?.setPitch(value)
    }

    fun diagnostics(): TtsHelper.TtsDiagnostics =
        TtsHelper.getDiagnostics(
            context = context,
            engine = tts,
            forcedGoogle = usesGoogleEngine,
            preferredVoiceName = settingsStore.getSelectedVoiceId(),
            preference = readVoicePreference(),
        )

    fun listVoiceOptions(): List<TtsVoiceOption> {
        val engine = tts ?: return emptyList()
        return TtsHelper.toVoiceOptions(
            voices = TtsHelper.listChineseVoices(engine),
            preference = readVoicePreference(),
        )
    }

    fun applySelectedVoice(voiceId: String?) {
        settingsStore.saveSelectedVoiceId(voiceId)
        tts?.let { engine ->
            val setup = TtsHelper.setupChineseVoice(
                engine = engine,
                preferredVoiceName = voiceId,
                preference = readVoicePreference(),
            )
            selectedVoice = setup.voice
        }
    }

    fun applyVoicePreference(preference: TtsVoicePreference) {
        settingsStore.saveVoicePreference(preference.name)
        if (settingsStore.getSelectedVoiceId() == null) {
            tts?.let { engine ->
                val setup = TtsHelper.setupChineseVoice(
                    engine = engine,
                    preferredVoiceName = null,
                    preference = preference,
                )
                selectedVoice = setup.voice
            }
        }
    }

    private fun readVoicePreference(): TtsVoicePreference {
        return runCatching {
            TtsVoicePreference.valueOf(settingsStore.getVoicePreference())
        }.getOrDefault(TtsVoicePreference.AUTO)
    }

    fun previewSample() {
        init()
        if (!ready) return
        tts?.speak(
            "你好，这是笔记朗读的语音试听。",
            TextToSpeech.QUEUE_FLUSH,
            null,
            "preview",
        )
    }

    fun reinitialize() {
        shutdown()
        init()
    }

    fun start(text: String) {
        init()
        segments.clear()
        segments.addAll(splitSegments(MarkdownPlainText.stripForSpeech(text)))
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
        selectedVoice = null
        if (!initDeferred.isCompleted) {
            initDeferred.complete(false)
        }
    }

    fun isCurrentlyPlaying(): Boolean = isPlaying.get() && !isPaused.get()

    private fun speakCurrent() {
        if (!ready || segments.isEmpty()) return
        val segment = segments[currentIndex]
        tts?.speak(segment, TextToSpeech.QUEUE_FLUSH, null, "seg-$currentIndex")
    }

    private fun splitSegments(text: String): List<String> {
        return text.split(Regex("(?<=[。！？；;.!?\\n])|(?<=[，,])"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(text) }
    }
}

object TtsPlaybackManager {
    private var controller: TtsController? = null

    private fun settingsStore(context: android.content.Context): SettingsStore {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            TtsServiceEntryPoint::class.java,
        ).settingsStore()
    }

    fun getOrCreate(
        context: android.content.Context,
        onSegmentChanged: (Int, Int) -> Unit,
        onPlaybackStateChanged: (Boolean) -> Unit,
    ): TtsController {
        return controller ?: TtsController(
            context = context,
            settingsStore = settingsStore(context),
            onSegmentChanged = onSegmentChanged,
            onPlaybackStateChanged = onPlaybackStateChanged,
        ).also {
            controller = it
            it.init()
        }
    }

    fun getOrNull(): TtsController? = controller

    suspend fun awaitReady(context: android.content.Context): TtsController {
        val ctrl = getOrCreate(context, { _, _ -> }, {})
        ctrl.awaitReady()
        return ctrl
    }

    fun release() {
        controller?.shutdown()
        controller = null
    }

    fun reinitialize(context: android.content.Context) {
        release()
        getOrCreate(context, { _, _ -> }, {})
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

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TtsServiceEntryPoint {
    fun settingsStore(): SettingsStore
}
