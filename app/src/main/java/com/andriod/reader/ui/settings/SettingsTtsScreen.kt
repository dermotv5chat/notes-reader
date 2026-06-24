package com.andriod.reader.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andriod.reader.ui.tts.TtsVoiceSettingsSection

@Composable
fun SettingsTtsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    SettingsLifecycleEffects(viewModel = viewModel, refreshTts = true)
    SettingsSnackbarEffect(
        testMessage = uiState.testMessage,
        snackbar = snackbar,
        onClear = viewModel::clearTestMessage,
    )

    SettingsScaffold(
        title = "语音朗读",
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("当前引擎：${uiState.ttsEngine.ifBlank { "检测中…" }}")
            Text(
                uiState.ttsVoice.ifBlank { "检测中…" },
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                uiState.ttsRecommendation,
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
            )
            TtsVoiceSettingsSection(
                voiceOptions = uiState.voiceOptions,
                selectedVoiceId = uiState.selectedVoiceId,
                voicePreference = uiState.voicePreference,
                speechBackend = uiState.speechBackend,
                qualityGuide = uiState.qualityGuide,
                onVoicePreferenceChange = viewModel::onVoicePreferenceChange,
                onVoicePickerExpandedChange = viewModel::onVoicePickerExpandedChange,
                onVoiceSelected = viewModel::onVoiceSelected,
                onSpeechBackendChange = viewModel::onSpeechBackendChange,
                voicePickerExpanded = uiState.voicePickerExpanded,
                sherpaModelPacks = uiState.sherpaModelPacks,
                selectedSherpaPackId = uiState.selectedSherpaPackId,
                installedSherpaPackIds = uiState.installedSherpaPackIds,
                selectedSherpaSpeakerId = uiState.selectedSherpaSpeakerId,
                sherpaPackPickerExpanded = uiState.sherpaPackPickerExpanded,
                onSherpaPackPickerExpandedChange = viewModel::onSherpaPackPickerExpandedChange,
                onSherpaPackSelected = viewModel::onSherpaPackSelected,
                onSherpaSpeakerSelected = viewModel::onSherpaSpeakerSelected,
                isDownloadingSherpaModel = uiState.isDownloadingSherpaModel,
                downloadingSherpaPackId = uiState.downloadingSherpaPackId,
                sherpaDownloadHint = uiState.sherpaDownloadHint,
                sherpaDownloadProgress = uiState.sherpaDownloadProgress,
                sherpaDownloadPhase = uiState.sherpaDownloadPhase,
                sherpaDownloadBytesLabel = uiState.sherpaDownloadBytesLabel,
            )
            OutlinedButton(
                onClick = viewModel::previewTts,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            ) {
                Text("试听当前语音")
            }
            Text("默认语速 ${"%.1f".format(uiState.speechRate)}x（阅读页可临时调整）")
            Slider(
                value = uiState.speechRate,
                onValueChange = viewModel::onSpeechRateChange,
                valueRange = 0.5f..2.0f,
            )
            Text("音调 ${"%.1f".format(uiState.speechPitch)}（略降有时更自然）")
            Slider(
                value = uiState.speechPitch,
                onValueChange = viewModel::onSpeechPitchChange,
                valueRange = 0.8f..1.2f,
            )
            RowSwitch(
                label = "阅读时保持屏幕常亮",
                checked = uiState.keepScreenOn,
                onCheckedChange = viewModel::onKeepScreenOnChange,
            )
            OutlinedButton(
                onClick = viewModel::openTtsSettings,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("打开系统 TTS 设置")
            }
            OutlinedButton(
                onClick = viewModel::openGoogleTtsStore,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text("安装 Google 文字转语音")
            }
            OutlinedButton(
                onClick = viewModel::refreshTtsInfo,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = !uiState.isRefreshingTts,
            ) {
                if (uiState.isRefreshingTts) {
                    CircularProgressIndicator()
                } else {
                    Text("重新检测语音引擎")
                }
            }
            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 16.dp),
            ) {
                Text(if (uiState.saved) "已保存" else "保存设置")
            }
        }
    }
}
