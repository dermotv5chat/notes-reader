package com.andriod.reader.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.data.repository.SyncRepository
import com.andriod.reader.domain.GitHubSettings
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.domain.TtsVoicePreference
import com.andriod.reader.service.TtsHelper
import com.andriod.reader.service.TtsPlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val token: String = "",
    val owner: String = "dermotv5chat",
    val repo: String = "notes",
    val notesPath: String = "notes",
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val keepScreenOn: Boolean = false,
    val saved: Boolean = false,
    val testMessage: String? = null,
    val isTesting: Boolean = false,
    val ttsEngine: String = "",
    val ttsVoice: String = "",
    val ttsVoiceCount: Int = 0,
    val ttsRecommendation: String = "",
    val isRefreshingTts: Boolean = false,
    val voiceOptions: List<TtsVoiceOption> = emptyList(),
    val selectedVoiceId: String? = null,
    val voicePreference: TtsVoicePreference = TtsVoicePreference.AUTO,
    val voicePickerExpanded: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
    private val syncRepository: SyncRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        refreshTtsInfo()
    }

    private fun loadState(): SettingsUiState {
        val gh = settingsStore.getGitHubSettings()
        val preference = runCatching {
            TtsVoicePreference.valueOf(settingsStore.getVoicePreference())
        }.getOrDefault(TtsVoicePreference.AUTO)
        return SettingsUiState(
            token = settingsStore.getToken() ?: "",
            owner = gh.owner,
            repo = gh.repo,
            notesPath = gh.notesPath,
            speechRate = settingsStore.getDefaultSpeechRate(),
            speechPitch = settingsStore.getDefaultSpeechPitch(),
            keepScreenOn = settingsStore.isKeepScreenOn(),
            selectedVoiceId = settingsStore.getSelectedVoiceId(),
            voicePreference = preference,
        )
    }

    fun onTokenChange(value: String) = _uiState.update { it.copy(token = value, saved = false) }
    fun onOwnerChange(value: String) = _uiState.update { it.copy(owner = value, saved = false) }
    fun onRepoChange(value: String) = _uiState.update { it.copy(repo = value, saved = false) }
    fun onNotesPathChange(value: String) = _uiState.update { it.copy(notesPath = value, saved = false) }
    fun onSpeechRateChange(value: Float) = _uiState.update { it.copy(speechRate = value, saved = false) }
    fun onSpeechPitchChange(value: Float) = _uiState.update { it.copy(speechPitch = value, saved = false) }
    fun onKeepScreenOnChange(value: Boolean) = _uiState.update { it.copy(keepScreenOn = value, saved = false) }

    fun onVoicePickerExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(voicePickerExpanded = expanded) }
    }

    fun onVoiceSelected(voiceId: String) {
        viewModelScope.launch {
            settingsStore.saveSelectedVoiceId(voiceId)
            val controller = TtsPlaybackManager.getOrNull() ?: TtsPlaybackManager.awaitReady(context)
            controller.applySelectedVoice(voiceId)
            _uiState.update {
                it.copy(
                    selectedVoiceId = voiceId,
                    voicePickerExpanded = false,
                    saved = false,
                )
            }
            updateDiagnostics(controller)
        }
    }

    fun onVoicePreferenceChange(preference: TtsVoicePreference) {
        viewModelScope.launch {
            settingsStore.saveVoicePreference(preference.name)
            val controller = TtsPlaybackManager.getOrNull() ?: TtsPlaybackManager.awaitReady(context)
            controller.applyVoicePreference(preference)
            _uiState.update { it.copy(voicePreference = preference, saved = false) }
            updateDiagnostics(controller)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.token.isNotBlank()) {
            settingsStore.saveToken(state.token)
        }
        settingsStore.saveGitHubSettings(
            GitHubSettings(state.owner, state.repo, state.notesPath),
        )
        settingsStore.saveDefaultSpeechRate(state.speechRate)
        settingsStore.saveDefaultSpeechPitch(state.speechPitch)
        settingsStore.setKeepScreenOn(state.keepScreenOn)
        settingsStore.saveVoicePreference(state.voicePreference.name)
        state.selectedVoiceId?.let { settingsStore.saveSelectedVoiceId(it) }
        TtsPlaybackManager.getOrNull()?.apply {
            setSpeechRate(state.speechRate)
            setPitch(state.speechPitch)
            applyVoicePreference(state.voicePreference)
            state.selectedVoiceId?.let { applySelectedVoice(it) }
        }
        _uiState.update { it.copy(saved = true, testMessage = null) }
    }

    fun testConnection() {
        viewModelScope.launch {
            save()
            _uiState.update { it.copy(isTesting = true, testMessage = null) }
            val message = syncRepository.testConnectionMessage()
            _uiState.update { it.copy(isTesting = false, testMessage = message) }
        }
    }

    fun refreshTtsInfo() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingTts = true) }
            TtsPlaybackManager.reinitialize(context)
            val controller = TtsPlaybackManager.awaitReady(context)
            controller.setSpeechRate(_uiState.value.speechRate)
            controller.setPitch(_uiState.value.speechPitch)
            controller.applyVoicePreference(_uiState.value.voicePreference)
            _uiState.value.selectedVoiceId?.let { controller.applySelectedVoice(it) }
            updateDiagnostics(controller)
            _uiState.update { it.copy(isRefreshingTts = false) }
        }
    }

    private fun updateDiagnostics(controller: com.andriod.reader.service.TtsController) {
        val diag = controller.diagnostics()
        val options = controller.listVoiceOptions()
        val activeVoiceId = diag.voiceName?.takeIf { name ->
            options.any { it.id == name }
        } ?: _uiState.value.selectedVoiceId
        val selectedLabel = options.find { it.id == activeVoiceId }?.label ?: diag.voiceName
        val voiceLabel = buildString {
            if (diag.chineseVoiceCount > 0) {
                append("已识别 ${diag.chineseVoiceCount} 个中文语音")
                selectedLabel?.let { append(" · $it") }
            } else {
                append(selectedLabel ?: "未识别")
            }
        }
        _uiState.update {
            it.copy(
                ttsEngine = diag.engineLabel,
                ttsVoice = voiceLabel,
                ttsVoiceCount = diag.chineseVoiceCount,
                ttsRecommendation = diag.recommendation,
                voiceOptions = options,
                selectedVoiceId = activeVoiceId,
            )
        }
    }

    fun previewTts() {
        viewModelScope.launch {
            save()
            val controller = TtsPlaybackManager.getOrNull() ?: TtsPlaybackManager.awaitReady(context)
            controller.previewSample()
        }
    }

    fun openTtsSettings() {
        TtsHelper.openTtsSettings(context)
    }

    fun openGoogleTtsStore() {
        TtsHelper.openGoogleTtsInPlayStore(context)
    }

    fun clearTestMessage() {
        _uiState.update { it.copy(testMessage = null) }
    }
}
