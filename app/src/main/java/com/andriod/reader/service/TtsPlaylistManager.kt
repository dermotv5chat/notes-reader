package com.andriod.reader.service

import android.content.Context
import com.andriod.reader.data.local.TtsPlaylistSnapshot
import com.andriod.reader.data.local.TtsPlaylistStore
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.data.repository.NoteRepository
import com.andriod.reader.domain.TtsPlaylistItem
import com.andriod.reader.domain.TtsQueueRepeatMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsPlaylistManager @Inject constructor(
    private val store: TtsPlaylistStore,
    private val noteRepository: NoteRepository,
    private val settingsStore: SettingsStore,
) {
    private val _state = MutableStateFlow(loadInitialSnapshot())
    val state: StateFlow<TtsPlaylistSnapshot> = _state.asStateFlow()

    private var playingIndex: Int? = null

    private fun loadInitialSnapshot(): TtsPlaylistSnapshot {
        val saved = store.read()
        if (saved.repeatMode == TtsQueueRepeatMode.OFF && settingsStore.isLoopPlaybackEnabled()) {
            val migrated = saved.copy(repeatMode = TtsQueueRepeatMode.REPEAT_ONE)
            store.write(migrated)
            return migrated
        }
        return saved
    }

    fun contains(fileName: String): Boolean =
        _state.value.items.any { it.fileName == fileName }

    fun add(fileName: String, title: String) {
        val current = _state.value
        if (current.items.any { it.fileName == fileName }) return
        persist(
            current.copy(
                items = current.items + TtsPlaylistItem(
                    fileName = fileName,
                    title = title,
                    addedAt = Instant.now(),
                ),
            ),
        )
    }

    fun remove(fileName: String) {
        val current = _state.value
        val items = current.items.filterNot { it.fileName == fileName }
        if (playingIndex != null && current.items.getOrNull(playingIndex!!)?.fileName == fileName) {
            playingIndex = null
        }
        persist(sanitizeRepeatMode(current.copy(items = items)))
    }

    fun clear() {
        playingIndex = null
        persist(TtsPlaylistSnapshot())
    }

    fun setRepeatMode(mode: TtsQueueRepeatMode) {
        val current = _state.value
        if (mode == TtsQueueRepeatMode.REPEAT_ALL && !TtsPlaylistPolicy.canSelectRepeatAll(current.items)) {
            return
        }
        persist(current.copy(repeatMode = mode))
    }

    fun cycleRepeatMode(): TtsQueueRepeatMode {
        val current = _state.value
        val next = when (current.repeatMode) {
            TtsQueueRepeatMode.OFF -> TtsQueueRepeatMode.REPEAT_ONE
            TtsQueueRepeatMode.REPEAT_ONE -> {
                if (TtsPlaylistPolicy.canSelectRepeatAll(current.items)) {
                    TtsQueueRepeatMode.REPEAT_ALL
                } else {
                    TtsQueueRepeatMode.OFF
                }
            }
            TtsQueueRepeatMode.REPEAT_ALL -> TtsQueueRepeatMode.OFF
        }
        persist(current.copy(repeatMode = next))
        return next
    }

    fun onPlaybackStarted(fileName: String) {
        val index = _state.value.items.indexOfFirst { it.fileName == fileName }
        playingIndex = if (index >= 0) index else null
        syncLoopToController()
    }

    fun handleNoteFinished(context: Context): Boolean {
        val snap = _state.value
        val currentFile = TtsPlaybackManager.getOrNull()?.playbackSnapshot()?.fileName ?: return false
        val sleepTimer = TtsPlaybackManager.session.value.let {
            TtsSleepTimerState(
                mode = it.sleepTimerMode,
                remainingMs = it.sleepTimerRemainingMs,
                label = it.sleepTimerLabel,
            )
        }
        return when (
            val action = TtsPlaylistPolicy.resolveAfterNoteFinished(
                repeatMode = snap.repeatMode,
                sleepTimer = sleepTimer,
                currentFileName = currentFile,
                items = snap.items,
                playingIndex = playingIndex,
            )
        ) {
            NoteFinishedAction.Stop -> false
            is NoteFinishedAction.PlayAtIndex -> playAtIndex(context, action.index)
        }
    }

    fun playAtIndex(context: Context, index: Int): Boolean {
        val item = _state.value.items.getOrNull(index) ?: return false
        val note = noteRepository.getNote(item.fileName) ?: return false
        if (!TtsPlaybackManager.noteHasReadableContent(note.content)) return false
        playingIndex = index
        TtsPlaybackManager.startPlayback(
            context = context,
            fileName = note.fileName,
            title = note.title,
            content = note.content,
        )
        syncLoopToController()
        return true
    }

    fun playItem(context: Context, fileName: String): Boolean {
        val index = _state.value.items.indexOfFirst { it.fileName == fileName }
        if (index < 0) return false
        return playAtIndex(context, index)
    }

    fun syncLoopToController() {
        val loop = _state.value.repeatMode == TtsQueueRepeatMode.REPEAT_ONE
        TtsPlaybackManager.getOrNull()?.setLoopEnabled(loop)
    }

    private fun sanitizeRepeatMode(snapshot: TtsPlaylistSnapshot): TtsPlaylistSnapshot {
        if (snapshot.repeatMode == TtsQueueRepeatMode.REPEAT_ALL &&
            !TtsPlaylistPolicy.canSelectRepeatAll(snapshot.items)
        ) {
            return snapshot.copy(repeatMode = TtsQueueRepeatMode.OFF)
        }
        return snapshot
    }

    private fun persist(snapshot: TtsPlaylistSnapshot) {
        val sanitized = sanitizeRepeatMode(snapshot)
        store.write(sanitized)
        _state.value = sanitized
        syncLoopToController()
    }
}
