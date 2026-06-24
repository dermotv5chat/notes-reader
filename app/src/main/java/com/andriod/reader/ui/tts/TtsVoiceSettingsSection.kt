package com.andriod.reader.ui.tts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andriod.reader.domain.TtsSpeechBackend
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.domain.TtsVoicePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsVoiceSettingsSection(
    voiceOptions: List<TtsVoiceOption>,
    selectedVoiceId: String?,
    voicePreference: TtsVoicePreference,
    speechBackend: TtsSpeechBackend,
    qualityGuide: String?,
    onVoicePreferenceChange: (TtsVoicePreference) -> Unit,
    onVoicePickerExpandedChange: (Boolean) -> Unit,
    onVoiceSelected: (String) -> Unit,
    onSpeechBackendChange: (TtsSpeechBackend) -> Unit,
    voicePickerExpanded: Boolean,
    showBackendSelector: Boolean = true,
) {
    if (showBackendSelector) {
        Text("朗读引擎", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = speechBackend == TtsSpeechBackend.SYSTEM,
                onClick = { onSpeechBackendChange(TtsSpeechBackend.SYSTEM) },
                label = { Text("系统 TTS") },
            )
            FilterChip(
                selected = speechBackend == TtsSpeechBackend.ONLINE_EDGE,
                onClick = { onSpeechBackendChange(TtsSpeechBackend.ONLINE_EDGE) },
                label = { Text("在线高质量") },
            )
        }
        Text(
            when (speechBackend) {
                TtsSpeechBackend.SYSTEM -> "离线可用；推荐安装 Google TTS 并选 neural 在线音色。"
                TtsSpeechBackend.ONLINE_EDGE -> "Microsoft Edge neural，需联网；无网时自动回退系统 TTS。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }

    if (speechBackend == TtsSpeechBackend.SYSTEM && voiceOptions.isNotEmpty()) {
        Text("语音偏好", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = voicePreference == TtsVoicePreference.AUTO,
                onClick = { onVoicePreferenceChange(TtsVoicePreference.AUTO) },
                label = { Text("自动") },
            )
            FilterChip(
                selected = voicePreference == TtsVoicePreference.PREFER_LOCAL,
                onClick = { onVoicePreferenceChange(TtsVoicePreference.PREFER_LOCAL) },
                label = { Text("离线") },
            )
            FilterChip(
                selected = voicePreference == TtsVoicePreference.PREFER_ONLINE,
                onClick = { onVoicePreferenceChange(TtsVoicePreference.PREFER_ONLINE) },
                label = { Text("在线") },
            )
        }

        val selectedLabel = voiceOptions.find { it.id == selectedVoiceId }?.label ?: "选择朗读语音"
        ExposedDropdownMenuBox(
            expanded = voicePickerExpanded,
            onExpandedChange = onVoicePickerExpandedChange,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("朗读语音") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(voicePickerExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            DropdownMenu(
                expanded = voicePickerExpanded,
                onDismissRequest = { onVoicePickerExpandedChange(false) },
            ) {
                voiceOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = { onVoiceSelected(option.id) },
                    )
                }
            }
        }
    }

    if (speechBackend == TtsSpeechBackend.ONLINE_EDGE) {
        Text("在线音色", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            voiceOptions.forEach { option ->
                FilterChip(
                    selected = option.id == selectedVoiceId,
                    onClick = { onVoiceSelected(option.id) },
                    label = { Text(option.label.substringAfter(" · ")) },
                )
            }
        }
    }

    qualityGuide?.let { guide ->
        Text(
            guide,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}
