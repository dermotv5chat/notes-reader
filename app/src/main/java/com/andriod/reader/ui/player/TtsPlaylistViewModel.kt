package com.andriod.reader.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriod.reader.domain.TtsQueueRepeatMode
import com.andriod.reader.service.TtsPlaybackManager
import com.andriod.reader.service.TtsPlaylistManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TtsPlaylistUiState(
    val isStartingPlayback: Boolean = false,
    val playbackError: String? = null,
)

@HiltViewModel
class TtsPlaylistViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playlistManager: TtsPlaylistManager,
) : ViewModel() {
    val snapshot = playlistManager.state

    private val _uiState = MutableStateFlow(TtsPlaylistUiState())
    val uiState: StateFlow<TtsPlaylistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            TtsPlaybackManager.playbackError.collect { message ->
                if (message != null) {
                    _uiState.update { it.copy(playbackError = message) }
                }
            }
        }
    }

    fun add(fileName: String, title: String) = playlistManager.add(fileName, title)

    fun remove(fileName: String) = playlistManager.remove(fileName)

    fun clear() = playlistManager.clear()

    fun setRepeatMode(mode: TtsQueueRepeatMode) = playlistManager.setRepeatMode(mode)

    fun playItem(fileName: String) {
        startPlayback { playlistManager.playItem(context, fileName) }
    }

    fun playFromStart() {
        startPlayback { playlistManager.playFromStart(context) }
    }

    fun clearPlaybackError() {
        TtsPlaybackManager.clearPlaybackError()
        _uiState.update { it.copy(playbackError = null) }
    }

    fun contains(fileName: String): Boolean = playlistManager.contains(fileName)

    private fun startPlayback(play: () -> Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isStartingPlayback = true, playbackError = null) }
            TtsPlaybackManager.clearPlaybackError()
            val ctrl = TtsPlaybackManager.awaitReady(context)
            if (!ctrl.isReady()) {
                _uiState.update {
                    it.copy(
                        isStartingPlayback = false,
                        playbackError = "语音引擎尚未就绪",
                    )
                }
                return@launch
            }
            TtsPlaybackManager.presynthUnavailableMessage(context)?.let { blocked ->
                _uiState.update {
                    it.copy(
                        isStartingPlayback = false,
                        playbackError = blocked,
                    )
                }
                TtsPlaybackManager.reportPlaybackError(blocked)
                return@launch
            }
            val ok = play()
            val errorMessage = if (ok) {
                null
            } else {
                TtsPlaybackManager.playbackError.value ?: "无法播放该笔记"
            }
            _uiState.update {
                it.copy(
                    isStartingPlayback = false,
                    playbackError = errorMessage,
                )
            }
        }
    }
}
