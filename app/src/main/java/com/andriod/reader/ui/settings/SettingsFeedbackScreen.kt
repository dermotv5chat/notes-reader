package com.andriod.reader.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

object SettingsFeedbackTestTags {
    const val PREVIEW_MUYU_SOUND = "settings_preview_muyu_sound"
}

@Composable
fun SettingsFeedbackScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    SettingsScaffold(
        title = "声音与震动",
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
            Text(
                "践行准则时「敲一下」的反馈。轻点即记一条，不影响绿/红圆点。",
                modifier = Modifier.padding(bottom = 12.dp),
            )
            SettingsGroupHeader("敲一下")
            RowSwitch(
                label = "木鱼声",
                checked = uiState.muyuSoundEnabled,
                onCheckedChange = viewModel::onMuyuSoundEnabledChange,
            )
            Text(
                "轻点「敲一下」时播放木鱼声，可选音色。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            MuyuSoundPresetSelector(
                selected = uiState.muyuSoundPreset,
                onSelect = viewModel::onMuyuSoundPresetChange,
            )
            TextButton(
                onClick = viewModel::previewMuyuSound,
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 8.dp)
                    .testTag(SettingsFeedbackTestTags.PREVIEW_MUYU_SOUND),
            ) {
                Text("试听")
            }
            RowSwitch(
                label = "震动",
                checked = uiState.muyuVibrationEnabled,
                onCheckedChange = viewModel::onMuyuVibrationEnabledChange,
            )
            Text(
                "轻点「敲一下」时短震一下，模仿敲木鱼。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
