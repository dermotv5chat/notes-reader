package com.andriod.reader.service

import android.content.Context
import com.andriod.reader.data.local.AppDiagnosticLog
import com.andriod.reader.data.local.MarkdownPlainText
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.service.synthesis.TtsPreSynthProgress
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TtsPlaybackManager {
    private var controller: TtsController? = null
    private var appContext: Context? = null
    private val _session = MutableStateFlow(TtsPlaybackSession())
    val session: StateFlow<TtsPlaybackSession> = _session.asStateFlow()

    private var sleepTimer: TtsSleepTimer? = null
    private var sleepTimerPausedWithPlayback = false

    private val noopSegment: (Int, Int) -> Unit = { _, _ -> }
    private val noopPlayback: (Boolean) -> Unit = {}
    private val noopError: (String) -> Unit = {}

    private var attachedSegmentCallback: (Int, Int) -> Unit = noopSegment
    private var attachedPlaybackCallback: (Boolean) -> Unit = noopPlayback
    private var attachedErrorCallback: (String) -> Unit = noopError

    private fun diagnosticLog(context: Context): AppDiagnosticLog {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            TtsServiceEntryPoint::class.java,
        ).appDiagnosticLog()
    }

    private fun settingsStore(context: Context): SettingsStore {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            TtsServiceEntryPoint::class.java,
        ).settingsStore()
    }

    private fun ensureTimer(context: Context): TtsSleepTimer {
        appContext = context.applicationContext
        return sleepTimer ?: TtsSleepTimer(
            context = context.applicationContext,
            onSessionChanged = { syncSession() },
            onExpired = { stopPlayback(clearTimer = false) },
        ).also { sleepTimer = it }
    }

    private fun syncSession() {
        val base = controller?.playbackSnapshot() ?: TtsPlaybackSession()
        updateSleepTimerForPlayback(base)
        val timer = sleepTimer?.snapshot() ?: TtsSleepTimerState()
        _session.value = base.copy(
            sleepTimerMode = timer.mode,
            sleepTimerRemainingMs = timer.remainingMs,
            sleepTimerLabel = timer.label,
        )
    }

    private fun updateSleepTimerForPlayback(session: TtsPlaybackSession) {
        val timer = sleepTimer ?: return
        when {
            session.hasActiveSession && session.isPaused && !sleepTimerPausedWithPlayback -> {
                timer.onPlaybackPaused()
                sleepTimerPausedWithPlayback = true
            }
            session.hasActiveSession && session.isPlaying && sleepTimerPausedWithPlayback -> {
                timer.onPlaybackResumed()
                sleepTimerPausedWithPlayback = false
            }
            !session.hasActiveSession -> {
                sleepTimerPausedWithPlayback = false
            }
        }
    }

    fun getOrCreate(context: Context): TtsController {
        appContext = context.applicationContext
        ensureTimer(context)
        val existing = controller
        if (existing != null) return existing
        return TtsController(
            context = context.applicationContext,
            settingsStore = settingsStore(context),
            diagnosticLog = diagnosticLog(context),
            onSegmentChanged = attachedSegmentCallback,
            onPlaybackStateChanged = attachedPlaybackCallback,
            onSpeakError = attachedErrorCallback,
            onSessionChanged = { syncSession() },
            sleepTimerStateProvider = { sleepTimer?.snapshot() ?: TtsSleepTimerState() },
        ).also {
            controller = it
            syncSession()
        }
    }

    fun attachUiCallbacks(
        onSegmentChanged: (Int, Int) -> Unit,
        onPlaybackStateChanged: (Boolean) -> Unit,
        onSpeakError: (String) -> Unit = {},
    ) {
        attachedSegmentCallback = onSegmentChanged
        attachedPlaybackCallback = onPlaybackStateChanged
        attachedErrorCallback = onSpeakError
        controller?.updateCallbacks(onSegmentChanged, onPlaybackStateChanged, onSpeakError)
    }

    fun detachUiCallbacks() {
        attachedSegmentCallback = noopSegment
        attachedPlaybackCallback = noopPlayback
        attachedErrorCallback = noopError
        controller?.updateCallbacks(noopSegment, noopPlayback, noopError)
    }

    fun getOrNull(): TtsController? = controller

    fun presynthProgress(): StateFlow<TtsPreSynthProgress>? = controller?.presynthProgress()

    fun refreshPresynthForNote(content: String) {
        controller?.refreshPresynthForText(content)
    }

    fun canPreparePresynth(): Boolean = controller?.canPreparePresynth() == true

    fun isPresynthReady(): Boolean = controller?.isPresynthReady() == true

    fun preparePresynth(
        content: String,
        forceRegenerate: Boolean = false,
        autoPlayWhenReady: Boolean = false,
    ) {
        controller?.preparePresynth(content, forceRegenerate, autoPlayWhenReady)
    }

    fun cancelPresynth() {
        controller?.cancelPresynth()
    }

    fun sherpaModelInstalled(): Boolean = controller?.sherpaModelInstalled() == true

    suspend fun awaitReady(context: Context): TtsController {
        val ctrl = getOrCreate(context)
        if (!ctrl.isReady()) {
            ctrl.awaitReady(context)
        }
        syncSession()
        return ctrl
    }

    fun startPlayback(
        context: Context,
        fileName: String,
        title: String,
        content: String,
        withForegroundService: Boolean = true,
    ) {
        val previousFile = controller?.playbackSnapshot()?.fileName
        if (previousFile != null && previousFile != fileName) {
            sleepTimer?.cancel()
        }
        diagnosticLog(context).i(
            "TtsPlayback",
            "startPlayback file=$fileName title=$title chars=${content.length}",
        )
        controller?.start(fileName, title, content)
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                TtsServiceEntryPoint::class.java,
            ).ttsPlaylistManager().onPlaybackStarted(fileName)
        }
        syncSession()
        if (withForegroundService) {
            TtsPlaybackService.ensureStarted(context)
        }
    }

    fun togglePlayPause() {
        val snap = controller?.playbackSnapshot() ?: return
        if (snap.isPlaying) {
            pausePlayback()
        } else if (snap.hasActiveSession) {
            resumePlayback()
        }
    }

    fun pausePlayback() {
        val ctrl = controller ?: return
        if (!ctrl.playbackSnapshot().isPlaying) return
        ctrl.pause()
        syncSession()
    }

    fun resumePlayback() {
        val ctrl = controller ?: return
        val snap = ctrl.playbackSnapshot()
        if (!snap.hasActiveSession || snap.isPlaying) return
        ctrl.resume()
        syncSession()
    }

    fun stopPlayback(clearTimer: Boolean = true) {
        appContext?.let { diagnosticLog(it).i("TtsPlayback", "stopPlayback") }
        if (clearTimer) {
            sleepTimer?.cancel()
        }
        controller?.stop()
        syncSession()
    }

    fun onSleepTimerAlarm() {
        sleepTimer?.onAlarmFired()
    }

    fun setSleepTimerMinutes(context: Context, minutes: Int) {
        ensureTimer(context).setMinutes(minutes) {}
        syncSession()
    }

    fun setSleepTimerAfterNoteEnd(context: Context) {
        ensureTimer(context).setAfterNoteEnd()
        settingsStore(context).saveLastSleepTimerPreset(LastSleepTimerPreset.AfterNoteEnd)
        syncSession()
    }

    fun clearSleepTimer(context: Context) {
        ensureTimer(context).cancel()
        syncSession()
    }

    fun getLastSleepTimerPreset(context: Context): LastSleepTimerPreset =
        settingsStore(context).getLastSleepTimerPreset()

    fun applyLastSleepTimerPreset(context: Context): LastSleepTimerPreset {
        val preset = getLastSleepTimerPreset(context)
        when (preset) {
            is LastSleepTimerPreset.FixedMinutes -> setSleepTimerMinutes(context, preset.minutes)
            LastSleepTimerPreset.AfterNoteEnd -> setSleepTimerAfterNoteEnd(context)
        }
        return preset
    }

    fun initialSleepTimerSliderMinutes(context: Context): Int {
        val active = sleepTimerSliderMinutes()
        if (active > 0) return active
        return when (val preset = getLastSleepTimerPreset(context)) {
            is LastSleepTimerPreset.FixedMinutes -> preset.minutes
            LastSleepTimerPreset.AfterNoteEnd -> 0
        }
    }

    fun noteHasReadableContent(noteContent: String?): Boolean {
        if (noteContent.isNullOrBlank()) return false
        return MarkdownPlainText.stripForSpeech(noteContent).isNotBlank()
    }

    fun estimateNoteRemainingMinutes(noteContent: String?, speechRate: Float): Int {
        val ctrl = controller
        if (ctrl != null && ctrl.allSegmentTexts().isNotEmpty()) {
            return TtsRemainingEstimate.estimateNoteRemainingMinutes(
                segments = ctrl.allSegmentTexts(),
                fromIndex = ctrl.currentSegmentIndex(),
                speechRate = ctrl.currentSpeechRate(),
            )
        }
        val plain = noteContent?.let { MarkdownPlainText.stripForSpeech(it) } ?: return 0
        if (plain.isBlank()) return 0
        val segments = TtsSegmentSplitter.split(plain)
        return TtsRemainingEstimate.estimateNoteRemainingMinutes(
            segments = segments,
            fromIndex = 0,
            speechRate = speechRate,
        )
    }

    fun sleepTimerSliderMinutes(): Int {
        val timer = sleepTimer?.snapshot() ?: return 0
        if (timer.mode == SleepTimerMode.FixedMinutes && timer.remainingMs != null) {
            return ((timer.remainingMs + 59_999) / 60_000).toInt().coerceIn(0, 90)
        }
        return 0
    }

    fun release() {
        sleepTimer?.cancel()
        controller?.shutdown()
        controller = null
        sleepTimer = null
        sleepTimerPausedWithPlayback = false
        _session.value = TtsPlaybackSession()
    }

    fun reinitialize(context: Context) {
        diagnosticLog(context).i("TtsPlayback", "reinitialize")
        release()
        getOrCreate(context)
    }

    suspend fun ensureReady(context: Context): TtsController {
        val ctrl = getOrNull() ?: getOrCreate(context)
        if (!ctrl.isReady()) {
            ctrl.awaitReady(context)
        }
        syncSession()
        return ctrl
    }
}
