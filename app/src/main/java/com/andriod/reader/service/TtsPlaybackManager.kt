package com.andriod.reader.service

import android.content.Context
import com.andriod.reader.data.remote.SettingsStore
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object TtsPlaybackManager {
    private var controller: TtsController? = null
    private val _session = MutableStateFlow(TtsPlaybackSession())
    val session: StateFlow<TtsPlaybackSession> = _session.asStateFlow()

    private val noopSegment: (Int, Int) -> Unit = { _, _ -> }
    private val noopPlayback: (Boolean) -> Unit = {}
    private val noopError: (String) -> Unit = {}

    private fun settingsStore(context: Context): SettingsStore {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            TtsServiceEntryPoint::class.java,
        ).settingsStore()
    }

    private fun syncSession() {
        _session.value = controller?.playbackSnapshot() ?: TtsPlaybackSession()
    }

    fun getOrCreate(context: Context): TtsController {
        val existing = controller
        if (existing != null) return existing
        return TtsController(
            context = context,
            settingsStore = settingsStore(context),
            onSegmentChanged = noopSegment,
            onPlaybackStateChanged = noopPlayback,
            onSpeakError = noopError,
            onSessionChanged = { syncSession() },
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
        controller?.updateCallbacks(onSegmentChanged, onPlaybackStateChanged, onSpeakError)
    }

    fun detachUiCallbacks() {
        controller?.updateCallbacks(noopSegment, noopPlayback, noopError)
    }

    fun getOrNull(): TtsController? = controller

    suspend fun awaitReady(context: Context): TtsController {
        val ctrl = getOrCreate(context)
        if (!ctrl.isReady()) {
            ctrl.awaitReady(context)
        }
        syncSession()
        return ctrl
    }

    fun startPlayback(context: Context, fileName: String, title: String, content: String) {
        controller?.start(fileName, title, content)
        syncSession()
        TtsPlaybackService.ensureStarted(context)
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        val snap = ctrl.playbackSnapshot()
        if (snap.isPlaying) {
            ctrl.pause()
        } else if (snap.hasActiveSession) {
            ctrl.resume()
        }
        syncSession()
    }

    fun stopPlayback() {
        controller?.stop()
        syncSession()
    }

    fun release() {
        controller?.shutdown()
        controller = null
        _session.value = TtsPlaybackSession()
    }

    fun reinitialize(context: Context) {
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
