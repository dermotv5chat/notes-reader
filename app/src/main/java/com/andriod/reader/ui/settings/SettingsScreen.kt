package com.andriod.reader.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.andriod.reader.ui.theme.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    LaunchedEffect(context) {
        viewModel.setHostContext(context)
        viewModel.refreshTtsInfo()
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.setHostContext(context)
                viewModel.refreshTtsInfo()
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
                "朗读语音和语速请在笔记阅读页调整。",
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                uiState.ttsRecommendation,
                modifier = Modifier.padding(top = 8.dp),
            )
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
                    .padding(top = 24.dp),
            ) {
                Text(if (uiState.saved) "已保存" else "保存设置")
            }
        }
    }
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
