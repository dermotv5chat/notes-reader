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
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

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
            Text("GitHub 同步", modifier = Modifier.padding(bottom = 8.dp))
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
            OutlinedTextField(
                value = uiState.notesPath,
                onValueChange = viewModel::onNotesPathChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text("笔记目录路径") },
                placeholder = { Text("留空=仓库根目录，或填 notes") },
                singleLine = true,
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
            Text("默认语速 ${"%.1f".format(uiState.speechRate)}x")
            Slider(
                value = uiState.speechRate,
                onValueChange = viewModel::onSpeechRateChange,
                valueRange = 0.5f..2.0f,
            )
            RowSwitch(
                label = "阅读时保持屏幕常亮",
                checked = uiState.keepScreenOn,
                onCheckedChange = viewModel::onKeepScreenOnChange,
            )
            Text(
                "建议安装 Google 文字转语音，并下载中文神经网络语音包以获得更自然的朗读效果。",
                modifier = Modifier.padding(top = 8.dp),
            )

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
