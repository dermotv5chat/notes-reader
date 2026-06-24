package com.andriod.reader.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
fun SettingsHomeScreen(
    onOpenTts: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenMaintenance: () -> Unit,
    onOpenPrinciplesGuide: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    SettingsLifecycleEffects(
        viewModel = viewModel,
        refreshTts = true,
        refreshMaintenance = true,
    )
    SettingsSnackbarEffect(
        testMessage = uiState.testMessage,
        snackbar = snackbar,
        onClear = viewModel::clearTestMessage,
    )

    SettingsScaffold(
        title = "设置",
        onBack = null,
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsGroupHeader("外观")
            ThemeModeSelector(
                selected = uiState.themeMode,
                onSelect = viewModel::onThemeModeChange,
            )
            Text(
                "浅色 / 深色 / 跟随系统。更改后立即生效。",
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            SettingsGroupHeader("更多设置")
            SettingsNavRow(
                title = "语音朗读",
                subtitle = viewModel.ttsSummary(uiState),
                onClick = onOpenTts,
            )
            SettingsNavRow(
                title = "GitHub 同步",
                subtitle = viewModel.syncSummary(uiState),
                onClick = onOpenSync,
            )
            SettingsNavRow(
                title = "存储与诊断",
                subtitle = viewModel.maintenanceSummary(uiState),
                onClick = onOpenMaintenance,
            )
            SettingsNavRow(
                title = "行为准则",
                subtitle = "编写与践行说明",
                onClick = onOpenPrinciplesGuide,
            )
        }
    }
}
