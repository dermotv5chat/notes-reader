package com.andriod.reader.ui.reader

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andriod.reader.domain.TtsVoicePreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as? Activity

    DisposableEffect(Unit) {
        onDispose { viewModel.releaseTts() }
    }

    DisposableEffect(uiState.keepScreenOn) {
        if (uiState.keepScreenOn && activity != null) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.note?.title ?: "阅读") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.stop()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (uiState.voiceOptions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = uiState.voicePreference == TtsVoicePreference.AUTO,
                            onClick = { viewModel.onVoicePreferenceChange(TtsVoicePreference.AUTO) },
                            label = { Text("自动") },
                        )
                        FilterChip(
                            selected = uiState.voicePreference == TtsVoicePreference.PREFER_LOCAL,
                            onClick = { viewModel.onVoicePreferenceChange(TtsVoicePreference.PREFER_LOCAL) },
                            label = { Text("离线") },
                        )
                        FilterChip(
                            selected = uiState.voicePreference == TtsVoicePreference.PREFER_ONLINE,
                            onClick = { viewModel.onVoicePreferenceChange(TtsVoicePreference.PREFER_ONLINE) },
                            label = { Text("在线") },
                        )
                    }

                    val selectedLabel = uiState.voiceOptions
                        .find { it.id == uiState.selectedVoiceId }
                        ?.label ?: "选择朗读语音"
                    ExposedDropdownMenuBox(
                        expanded = uiState.voicePickerExpanded,
                        onExpandedChange = viewModel::onVoicePickerExpandedChange,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        OutlinedTextField(
                            value = selectedLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("朗读语音") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(uiState.voicePickerExpanded)
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(
                            expanded = uiState.voicePickerExpanded,
                            onDismissRequest = { viewModel.onVoicePickerExpandedChange(false) },
                        ) {
                            uiState.voiceOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = { viewModel.onVoiceSelected(option.id) },
                                )
                            }
                        }
                    }
                }

                Text("语速 ${"%.1f".format(uiState.speechRate)}x")
                Slider(
                    value = uiState.speechRate,
                    onValueChange = viewModel::onSpeechRateChange,
                    valueRange = 0.5f..2.0f,
                )
                if (uiState.segmentTotal > 0) {
                    Text("段落 ${uiState.segmentIndex + 1} / ${uiState.segmentTotal}")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = viewModel::togglePlayPause,
                        modifier = Modifier.padding(8.dp),
                        enabled = uiState.isTtsReady,
                    ) {
                        Icon(
                            if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "播放/暂停",
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                    IconButton(onClick = viewModel::stop, enabled = uiState.isTtsReady) {
                        Icon(Icons.Default.Stop, contentDescription = "停止")
                    }
                    IconButton(onClick = viewModel::nextSegment, enabled = uiState.isTtsReady) {
                        Icon(Icons.Default.SkipNext, contentDescription = "下一段")
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            MarkdownContent(
                text = uiState.note?.content ?: "笔记不存在",
            )
        }
    }
}
