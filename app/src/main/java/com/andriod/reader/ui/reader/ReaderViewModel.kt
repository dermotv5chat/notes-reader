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
import com.andriod.reader.service.TtsPlaybackManager
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
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val fileName: String = savedStateHandle.get<String>("fileName") ?: ""

    private val _uiState = MutableStateFlow(
        ReaderUiState(
            note = noteRepository.getNote(fileName),
            speechRate = settingsStore.getDefaultSpeechRate(),
            speechPitch = settingsStore.getDefaultSpeechPitch(),
            keepScreenOn = settingsStore.isKeepScreenOn(),
            selectedVoiceId = settingsStore.getSelectedVoiceId(),
            voicePreference = readVoicePreference(),
        ),
    )
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var controller: com.andriod.reader.service.TtsController? = null

    init {
        initTts()
    }

    private fun readVoicePreference(): TtsVoicePreference {
        return runCatching {
            TtsVoicePreference.valueOf(settingsStore.getVoicePreference())
        }.getOrDefault(TtsVoicePreference.AUTO)
    }

    fun initTts() {
        viewModelScope.launch {
            val tts = TtsPlaybackManager.getOrCreate(
                context = context,
                onSegmentChanged = { index, total ->
                    _uiState.update { it.copy(segmentIndex = index, segmentTotal = total) }
                },
                onPlaybackStateChanged = { playing ->
                    _uiState.update { it.copy(isPlaying = playing) }
                },
            )
            tts.awaitReady()
            tts.setSpeechRate(_uiState.value.speechRate)
            tts.setPitch(_uiState.value.speechPitch)
            tts.applyVoicePreference(_uiState.value.voicePreference)
            _uiState.value.selectedVoiceId?.let { tts.applySelectedVoice(it) }
            controller = tts
            refreshVoiceOptions(tts)
            _uiState.update { it.copy(isTtsReady = true) }
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
        val tts = controller ?: return
        val state = _uiState.value
        if (state.isPlaying) {
            tts.pause()
        } else if (state.segmentTotal == 0) {
            state.note?.content?.let { tts.start(it) }
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
