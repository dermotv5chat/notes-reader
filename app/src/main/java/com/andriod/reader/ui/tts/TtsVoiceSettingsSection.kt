package com.andriod.reader.ui.tts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andriod.reader.domain.TtsSpeechBackend
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.domain.TtsVoicePreference
import com.andriod.reader.service.synthesis.SherpaDownloadPhase
import com.andriod.reader.service.synthesis.SherpaModelPack

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
    sherpaModelPacks: List<SherpaModelPack> = emptyList(),
    selectedSherpaPackId: String? = null,
    installedSherpaPackIds: Set<String> = emptySet(),
    selectedSherpaSpeakerId: Int = 0,
    sherpaPackPickerExpanded: Boolean = false,
    onSherpaPackPickerExpandedChange: (Boolean) -> Unit = {},
    onSherpaPackSelected: (String) -> Unit = {},
    onSherpaSpeakerSelected: (Int) -> Unit = {},
    isDownloadingSherpaModel: Boolean = false,
    downloadingSherpaPackId: String? = null,
    sherpaDownloadHint: String? = null,
    sherpaDownloadProgress: Float? = null,
    sherpaDownloadPhase: SherpaDownloadPhase = SherpaDownloadPhase.Idle,
    sherpaDownloadBytesLabel: String? = null,
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
                selected = speechBackend == TtsSpeechBackend.OFFLINE_SHERPA,
                onClick = { onSpeechBackendChange(TtsSpeechBackend.OFFLINE_SHERPA) },
                label = { Text("离线高质量") },
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
                TtsSpeechBackend.ONLINE_EDGE -> "Microsoft Edge neural，需联网；整篇预合成后播放更连贯。"
                TtsSpeechBackend.OFFLINE_SHERPA -> "Sherpa-onnx 离线 neural；整篇预合成，无需联网。"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }

    if (speechBackend == TtsSpeechBackend.OFFLINE_SHERPA && sherpaModelPacks.isNotEmpty()) {
        Text("离线语音包", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
        val selectedPack = sherpaModelPacks.find { it.id == selectedSherpaPackId }
            ?: sherpaModelPacks.first()
        val selectedInstalled = selectedSherpaPackId in installedSherpaPackIds
        ExposedDropdownMenuBox(
            expanded = sherpaPackPickerExpanded,
            onExpandedChange = onSherpaPackPickerExpandedChange,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        ) {
            OutlinedTextField(
                value = selectedPack.dropdownLabel(selectedInstalled),
                onValueChange = {},
                readOnly = true,
                label = { Text("语音包") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sherpaPackPickerExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            androidx.compose.material3.DropdownMenu(
                expanded = sherpaPackPickerExpanded,
                onDismissRequest = { onSherpaPackPickerExpandedChange(false) },
            ) {
                sherpaModelPacks.forEach { pack ->
                    val installed = pack.id in installedSherpaPackIds
                    DropdownMenuItem(
                        text = { Text(pack.dropdownLabel(installed)) },
                        onClick = {
                            onSherpaPackPickerExpandedChange(false)
                            onSherpaPackSelected(pack.id)
                        },
                    )
                }
            }
        }

        if (selectedPack.speakerCount > 1) {
            Text("说话人", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                selectedPack.speakerLabels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedSherpaSpeakerId == index,
                        onClick = { onSherpaSpeakerSelected(index) },
                        enabled = selectedInstalled && !isDownloadingSherpaModel,
                        label = { Text(label) },
                    )
                }
            }
        } else {
            Text(
                "当前模型为单音色（${selectedPack.genderLabel ?: selectedPack.speakerLabels.firstOrNull() ?: "默认"}）",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        if (isDownloadingSherpaModel) {
            when {
                sherpaDownloadProgress != null -> {
                    LinearProgressIndicator(
                        progress = { sherpaDownloadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }
                sherpaDownloadPhase == SherpaDownloadPhase.Extracting ||
                    sherpaDownloadPhase == SherpaDownloadPhase.Downloading -> {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }
            }
            sherpaDownloadHint?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            sherpaDownloadBytesLabel?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        } else {
            sherpaDownloadHint?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (!selectedInstalled) {
                Text(
                    "选中未下载的语音包后将自动开始下载。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
        }
        downloadingSherpaPackId?.let { packId ->
            val downloadingPack = sherpaModelPacks.find { it.id == packId }
            if (isDownloadingSherpaModel && downloadingPack != null) {
                Text(
                    "正在下载：${downloadingPack.displayName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
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
            androidx.compose.material3.DropdownMenu(
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
