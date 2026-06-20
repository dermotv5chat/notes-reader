package com.andriod.reader.ui.reader

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(context) {
        viewModel.initTts(context)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.detachTtsUi() }
    }

    DisposableEffect(uiState.keepScreenOn) {
        if (uiState.keepScreenOn && activity != null) {
            activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    ReaderTtsSettingsSheet(
        uiState = uiState,
        onDismiss = viewModel::closeTtsSettings,
        onVoicePreferenceChange = viewModel::onVoicePreferenceChange,
        onVoicePickerExpandedChange = viewModel::onVoicePickerExpandedChange,
        onVoiceSelected = viewModel::onVoiceSelected,
        onSpeechRateChange = viewModel::onSpeechRateChange,
        onSpeechPitchChange = viewModel::onSpeechPitchChange,
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.note?.title ?: "阅读") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            ReaderPlaybackBar(
                uiState = uiState,
                onTogglePlayPause = viewModel::togglePlayPause,
                onStop = viewModel::stop,
                onNextSegment = viewModel::nextSegment,
                onToggleLoop = viewModel::toggleLoop,
                onOpenTtsSettings = viewModel::openTtsSettings,
            )
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
