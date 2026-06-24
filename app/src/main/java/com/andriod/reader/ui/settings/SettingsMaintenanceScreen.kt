package com.andriod.reader.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
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

@Composable
fun SettingsMaintenanceScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    SettingsLifecycleEffects(
        viewModel = viewModel,
        refreshTts = false,
        refreshMaintenance = true,
    )
    SettingsSnackbarEffect(
        testMessage = uiState.testMessage,
        snackbar = snackbar,
        onClear = viewModel::clearTestMessage,
    )

    SettingsScaffold(
        title = "存储与诊断",
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
            SettingsGroupHeader("存储空间")
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            SettingsGroupHeader("诊断日志")
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
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                enabled = !uiState.isClearingLog && uiState.logLineCount > 0,
            ) {
                if (uiState.isClearingLog) {
                    CircularProgressIndicator()
                } else {
                    Text("清除日志")
                }
            }
        }
    }
}
