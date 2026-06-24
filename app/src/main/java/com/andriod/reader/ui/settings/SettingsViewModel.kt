package com.andriod.reader.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriod.reader.data.local.AppDiagnosticLog
import com.andriod.reader.data.local.AppStorageAnalyzer
import com.andriod.reader.data.local.AppStorageCleaner
import com.andriod.reader.data.local.DiagnosticLogExporter
import com.andriod.reader.data.local.StorageCategory
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.data.repository.SyncRepository
import com.andriod.reader.domain.GitHubSettings
import com.andriod.reader.domain.TtsSpeechBackend
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.domain.TtsVoicePreference
import com.andriod.reader.service.TtsHelper
import com.andriod.reader.service.TtsVoiceQuality
import com.andriod.reader.service.TtsPlaybackManager
import com.andriod.reader.service.synthesis.SherpaModelManager
import com.andriod.reader.ui.theme.AppThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUiState(
    val token: String = "",
    val owner: String = "dermotv5chat",
    val repo: String = "notes",
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val keepScreenOn: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
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
    val speechBackend: TtsSpeechBackend = TtsSpeechBackend.SYSTEM,
    val qualityGuide: String? = null,
    val voicePickerExpanded: Boolean = false,
    val logLineCount: Int = 0,
    val logSizeBytes: Long = 0,
    val isExportingLog: Boolean = false,
    val isClearingLog: Boolean = false,
    val storageCategories: List<StorageCategory> = emptyList(),
    val storageTotalBytes: Long = 0,
    val selectedStorageIds: Set<String> = emptySet(),
    val isAnalyzingStorage: Boolean = false,
    val isCleaningStorage: Boolean = false,
    val sherpaModelInstalled: Boolean = false,
    val isDownloadingSherpaModel: Boolean = false,
    val sherpaDownloadHint: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
    private val syncRepository: SyncRepository,
    private val diagnosticLog: AppDiagnosticLog,
    private val diagnosticLogExporter: DiagnosticLogExporter,
    private val storageAnalyzer: AppStorageAnalyzer,
    private val storageCleaner: AppStorageCleaner,
) : ViewModel() {
    private val sherpaModelManager = SherpaModelManager(context, diagnosticLog)
    private val _uiState = MutableStateFlow(loadState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private var lastHostContext: Context? = null

    fun setHostContext(hostContext: Context) {
        lastHostContext = hostContext
    }

    private fun ttsContext(): Context = lastHostContext ?: context

    private fun loadState(): SettingsUiState {
        val gh = settingsStore.getGitHubSettings()
        val preference = runCatching {
            TtsVoicePreference.valueOf(settingsStore.getVoicePreference())
        }.getOrDefault(TtsVoicePreference.AUTO)
        return SettingsUiState(
            token = settingsStore.getToken() ?: "",
            owner = gh.owner,
            repo = gh.repo,
            speechRate = settingsStore.getDefaultSpeechRate(),
            speechPitch = settingsStore.getDefaultSpeechPitch(),
            keepScreenOn = settingsStore.isKeepScreenOn(),
            themeMode = settingsStore.getAppThemeMode(),
            selectedVoiceId = settingsStore.getSelectedVoiceId(),
            voicePreference = preference,
            speechBackend = settingsStore.getTtsSpeechBackend(),
            sherpaModelInstalled = sherpaModelManager.isModelInstalled(),
        )
    }

    fun onTokenChange(value: String) = _uiState.update { it.copy(token = value, saved = false) }
    fun onOwnerChange(value: String) = _uiState.update { it.copy(owner = value, saved = false) }
    fun onRepoChange(value: String) = _uiState.update { it.copy(repo = value, saved = false) }
    fun onSpeechRateChange(value: Float) = _uiState.update { it.copy(speechRate = value, saved = false) }
    fun onSpeechPitchChange(value: Float) = _uiState.update { it.copy(speechPitch = value, saved = false) }
    fun onKeepScreenOnChange(value: Boolean) = _uiState.update { it.copy(keepScreenOn = value, saved = false) }

    fun onThemeModeChange(mode: AppThemeMode) {
        settingsStore.saveAppThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun onVoicePickerExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(voicePickerExpanded = expanded) }
    }

    fun onVoiceSelected(voiceId: String) {
        viewModelScope.launch {
            settingsStore.saveSelectedVoiceId(voiceId)
            val controller = TtsPlaybackManager.getOrNull() ?: TtsPlaybackManager.awaitReady(ttsContext())
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

    fun onSpeechBackendChange(backend: TtsSpeechBackend) {
        viewModelScope.launch {
            settingsStore.saveTtsSpeechBackend(backend)
            TtsPlaybackManager.getOrNull()?.applySpeechBackend(backend)
            val wasPlaying = TtsPlaybackManager.session.value.hasActiveSession
            TtsPlaybackManager.reinitialize(ttsContext())
            val controller = TtsPlaybackManager.awaitReady(ttsContext())
            controller.setSpeechRate(_uiState.value.speechRate)
            controller.setPitch(_uiState.value.speechPitch)
            updateDiagnostics(controller)
            _uiState.update {
                it.copy(
                    speechBackend = backend,
                    saved = false,
                    testMessage = if (wasPlaying) "已切换朗读引擎（已停止当前朗读）" else it.testMessage,
                )
            }
        }
    }

    fun onVoicePreferenceChange(preference: TtsVoicePreference) {
        viewModelScope.launch {
            settingsStore.saveVoicePreference(preference.name)
            val controller = TtsPlaybackManager.getOrNull() ?: TtsPlaybackManager.awaitReady(ttsContext())
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
            GitHubSettings(state.owner, state.repo),
        )
        settingsStore.saveDefaultSpeechRate(state.speechRate)
        settingsStore.saveDefaultSpeechPitch(state.speechPitch)
        settingsStore.setKeepScreenOn(state.keepScreenOn)
        settingsStore.saveVoicePreference(state.voicePreference.name)
        settingsStore.saveTtsSpeechBackend(state.speechBackend)
        state.selectedVoiceId?.let { settingsStore.saveSelectedVoiceId(it) }
        TtsPlaybackManager.getOrNull()?.apply {
            applySpeechBackend(state.speechBackend)
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

    fun previewTts() {
        viewModelScope.launch {
            save()
            if (TtsPlaybackManager.session.value.hasActiveSession) {
                TtsPlaybackManager.stopPlayback()
            }
            val controller = TtsPlaybackManager.getOrNull() ?: TtsPlaybackManager.awaitReady(ttsContext())
            controller.previewSample()
        }
    }

    fun refreshTtsInfo() {
        viewModelScope.launch {
            val wasPlaying = TtsPlaybackManager.session.value.hasActiveSession
            _uiState.update { it.copy(isRefreshingTts = true) }
            TtsPlaybackManager.reinitialize(ttsContext())
            val controller = TtsPlaybackManager.awaitReady(ttsContext())
            controller.setSpeechRate(_uiState.value.speechRate)
            controller.setPitch(_uiState.value.speechPitch)
            controller.applyVoicePreference(_uiState.value.voicePreference)
            _uiState.value.selectedVoiceId?.let { controller.applySelectedVoice(it) }
            updateDiagnostics(controller)
            _uiState.update {
                it.copy(
                    isRefreshingTts = false,
                    testMessage = if (wasPlaying) {
                        "已刷新语音引擎（已停止当前朗读）"
                    } else {
                        it.testMessage
                    },
                )
            }
        }
    }

    private fun updateDiagnostics(controller: com.andriod.reader.service.TtsController) {
        val diag = controller.diagnostics()
        val options = controller.listVoiceOptions()
        val backend = controller.speechBackend()
        val activeVoiceId = when (backend) {
            TtsSpeechBackend.ONLINE_EDGE -> settingsStore.getEdgeTtsVoiceId()
            TtsSpeechBackend.OFFLINE_SHERPA -> "sherpa-vits-zh"
            TtsSpeechBackend.SYSTEM -> diag.voiceName?.takeIf { name ->
                options.any { it.id == name }
            } ?: _uiState.value.selectedVoiceId
        }
        val selectedLabel = options.find { it.id == activeVoiceId }?.label ?: diag.voiceName
        val voiceLabel = buildString {
            if (diag.chineseVoiceCount > 0) {
                append("已识别 ${diag.chineseVoiceCount} 个中文语音")
                selectedLabel?.let { append(" · $it") }
            } else {
                append(selectedLabel ?: "未识别")
            }
        }
        val tier = controller.voiceQualityTier()
        val guide = TtsVoiceQuality.qualityGuide(tier, diag.googleTtsInstalled)
        _uiState.update {
            it.copy(
                ttsEngine = diag.engineLabel,
                ttsVoice = voiceLabel,
                ttsVoiceCount = diag.chineseVoiceCount,
                ttsRecommendation = diag.recommendation,
                qualityGuide = if (TtsVoiceQuality.needsQualityHint(tier)) guide else null,
                voiceOptions = options,
                selectedVoiceId = activeVoiceId,
                speechBackend = backend,
                sherpaModelInstalled = TtsPlaybackManager.sherpaModelInstalled() ||
                    sherpaModelManager.isModelInstalled(),
            )
        }
    }

    fun downloadSherpaModel() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isDownloadingSherpaModel = true, sherpaDownloadHint = "正在下载离线语音包…")
            }
            val result = withContext(Dispatchers.IO) {
                sherpaModelManager.downloadAndInstall()
            }
            _uiState.update {
                it.copy(
                    isDownloadingSherpaModel = false,
                    sherpaModelInstalled = sherpaModelManager.isModelInstalled(),
                    sherpaDownloadHint = result.fold(
                        onSuccess = { "离线语音包已就绪" },
                        onFailure = { error -> error.message ?: "下载失败" },
                    ),
                    testMessage = result.fold(
                        onSuccess = { "离线语音包已下载" },
                        onFailure = { error -> "下载失败：${error.message ?: "未知错误"}" },
                    ),
                )
            }
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

    fun refreshLogStats() {
        val stats = diagnosticLog.stats()
        _uiState.update {
            it.copy(logLineCount = stats.lineCount, logSizeBytes = stats.sizeBytes)
        }
    }

    fun refreshStorageStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingStorage = true) }
            val breakdown = withContext(Dispatchers.IO) { storageAnalyzer.analyze() }
            _uiState.update { state ->
                val defaultSelected = breakdown.categories
                    .filter { it.cleanable && it.sizeBytes > 0 }
                    .map { it.id }
                    .toSet()
                val selected = if (state.selectedStorageIds.isEmpty() && defaultSelected.isNotEmpty()) {
                    defaultSelected
                } else {
                    state.selectedStorageIds.intersect(
                        breakdown.categories.filter { it.cleanable }.map { it.id }.toSet(),
                    )
                }
                state.copy(
                    storageCategories = breakdown.categories,
                    storageTotalBytes = breakdown.totalBytes,
                    selectedStorageIds = selected,
                    isAnalyzingStorage = false,
                )
            }
            refreshLogStats()
        }
    }

    fun toggleStorageSelection(id: String) {
        _uiState.update { state ->
            val selected = state.selectedStorageIds.toMutableSet()
            if (id in selected) {
                selected.remove(id)
            } else {
                selected.add(id)
            }
            state.copy(selectedStorageIds = selected)
        }
    }

    fun cleanSelectedStorage() {
        val selected = _uiState.value.selectedStorageIds
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCleaningStorage = true) }
            val result = withContext(Dispatchers.IO) {
                storageCleaner.clean(selected)
            }
            refreshStorageStats()
            val freedMb = result.freedBytes / (1024.0 * 1024.0)
            val message = when {
                result.failedIds.isNotEmpty() && result.cleanedIds.isEmpty() ->
                    "清理失败，请稍后重试"
                result.failedIds.isNotEmpty() ->
                    "部分清理完成，已释放 ${"%.2f".format(freedMb)} MB"
                freedMb >= 0.01 ->
                    "已释放 ${"%.2f".format(freedMb)} MB"
                else ->
                    "已清理选中项"
            }
            _uiState.update {
                it.copy(isCleaningStorage = false, testMessage = message)
            }
        }
    }

    fun exportDiagnosticLog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingLog = true) }
            val message = withContext(Dispatchers.IO) {
                diagnosticLogExporter.export()
                    .fold(
                        onSuccess = { path -> "日志已保存：$path" },
                        onFailure = { error -> "导出失败：${error.message ?: "未知错误"}" },
                    )
            }
            refreshLogStats()
            _uiState.update {
                it.copy(isExportingLog = false, testMessage = message)
            }
        }
    }

    fun clearDiagnosticLog() {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearingLog = true) }
            withContext(Dispatchers.IO) {
                diagnosticLog.clear()
            }
            refreshLogStats()
            refreshStorageStats()
            _uiState.update {
                it.copy(isClearingLog = false, testMessage = "已清除诊断日志")
            }
        }
    }
}
