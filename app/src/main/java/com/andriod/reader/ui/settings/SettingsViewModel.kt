package com.andriod.reader.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.data.repository.SyncRepository
import com.andriod.reader.domain.GitHubSettings
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val keepScreenOn: Boolean = false,
    val saved: Boolean = false,
    val testMessage: String? = null,
    val isTesting: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    private val syncRepository: SyncRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun loadState(): SettingsUiState {
        val gh = settingsStore.getGitHubSettings()
        return SettingsUiState(
            token = settingsStore.getToken() ?: "",
            owner = gh.owner,
            repo = gh.repo,
            notesPath = gh.notesPath,
            speechRate = settingsStore.getDefaultSpeechRate(),
            keepScreenOn = settingsStore.isKeepScreenOn(),
        )
    }

    fun onTokenChange(value: String) = _uiState.update { it.copy(token = value, saved = false) }
    fun onOwnerChange(value: String) = _uiState.update { it.copy(owner = value, saved = false) }
    fun onRepoChange(value: String) = _uiState.update { it.copy(repo = value, saved = false) }
    fun onNotesPathChange(value: String) = _uiState.update { it.copy(notesPath = value, saved = false) }
    fun onSpeechRateChange(value: Float) = _uiState.update { it.copy(speechRate = value, saved = false) }
    fun onKeepScreenOnChange(value: Boolean) = _uiState.update { it.copy(keepScreenOn = value, saved = false) }

    fun save() {
        val state = _uiState.value
        if (state.token.isNotBlank()) {
            settingsStore.saveToken(state.token)
        }
        settingsStore.saveGitHubSettings(
            GitHubSettings(state.owner, state.repo, state.notesPath),
        )
        settingsStore.saveDefaultSpeechRate(state.speechRate)
        settingsStore.setKeepScreenOn(state.keepScreenOn)
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

    fun clearTestMessage() {
        _uiState.update { it.copy(testMessage = null) }
    }
}
