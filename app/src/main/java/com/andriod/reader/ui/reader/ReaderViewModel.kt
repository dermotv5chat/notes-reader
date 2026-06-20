package com.andriod.reader.ui.reader

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.data.repository.NoteRepository
import com.andriod.reader.domain.Note
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.domain.TtsVoicePreference
import com.andriod.reader.service.TtsHelper
import com.andriod.reader.service.TtsPlaybackManager
import com.andriod.reader.ui.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val note: Note? = null,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val keepScreenOn: Boolean = false,
    val isPlaying: Boolean = false,
    val segmentIndex: Int = 0,
    val segmentTotal: Int = 0,
    val voiceOptions: List<TtsVoiceOption> = emptyList(),
    val selectedVoiceId: String? = null,
    val voicePreference: TtsVoicePreference = TtsVoicePreference.AUTO,
    val voicePickerExpanded: Boolean = false,
    val isTtsReady: Boolean = false,
    val isTtsInitializing: Boolean = false,
    val ttsError: String? = null,
    val loopEnabled: Boolean = false,
    val ttsSettingsVisible: Boolean = false,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val fileName: String = NavArgs.decodeFileName(savedStateHandle.get<String>("fileName"))

    private val _uiState = MutableStateFlow(
        ReaderUiState(
            note = noteRepository.getNote(fileName),
            speechRate = settingsStore.getDefaultSpeechRate(),
            speechPitch = settingsStore.getDefaultSpeechPitch(),
            keepScreenOn = settingsStore.isKeepScreenOn(),
            selectedVoiceId = settingsStore.getSelectedVoiceId(),
            voicePreference = readVoicePreference(),
            loopEnabled = settingsStore.isLoopPlaybackEnabled(),
        ),
    )
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var controller: com.andriod.reader.service.TtsController? = null
    private var lastHostContext: Context? = null

    private fun readVoicePreference(): TtsVoicePreference {
        return runCatching {
            TtsVoicePreference.valueOf(settingsStore.getVoicePreference())
        }.getOrDefault(TtsVoicePreference.AUTO)
    }

    fun initTts(hostContext: Context) {
        lastHostContext = hostContext
        viewModelScope.launch {
            _uiState.update { it.copy(isTtsInitializing = true, ttsError = null, isTtsReady = false) }
            TtsPlaybackManager.release()
            val tts = TtsPlaybackManager.getOrCreate(
                context = hostContext,
                onSegmentChanged = { index, total ->
                    _uiState.update { it.copy(segmentIndex = index, segmentTotal = total) }
                },
                onPlaybackStateChanged = { playing ->
                    _uiState.update { it.copy(isPlaying = playing) }
                },
                onSpeakError = { message ->
                    _uiState.update { it.copy(ttsError = message, isPlaying = false) }
                },
            )
            val ready = tts.awaitReady(hostContext)
            if (!ready) {
                val engines = TtsHelper.listInstalledEngines(hostContext)
                val defaultEngine = TtsHelper.defaultEnginePackage(hostContext)
                val tried = tts.attemptedEngineLabels()
                _uiState.update {
                    it.copy(
                        isTtsInitializing = false,
                        isTtsReady = false,
                        ttsError = buildString {
                            append("无法启动语音引擎。")
                            if (tried.isNotEmpty()) {
                                append("已尝试：${tried.joinToString()}。")
                            }
                            if (!defaultEngine.isNullOrBlank()) {
                                append("系统默认：$defaultEngine。")
                            }
                            if (engines.isNotEmpty()) {
                                append("已安装：${engines.joinToString()}。")
                            }
                            append("请到 设置 → 更多设置 → 无障碍 → 文字转语音输出，确认默认引擎能试听中文。")
                        },
                    )
                }
                return@launch
            }
            tts.setSpeechRate(_uiState.value.speechRate)
            tts.setPitch(_uiState.value.speechPitch)
            tts.setLoopEnabled(_uiState.value.loopEnabled)
            tts.applyVoicePreference(_uiState.value.voicePreference)
            _uiState.value.selectedVoiceId?.let { tts.applySelectedVoice(it) }
            controller = tts
            refreshVoiceOptions(tts)
            _uiState.update {
                it.copy(
                    isTtsInitializing = false,
                    isTtsReady = true,
                    ttsError = null,
                )
            }
        }
    }

    private fun refreshVoiceOptions(tts: com.andriod.reader.service.TtsController) {
        val options = tts.listVoiceOptions()
        val activeId = tts.diagnostics().voiceName?.takeIf { id -> options.any { it.id == id } }
            ?: _uiState.value.selectedVoiceId
        _uiState.update {
            it.copy(voiceOptions = options, selectedVoiceId = activeId)
        }
    }

    fun onSpeechRateChange(rate: Float) {
        settingsStore.saveDefaultSpeechRate(rate)
        controller?.setSpeechRate(rate)
        _uiState.update { it.copy(speechRate = rate) }
    }

    fun onSpeechPitchChange(pitch: Float) {
        settingsStore.saveDefaultSpeechPitch(pitch)
        controller?.setPitch(pitch)
        _uiState.update { it.copy(speechPitch = pitch) }
    }

    fun openTtsSettings() {
        _uiState.update { it.copy(ttsSettingsVisible = true) }
    }

    fun closeTtsSettings() {
        _uiState.update { it.copy(ttsSettingsVisible = false, voicePickerExpanded = false) }
    }

    fun toggleLoop() {
        val enabled = !_uiState.value.loopEnabled
        settingsStore.saveLoopPlayback(enabled)
        controller?.setLoopEnabled(enabled)
        _uiState.update { it.copy(loopEnabled = enabled) }
    }

    fun onVoicePickerExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(voicePickerExpanded = expanded) }
    }

    fun onVoiceSelected(voiceId: String) {
        settingsStore.saveSelectedVoiceId(voiceId)
        controller?.applySelectedVoice(voiceId)
        _uiState.update {
            it.copy(selectedVoiceId = voiceId, voicePickerExpanded = false)
        }
    }

    fun onVoicePreferenceChange(preference: TtsVoicePreference) {
        settingsStore.saveVoicePreference(preference.name)
        controller?.applyVoicePreference(preference)
        controller?.let { refreshVoiceOptions(it) }
        _uiState.update { it.copy(voicePreference = preference) }
    }

    fun togglePlayPause() {
        val tts = controller
        val state = _uiState.value
        if (tts == null || !state.isTtsReady) {
            _uiState.update { it.copy(ttsError = it.ttsError ?: "语音引擎尚未就绪") }
            lastHostContext?.let { initTts(it) }
            return
        }
        if (state.note == null) {
            _uiState.update { it.copy(ttsError = "笔记不存在，无法朗读") }
            return
        }
        val content = state.note.content
        if (content.isBlank()) {
            _uiState.update { it.copy(ttsError = "笔记内容为空") }
            return
        }
        _uiState.update { it.copy(ttsError = null) }
        if (state.isPlaying) {
            tts.pause()
        } else if (state.segmentTotal == 0) {
            tts.start(content)
        } else {
            tts.resume()
        }
    }

    fun stop() = controller?.stop()

    fun nextSegment() = controller?.nextSegment()

    fun releaseTts() {
        TtsPlaybackManager.release()
        controller = null
    }
}
