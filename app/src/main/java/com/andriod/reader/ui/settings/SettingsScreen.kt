package com.andriod.reader.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.andriod.reader.data.local.StorageCategory
import com.andriod.reader.ui.theme.AppThemeMode
import com.andriod.reader.ui.tts.TtsVoiceSettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onOpenPrinciplesGuide: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    LaunchedEffect(context) {
        viewModel.setHostContext(context)
        viewModel.refreshTtsInfo()
        viewModel.refreshLogStats()
        viewModel.refreshStorageStats()
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.setHostContext(context)
                viewModel.refreshTtsInfo()
                viewModel.refreshLogStats()
                viewModel.refreshStorageStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.testMessage) {
        uiState.testMessage?.let {
            snackbar.showSnackbar(it)
            viewModel.clearTestMessage()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("外观", modifier = Modifier.padding(bottom = 8.dp))
            ThemeModeSelector(
                selected = uiState.themeMode,
                onSelect = viewModel::onThemeModeChange,
            )
            Text(
                "浅色 / 深色 / 跟随系统。更改后立即生效。",
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            Text("行为准则", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            Text(
                "编写准则、阅读页记录今日遵守/违背、Markdown 与工具栏说明。",
                modifier = Modifier.padding(bottom = 8.dp),
            )
            OutlinedButton(
                onClick = onOpenPrinciplesGuide,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("行为准则使用说明")
            }

            Text("GitHub 同步", modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            OutlinedTextField(
                value = uiState.token,
                onValueChange = viewModel::onTokenChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("GitHub PAT") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.owner,
                onValueChange = viewModel::onOwnerChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("仓库 Owner") },
                singleLine = true,
            )
            OutlinedTextField(
                value = uiState.repo,
                onValueChange = viewModel::onRepoChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("仓库名") },
                singleLine = true,
            )
            Text(
                "整个仓库均为笔记，同步会递归扫描所有子目录中的 .md 文件，并与手机本地目录结构保持一致。",
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                "Fine-grained Token 须授权此仓库并开启 Contents 读写；Classic Token 须勾选 repo 权限。",
                modifier = Modifier.padding(top = 8.dp),
            )
            OutlinedButton(
                onClick = viewModel::testConnection,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = !uiState.isTesting && uiState.token.isNotBlank(),
            ) {
                if (uiState.isTesting) {
                    CircularProgressIndicator()
                } else {
                    Text("测试 GitHub 连接")
                }
            }

            Text("语音朗读", modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            Text("当前引擎：${uiState.ttsEngine.ifBlank { "检测中…" }}")
            Text(
                uiState.ttsVoice.ifBlank { "检测中…" },
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                uiState.ttsRecommendation,
                modifier = Modifier.padding(top = 8.dp),
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
                sherpaModelInstalled = uiState.sherpaModelInstalled,
                isDownloadingSherpaModel = uiState.isDownloadingSherpaModel,
                sherpaDownloadHint = uiState.sherpaDownloadHint,
                onDownloadSherpaModel = viewModel::downloadSherpaModel,
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

            Text("存储空间", modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            val storageTotalLabel = formatStorageSize(uiState.storageTotalBytes)
            Text("应用占用约 $storageTotalLabel")
            if (uiState.isAnalyzingStorage && uiState.storageCategories.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.padding(vertical = 8.dp))
            } else {
                uiState.storageCategories.forEach { category ->
                    StorageCategoryRow(
                        category = category,
                        selected = category.id in uiState.selectedStorageIds,
                        onToggle = { viewModel.toggleStorageSelection(category.id) },
                    )
                }
            }
            OutlinedButton(
                onClick = viewModel::cleanSelectedStorage,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = !uiState.isCleaningStorage &&
                    uiState.selectedStorageIds.isNotEmpty() &&
                    !uiState.isAnalyzingStorage,
            ) {
                if (uiState.isCleaningStorage) {
                    CircularProgressIndicator()
                } else {
                    Text("清理选中项")
                }
            }

            Text("诊断日志", modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            val logSizeKb = (uiState.logSizeBytes + 1023) / 1024
            Text("当前日志：${uiState.logLineCount} 行 / ${logSizeKb} KB")
            Text(
                "记录朗读、同步与播放状态（不含 token 与笔记正文）。导出后可用 adb logcat -s ReaderDiag 对照调试。",
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            OutlinedButton(
                onClick = viewModel::exportDiagnosticLog,
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isExportingLog && uiState.logLineCount > 0,
            ) {
                if (uiState.isExportingLog) {
                    CircularProgressIndicator()
                } else {
                    Text("导出日志到本地")
                }
            }
            OutlinedButton(
                onClick = viewModel::clearDiagnosticLog,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                enabled = !uiState.isClearingLog && uiState.logLineCount > 0,
            ) {
                if (uiState.isClearingLog) {
                    CircularProgressIndicator()
                } else {
                    Text("清除日志")
                }
            }

            Button(
                onClick = viewModel::save,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
            ) {
                Text(if (uiState.saved) "已保存" else "保存设置")
            }
        }
    }
}

@Composable
private fun StorageCategoryRow(
    category: StorageCategory,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        if (category.cleanable) {
            Checkbox(
                checked = selected,
                onCheckedChange = { onToggle() },
            )
        } else {
            Spacer(modifier = Modifier.width(48.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("${category.label} · ${formatStorageSize(category.sizeBytes)}")
            Text(
                text = category.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (category.cleanable) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
        }
    }
}

private fun formatStorageSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = (bytes + 1023) / 1024
    if (kb < 1024) return "$kb KB"
    val mb = bytes / (1024.0 * 1024.0)
    return "${"%.2f".format(mb)} MB"
}

@Composable
private fun ThemeModeSelector(
    selected: AppThemeMode,
    onSelect: (AppThemeMode) -> Unit,
) {
    val modes = AppThemeMode.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        modes.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
            ) {
                Text(
                    when (mode) {
                        AppThemeMode.LIGHT -> "浅色"
                        AppThemeMode.DARK -> "深色"
                        AppThemeMode.SYSTEM -> "跟随系统"
                    },
                )
            }
        }
    }
}

@Composable
private fun RowSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
