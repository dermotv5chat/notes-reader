package com.andriod.reader.ui.reader

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andriod.reader.domain.TtsVoicePreference

@Composable
fun ReaderPlaybackBar(
    uiState: ReaderUiState,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onNextSegment: () -> Unit,
    onToggleLoop: () -> Unit,
    onOpenTtsSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        when {
            uiState.isTtsInitializing -> {
                Text("正在初始化语音引擎…", modifier = Modifier.padding(bottom = 4.dp))
            }
            uiState.ttsError != null -> {
                Text(
                    uiState.ttsError,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            uiState.backgroundNoteTitle != null -> {
                Text(
                    "正在朗读「${uiState.backgroundNoteTitle}」",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                if (uiState.segmentTotal > 0) {
                    Text(
                        "段落 ${uiState.segmentIndex + 1} / ${uiState.segmentTotal}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
            uiState.segmentTotal > 0 -> {
                Text(
                    "段落 ${uiState.segmentIndex + 1} / ${uiState.segmentTotal}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onTogglePlayPause,
                enabled = !uiState.isTtsInitializing,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "播放/暂停",
                    modifier = Modifier.size(32.dp),
                )
            }
            IconButton(onClick = onStop, enabled = !uiState.isTtsInitializing) {
                Icon(Icons.Default.Stop, contentDescription = "停止")
            }
            IconButton(onClick = onNextSegment, enabled = uiState.isTtsReady) {
                Icon(Icons.Default.SkipNext, contentDescription = "下一段")
            }
            IconButton(onClick = onToggleLoop, enabled = uiState.isTtsReady) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = "循环播放",
                    tint = if (uiState.loopEnabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            IconButton(onClick = onOpenTtsSettings, enabled = uiState.isTtsReady) {
                Icon(Icons.Default.Tune, contentDescription = "朗读设置")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTtsSettingsSheet(
    uiState: ReaderUiState,
    onDismiss: () -> Unit,
    onVoicePreferenceChange: (TtsVoicePreference) -> Unit,
    onVoicePickerExpandedChange: (Boolean) -> Unit,
    onVoiceSelected: (String) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onSpeechPitchChange: (Float) -> Unit,
) {
    if (!uiState.ttsSettingsVisible) return

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                "朗读设置",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            if (uiState.voiceOptions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.voicePreference == TtsVoicePreference.AUTO,
                        onClick = { onVoicePreferenceChange(TtsVoicePreference.AUTO) },
                        label = { Text("自动") },
                    )
                    FilterChip(
                        selected = uiState.voicePreference == TtsVoicePreference.PREFER_LOCAL,
                        onClick = { onVoicePreferenceChange(TtsVoicePreference.PREFER_LOCAL) },
                        label = { Text("离线") },
                    )
                    FilterChip(
                        selected = uiState.voicePreference == TtsVoicePreference.PREFER_ONLINE,
                        onClick = { onVoicePreferenceChange(TtsVoicePreference.PREFER_ONLINE) },
                        label = { Text("在线") },
                    )
                }

                val selectedLabel = uiState.voiceOptions
                    .find { it.id == uiState.selectedVoiceId }
                    ?.label ?: "选择朗读语音"
                ExposedDropdownMenuBox(
                    expanded = uiState.voicePickerExpanded,
                    onExpandedChange = onVoicePickerExpandedChange,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                ) {
                    OutlinedTextField(
                        value = selectedLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("朗读语音") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(uiState.voicePickerExpanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = uiState.voicePickerExpanded,
                        onDismissRequest = { onVoicePickerExpandedChange(false) },
                    ) {
                        uiState.voiceOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = { onVoiceSelected(option.id) },
                            )
                        }
                    }
                }
            }

            Text("语速 ${"%.1f".format(uiState.speechRate)}x")
            Slider(
                value = uiState.speechRate,
                onValueChange = onSpeechRateChange,
                valueRange = 0.5f..2.0f,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text("音调 ${"%.1f".format(uiState.speechPitch)}")
            Slider(
                value = uiState.speechPitch,
                onValueChange = onSpeechPitchChange,
                valueRange = 0.8f..1.2f,
            )
        }
    }
}
