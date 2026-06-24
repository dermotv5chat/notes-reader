package com.andriod.reader.ui.reader

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andriod.reader.ui.tts.TtsVoiceSettingsSection
import com.andriod.reader.domain.TtsQueueRepeatMode
import com.andriod.reader.service.SleepTimerDisplay
import com.andriod.reader.service.SleepTimerMode

@Composable
fun ReaderPlaybackBar(
    uiState: ReaderUiState,
    onTogglePlayPause: () -> Unit,
    onStop: () -> Unit,
    onNextSegment: () -> Unit,
    onCycleRepeatMode: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenTtsSettings: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenNotificationSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        when {
            uiState.isTtsInitializing -> {
                Text("正在初始化语音引擎…", modifier = Modifier.padding(bottom = 4.dp))
            }
            uiState.notificationPermissionDenied -> {
                Text(
                    "需要通知权限才能在后台显示朗读控制",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                TextButton(
                    onClick = onOpenNotificationSettings,
                    modifier = Modifier.padding(bottom = 4.dp),
                ) {
                    Text("去设置")
                }
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
        sleepTimerStatusLabel(uiState)?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
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
            IconButton(
                onClick = onCycleRepeatMode,
                enabled = uiState.isTtsReady,
            ) {
                Icon(
                    Icons.Default.Repeat,
                    contentDescription = repeatModeContentDescription(uiState.queueRepeatMode),
                    tint = when (uiState.queueRepeatMode) {
                        TtsQueueRepeatMode.OFF -> MaterialTheme.colorScheme.onSurfaceVariant
                        TtsQueueRepeatMode.REPEAT_ONE,
                        TtsQueueRepeatMode.REPEAT_ALL,
                        -> MaterialTheme.colorScheme.primary
                    },
                )
            }
            IconButton(onClick = onOpenQueue) {
                Icon(Icons.Default.List, contentDescription = "播放列表")
            }
            if (uiState.queueCount > 0) {
                Text(
                    text = "待播 ${uiState.queueCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenSleepTimer, enabled = uiState.isTtsReady) {
                Icon(
                    Icons.Default.Timer,
                    contentDescription = "定时关闭",
                    tint = if (uiState.sleepTimerMode != SleepTimerMode.Off) {
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
fun ReaderSleepTimerSheet(
    uiState: ReaderUiState,
    onDismiss: () -> Unit,
    onSliderFinished: (Float) -> Unit,
    onAfterNoteEnd: () -> Unit,
    onApplyLastPreset: () -> Unit,
) {
    if (!uiState.sleepTimerVisible) return

    ModalBottomSheet(onDismissRequest = onDismiss) {
        ReaderSleepTimerSheetContent(
            uiState = uiState,
            onSliderFinished = onSliderFinished,
            onAfterNoteEnd = onAfterNoteEnd,
            onApplyLastPreset = onApplyLastPreset,
        )
    }
}

@Composable
internal fun ReaderSleepTimerSheetContent(
    uiState: ReaderUiState,
    onSliderFinished: (Float) -> Unit,
    onAfterNoteEnd: () -> Unit,
    onApplyLastPreset: () -> Unit,
) {
    // Keep slider state local while dragging; only commit to ViewModel on release.
    var sliderMinutes by remember { mutableFloatStateOf(uiState.sleepTimerSliderMinutes) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    // Subtitle for "上次定时时间" is fixed for this sheet session; must not follow the slider.
    var lastPresetSubtitle by remember { mutableStateOf(uiState.lastSleepTimerPresetSubtitle) }
    LaunchedEffect(uiState.sleepTimerVisible) {
        if (uiState.sleepTimerVisible) {
            lastPresetSubtitle = uiState.lastSleepTimerPresetSubtitle
            sliderMinutes = uiState.sleepTimerSliderMinutes
        }
    }
    LaunchedEffect(uiState.sleepTimerSliderMinutes) {
        if (uiState.sleepTimerVisible && !isDraggingSlider) {
            sliderMinutes = uiState.sleepTimerSliderMinutes
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
            Text(
                "定时关闭",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 20.dp),
            )
            sleepTimerSheetStatusLabel(uiState)?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center,
                )
            }
            SleepTimerSlider(
                value = sliderMinutes,
                onValueChange = {
                    isDraggingSlider = true
                    sliderMinutes = it
                },
                onValueChangeFinished = {
                    isDraggingSlider = false
                    onSliderFinished(sliderMinutes)
                },
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("关", style = MaterialTheme.typography.bodySmall)
                Text("30", style = MaterialTheme.typography.bodySmall)
                Text("60", style = MaterialTheme.typography.bodySmall)
                Text("90", style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = if (sliderMinutes <= 0f) {
                    "关"
                } else {
                    "${sliderMinutes.toInt()} 分钟"
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(SleepTimerSheetTestTags.SLIDER_VALUE_LABEL)
                    .padding(bottom = 20.dp),
                textAlign = TextAlign.Center,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onAfterNoteEnd,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.canScheduleAfterNoteEnd,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("本篇结束后关闭")
                        if (uiState.estimatedNoteRemainingMinutes > 0) {
                            Text(
                                "约 ${uiState.estimatedNoteRemainingMinutes} 分钟",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onApplyLastPreset,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("上次定时时间")
                        Text(
                            lastPresetSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag(SleepTimerSheetTestTags.LAST_PRESET_SUBTITLE),
                        )
                    }
                }
            }
    }
}

@Composable
private fun SleepTimerSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .padding(horizontal = 12.dp),
        ) {
            val y = size.height / 2f
            drawLine(
                color = trackColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()), 0f),
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..90f,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(SleepTimerSheetTestTags.SLIDER),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTtsSettingsSheet(
    uiState: ReaderUiState,
    onDismiss: () -> Unit,
    onSpeechBackendChange: (com.andriod.reader.domain.TtsSpeechBackend) -> Unit,
    onVoicePreferenceChange: (com.andriod.reader.domain.TtsVoicePreference) -> Unit,
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

            TtsVoiceSettingsSection(
                voiceOptions = uiState.voiceOptions,
                selectedVoiceId = uiState.selectedVoiceId,
                voicePreference = uiState.voicePreference,
                speechBackend = uiState.speechBackend,
                qualityGuide = uiState.ttsQualityHint,
                onVoicePreferenceChange = onVoicePreferenceChange,
                onVoicePickerExpandedChange = onVoicePickerExpandedChange,
                onVoiceSelected = onVoiceSelected,
                onSpeechBackendChange = onSpeechBackendChange,
                voicePickerExpanded = uiState.voicePickerExpanded,
            )

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

private fun repeatModeContentDescription(mode: TtsQueueRepeatMode): String = when (mode) {
    TtsQueueRepeatMode.OFF -> "循环：关闭"
    TtsQueueRepeatMode.REPEAT_ONE -> "循环：单曲"
    TtsQueueRepeatMode.REPEAT_ALL -> "循环：列表"
}

private fun sleepTimerStatusLabel(uiState: ReaderUiState): String? = when (uiState.sleepTimerMode) {
    SleepTimerMode.Off -> null
    SleepTimerMode.FixedMinutes -> uiState.sleepTimerRemainingMs?.let {
        SleepTimerDisplay.fixedCountdownLabel(it)
    }
    SleepTimerMode.AfterNoteEnd ->
        SleepTimerDisplay.afterNoteEndStatusLabel(uiState.estimatedNoteRemainingMinutes)
}

private fun sleepTimerSheetStatusLabel(uiState: ReaderUiState): String? = when (uiState.sleepTimerMode) {
    SleepTimerMode.Off -> null
    SleepTimerMode.FixedMinutes -> uiState.sleepTimerRemainingMs?.let {
        SleepTimerDisplay.fixedCountdownSheetStatus(it)
    }
    SleepTimerMode.AfterNoteEnd ->
        SleepTimerDisplay.afterNoteEndStatusLabel(uiState.estimatedNoteRemainingMinutes)
}
