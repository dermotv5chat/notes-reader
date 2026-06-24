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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsSyncScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    SettingsSnackbarEffect(
        testMessage = uiState.testMessage,
        snackbar = snackbar,
        onClear = viewModel::clearTestMessage,
    )

    SettingsScaffold(
        title = "GitHub 同步",
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
