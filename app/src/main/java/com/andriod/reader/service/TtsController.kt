package com.andriod.reader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.session.MediaSessionCompat
import com.andriod.reader.MainActivity
import com.andriod.reader.R
import com.andriod.reader.data.local.MarkdownPlainText
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.domain.TtsSpeechBackend
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.domain.TtsVoicePreference
import com.andriod.reader.service.edge.EdgeTtsVoices
import com.andriod.reader.service.edge.NetworkAvailability
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CompletableDeferred
import java.util.concurrent.atomic.AtomicBoolean

class TtsController(
    context: Context,
    private val settingsStore: SettingsStore,
    private var onSegmentChanged: (Int, Int) -> Unit,
    private var onPlaybackStateChanged: (Boolean) -> Unit,
    private var onSpeakError: (String) -> Unit,
    private val onSessionChanged: () -> Unit = {},
    private val sleepTimerStateProvider: () -> TtsSleepTimerState = { TtsSleepTimerState() },
) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var ready = false
    private var selectedVoice: Voice? = null
    private var initDeferred = CompletableDeferred<Boolean>()
    private val segments = mutableListOf<String>()
    private var currentIndex = 0
    private var speechRate = 1.0f
    private var pitch = 1.0f
    private val isPlaying = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)
    private var loopEnabled = false
    private var engineTryOrder: List<String?> = emptyList()
    private var engineTryIndex = 0
    private var activeEnginePackage: String? = null
    private val attemptedEngines = mutableListOf<String>()
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var pausedByAudioFocus = false
    private val wakeLock = TtsWakeLock(appContext)

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                pausedByAudioFocus = false
                if (isPlaying.get() && !isPaused.get()) {
                    pause()
                    abandonAudioFocus()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            -> {
                if (isPlaying.get() && !isPaused.get()) {
                    pausedByAudioFocus = true
                    pauseForAudioFocusLoss()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isPlaying.get() && isPaused.get() && pausedByAudioFocus) {
                    pausedByAudioFocus = false
                    resume()
                }
            }
        }
    }

    private var awaitingInitAttemptId = 0
    private var currentFileName: String? = null
    private var currentTitle: String? = null
    private var onlineBackend: OnlineEdgeSpeechBackend? = null
    private var onlineActive = false

    private fun ensureOnlineBackend(): OnlineEdgeSpeechBackend {
        if (onlineBackend == null) {
            onlineBackend = OnlineEdgeSpeechBackend(appContext, settingsStore)
        }
        return onlineBackend!!
    }

    private fun wantsOnlineBackend(): Boolean =
        settingsStore.getTtsSpeechBackend() == TtsSpeechBackend.ONLINE_EDGE

    private fun refreshOnlineAvailability(): Boolean {
        if (!wantsOnlineBackend()) {
            onlineActive = false
            return false
        }
        onlineActive = NetworkAvailability.isConnected(appContext)
        return onlineActive
    }

    private fun shouldUseOnline(): Boolean = wantsOnlineBackend() && refreshOnlineAvailability()

    fun init() {
        // Actual engine startup happens in awaitReady() on the main thread.
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) {
            completeInit(false)
            return
        }
        val engine = tts ?: run {
            completeInit(false)
            return
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                engine.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
            }
            engine.setSpeechRate(speechRate)
            engine.setPitch(pitch)
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    handleSegmentFinished()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    onSpeakError("朗读出错，请检查系统 TTS 设置")
                    stop()
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    onSpeakError("朗读出错（$errorCode），请检查系统 TTS 设置")
                    stop()
                }
            })
            ready = true
            completeInit(true)
            runCatching {
                val setup = TtsHelper.setupChineseVoice(
                    engine = engine,
                    preferredVoiceName = settingsStore.getSelectedVoiceId(),
                    preference = readVoicePreference(),
                )
                selectedVoice = setup.voice
            }
        } catch (_: Exception) {
            completeInit(false)
        }
    }

    fun attemptedEngineLabels(): List<String> = attemptedEngines.toList()

    fun isReady(): Boolean = ready || (wantsOnlineBackend() && onlineActive)

    fun playbackSnapshot(): TtsPlaybackSession = TtsPlaybackSession(
        fileName = currentFileName,
        title = currentTitle,
        segmentIndex = currentIndex,
        segmentTotal = segments.size,
        isPlaying = isPlaying.get() && !isPaused.get(),
        isPaused = isPlaying.get() && isPaused.get(),
    )

    fun updateCallbacks(
        onSegmentChanged: (Int, Int) -> Unit,
        onPlaybackStateChanged: (Boolean) -> Unit,
        onSpeakError: (String) -> Unit,
    ) {
        this.onSegmentChanged = onSegmentChanged
        this.onPlaybackStateChanged = onPlaybackStateChanged
        this.onSpeakError = onSpeakError
    }

    suspend fun awaitReady(hostContext: Context): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main.immediate) {
            shutdownEngineOnly()
            attemptedEngines.clear()
            engineTryOrder = TtsHelper.engineTryOrder(appContext)

            for ((index, enginePackage) in engineTryOrder.withIndex()) {
                engineTryIndex = index
                activeEnginePackage = enginePackage
                val label = TtsHelper.engineLabel(enginePackage)
                attemptedEngines += label
                awaitingInitAttemptId++
                val attemptId = awaitingInitAttemptId
                initDeferred = CompletableDeferred()
                tts?.shutdown()
                tts = null
                ready = false
                selectedVoice = null

                tts = TtsHelper.createTextToSpeech(appContext, { status ->
                    if (attemptId != awaitingInitAttemptId) return@createTextToSpeech
                    onInit(status)
                }, enginePackage)
                val timeoutMs = if (enginePackage == null) DEFAULT_ENGINE_TIMEOUT_MS else PER_ENGINE_TIMEOUT_MS
                val success = TtsHelper.awaitEngineReady(initDeferred, timeoutMs = timeoutMs) == true && ready
                if (success) {
                    refreshOnlineBackend()
                    return@withContext true
                }
                awaitingInitAttemptId++
                tts?.shutdown()
                tts = null
                ready = false
            }
            refreshOnlineBackend()
            wantsOnlineBackend() && onlineActive
        }
    }

    private suspend fun refreshOnlineBackend() {
        if (!wantsOnlineBackend()) {
            onlineActive = false
            return
        }
        onlineActive = ensureOnlineBackend().awaitReady()
        ensureOnlineBackend().setSpeechRate(speechRate)
        ensureOnlineBackend().setPitch(pitch)
    }

    fun speechBackend(): TtsSpeechBackend = settingsStore.getTtsSpeechBackend()

    fun applySpeechBackend(backend: TtsSpeechBackend) {
        settingsStore.saveTtsSpeechBackend(backend)
        if (backend == TtsSpeechBackend.ONLINE_EDGE) {
            val edgeVoice = settingsStore.getEdgeTtsVoiceId()
            if (!EdgeTtsVoices.isKnownVoice(edgeVoice)) {
                settingsStore.saveEdgeTtsVoiceId(SettingsStore.DEFAULT_EDGE_TTS_VOICE)
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate
        tts?.setSpeechRate(rate)
        onlineBackend?.setSpeechRate(rate)
    }

    fun setPitch(value: Float) {
        pitch = value
        tts?.setPitch(value)
        onlineBackend?.setPitch(value)
    }

    fun setLoopEnabled(enabled: Boolean) {
        loopEnabled = enabled
    }

    fun isLoopEnabled(): Boolean = loopEnabled

    fun allSegmentTexts(): List<String> = segments.toList()

    fun currentSegmentIndex(): Int = currentIndex

    fun currentSpeechRate(): Float = speechRate

    fun diagnostics(): TtsHelper.TtsDiagnostics {
        if (wantsOnlineBackend()) {
            return ensureOnlineBackend().diagnostics()
        }
        return TtsHelper.getDiagnostics(
            context = appContext,
            engine = tts,
            activeEnginePackage = activeEnginePackage ?: tts?.defaultEngine,
            preferredVoiceName = settingsStore.getSelectedVoiceId(),
            preference = readVoicePreference(),
        )
    }

    fun voiceQualityTier(): TtsVoiceQualityTier {
        if (wantsOnlineBackend() && onlineActive) {
            return TtsVoiceQualityTier.NEURAL_ONLINE
        }
        return TtsVoiceQuality.assess(diagnostics())
    }

    fun listVoiceOptions(): List<TtsVoiceOption> {
        if (wantsOnlineBackend()) {
            return ensureOnlineBackend().listVoiceOptions()
        }
        val engine = tts ?: return emptyList()
        return TtsHelper.toVoiceOptions(
            voices = TtsHelper.listChineseVoices(engine),
            preference = readVoicePreference(),
        )
    }

    fun applySelectedVoice(voiceId: String?) {
        if (wantsOnlineBackend()) {
            voiceId?.let { ensureOnlineBackend().applySelectedVoice(it) }
            settingsStore.saveSelectedVoiceId(voiceId)
            return
        }
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
        if (!isReady()) {
            onSpeakError("语音引擎尚未就绪")
            return
        }
        speakText("你好，这是笔记朗读的语音试听。", "preview")
    }

    fun reinitialize() {
        shutdownEngineOnly()
    }

    fun start(fileName: String, title: String, text: String) {
        if (!isReady()) {
            onSpeakError("语音引擎尚未就绪")
            return
        }
        val plain = MarkdownPlainText.stripForSpeech(text)
        if (plain.isBlank()) {
            onSpeakError("没有可朗读的正文")
            return
        }
        segments.clear()
        segments.addAll(splitSegments(plain))
        if (segments.isEmpty()) {
            onSpeakError("没有可朗读的正文")
            return
        }
        currentFileName = fileName
        currentTitle = title
        currentIndex = 0
        isPlaying.set(true)
        isPaused.set(false)
        pausedByAudioFocus = false
        onSegmentChanged(currentIndex, segments.size)
        onPlaybackStateChanged(true)
        notifySessionChanged()
        if (!ensureAudioFocus()) {
            onSpeakError("无法获取音频焦点，请关闭其他正在播放的应用后重试")
            stop()
            return
        }
        wakeLock.acquire()
        speakCurrent()
    }

    fun pause() {
        if (!isPlaying.get()) return
        pausedByAudioFocus = false
        if (shouldUseOnline()) {
            if (!isPaused.get()) {
                ensureOnlineBackend().pause()
                isPaused.set(true)
                wakeLock.release()
                onPlaybackStateChanged(false)
                notifySessionChanged()
            }
            return
        }
        pauseForAudioFocusLoss()
    }

    private fun pauseForAudioFocusLoss() {
        if (!isPlaying.get() || isPaused.get()) return
        tts?.stop()
        isPaused.set(true)
        wakeLock.release()
        onPlaybackStateChanged(false)
        notifySessionChanged()
    }

    fun resume() {
        if (!isPlaying.get() || !isPaused.get()) return
        if (!ensureAudioFocus()) {
            onSpeakError("无法获取音频焦点，请关闭其他正在播放的应用后重试")
            return
        }
        isPaused.set(false)
        onPlaybackStateChanged(true)
        notifySessionChanged()
        wakeLock.acquire()
        if (shouldUseOnline()) {
            ensureOnlineBackend().resume()
        } else {
            speakCurrent()
        }
    }

    fun stop() {
        onlineBackend?.stop()
        tts?.stop()
        abandonAudioFocus()
        wakeLock.release()
        pausedByAudioFocus = false
        isPlaying.set(false)
        isPaused.set(false)
        currentIndex = 0
        segments.clear()
        currentFileName = null
        currentTitle = null
        onSegmentChanged(0, 0)
        onPlaybackStateChanged(false)
        notifySessionChanged()
    }

    fun nextSegment() {
        if (currentIndex < segments.lastIndex) {
            onlineBackend?.stop()
            tts?.stop()
            currentIndex++
            onSegmentChanged(currentIndex, segments.size)
            notifySessionChanged()
            if (isPlaying.get() && !isPaused.get()) speakCurrent()
        }
    }

    private fun notifySessionChanged() {
        onSessionChanged()
    }

    fun shutdown() {
        stop()
        shutdownEngineOnly()
    }

    private fun shutdownEngineOnly() {
        onlineBackend?.shutdown()
        onlineBackend = null
        onlineActive = false
        tts?.shutdown()
        tts = null
        ready = false
        selectedVoice = null
        engineTryOrder = emptyList()
        engineTryIndex = 0
        activeEnginePackage = null
        if (!initDeferred.isCompleted) {
            initDeferred.complete(false)
        }
        initDeferred = CompletableDeferred()
    }

    private fun completeInit(success: Boolean) {
        ready = success
        if (!initDeferred.isCompleted) {
            initDeferred.complete(success)
        }
    }

    private fun speakCurrent() {
        if (!isReady()) {
            onSpeakError("语音引擎尚未就绪")
            stop()
            return
        }
        if (segments.isEmpty()) {
            onSpeakError("没有可朗读的正文")
            return
        }
        val segment = segments[currentIndex]
        if (segment.isBlank()) {
            if (currentIndex < segments.lastIndex) {
                currentIndex++
                onSegmentChanged(currentIndex, segments.size)
                speakCurrent()
            } else {
                stop()
            }
            return
        }
        speakText(segment, "seg-$currentIndex")
    }

    private fun handleSegmentFinished() {
        if (!isPlaying.get() || isPaused.get()) return
        if (currentIndex < segments.lastIndex) {
            currentIndex++
            onSegmentChanged(currentIndex, segments.size)
            notifySessionChanged()
            speakCurrent()
        } else if (TtsPlaybackEndAction.shouldContinueAfterLastSegment(
                loopEnabled,
                sleepTimerStateProvider(),
            )
        ) {
            currentIndex = 0
            onSegmentChanged(currentIndex, segments.size)
            notifySessionChanged()
            speakCurrent()
        } else {
            stop()
        }
    }

    private fun speakText(text: String, utteranceId: String) {
        if (!ensureAudioFocus()) {
            onSpeakError("无法获取音频焦点，请关闭其他正在播放的应用后重试")
            stop()
            return
        }
        if (wantsOnlineBackend()) {
            if (shouldUseOnline()) {
                ensureOnlineBackend().speak(
                    text = text,
                    utteranceId = utteranceId,
                    onDone = { handleSegmentFinished() },
                    onError = { message ->
                        if (ready && tts != null) {
                            onSpeakError("在线合成失败，已改用系统语音：$message")
                            speakWithSystem(text, utteranceId)
                        } else {
                            onSpeakError(message)
                            stop()
                        }
                    },
                )
                return
            }
            if (ready && tts != null) {
                onSpeakError("当前无网络，已改用系统语音朗读")
                speakWithSystem(text, utteranceId)
                return
            }
            onSpeakError("无网络且系统语音未就绪，无法朗读")
            stop()
            return
        }
        speakWithSystem(text, utteranceId)
    }

    private fun speakWithSystem(text: String, utteranceId: String) {
        val engine = tts
        if (engine == null || !ready) {
            onSpeakError("语音引擎尚未就绪")
            stop()
            return
        }
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (result == TextToSpeech.ERROR) {
            onSpeakError("朗读失败，请到系统设置检查文字转语音引擎")
            stop()
        }
    }

    private fun ensureAudioFocus(): Boolean {
        if (hasAudioFocus) return true
        hasAudioFocus = requestAudioFocus()
        return hasAudioFocus
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN,
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        audioFocusRequest = null
        hasAudioFocus = false
    }

    private fun splitSegments(text: String): List<String> = TtsSegmentSplitter.split(text)

    companion object {
        private const val PER_ENGINE_TIMEOUT_MS = 8_000L
        private const val DEFAULT_ENGINE_TIMEOUT_MS = 15_000L
    }
}

object TtsNotificationHelper {
    const val CHANNEL_ID = "tts_playback"
    const val NOTIFICATION_ID = 1001
    private const val ALBUM_ART_SIZE_PX = 256

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "语音朗读",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun albumArtBitmap(context: Context): Bitmap {
        val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            ?: ContextCompat.getDrawable(context, R.drawable.ic_notification)
            ?: return Bitmap.createBitmap(ALBUM_ART_SIZE_PX, ALBUM_ART_SIZE_PX, Bitmap.Config.ARGB_8888)
        return drawable.toBitmap(ALBUM_ART_SIZE_PX, ALBUM_ART_SIZE_PX, Bitmap.Config.ARGB_8888)
    }

    fun buildNotification(
        context: Context,
        session: TtsPlaybackSession,
        sessionToken: MediaSessionCompat.Token,
    ): Notification {
        val title = session.title ?: "语音朗读"
        val contentText = subtitleFor(session)
        val albumArt = albumArtBitmap(context)
        val openReaderIntent = Intent(context, MainActivity::class.java).apply {
            session.fileName?.let { putExtra(TtsPlaybackService.EXTRA_FILE_NAME, it) }
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            openReaderIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val playPauseAction = if (session.isPlaying) {
            NotificationCompat.Action(
                android.R.drawable.ic_media_pause,
                "暂停",
                TtsPlaybackService.actionPendingIntent(context, TtsPlaybackService.ACTION_PLAY_PAUSE),
            )
        } else {
            NotificationCompat.Action(
                android.R.drawable.ic_media_play,
                "播放",
                TtsPlaybackService.actionPendingIntent(context, TtsPlaybackService.ACTION_PLAY_PAUSE),
            )
        }
        val stopAction = NotificationCompat.Action(
            android.R.drawable.ic_menu_close_clear_cancel,
            "停止",
            TtsPlaybackService.actionPendingIntent(context, TtsPlaybackService.ACTION_STOP),
        )
        val mediaStyle = MediaStyle()
            .setMediaSession(sessionToken)
            .setShowActionsInCompactView(0, 1)
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSubText("笔记朗读")
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(albumArt)
            .setContentIntent(contentIntent)
            .addAction(playPauseAction)
            .addAction(stopAction)
            .setStyle(mediaStyle)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(session.hasActiveSession)
            .build()
    }

    private fun subtitleFor(session: TtsPlaybackSession): String {
        val base = when {
            session.isPlaying && session.segmentTotal > 0 ->
                "段落 ${session.segmentIndex + 1} / ${session.segmentTotal}"
            session.isPaused -> "已暂停"
            session.isPlaying -> "正在朗读"
            else -> "已停止"
        }
        val timerLabel = session.sleepTimerLabel?.takeIf { session.sleepTimerActive }
        return if (timerLabel != null) "$base · $timerLabel" else base
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TtsServiceEntryPoint {
    fun settingsStore(): SettingsStore
}
