package com.andriod.reader.ui.reader

import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.andriod.reader.util.NotificationPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    onBack: () -> Unit,
    onEdit: (fileName: String) -> Unit,
    viewModel: ReaderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onNotificationPermissionResult(granted)
    }

    LaunchedEffect(context) {
        viewModel.initTts(context)
        viewModel.refreshNotificationPermissionState()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNotificationPermissionState()
                viewModel.refreshNote()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.detachTtsUi()
        }
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

    ReaderSleepTimerSheet(
        uiState = uiState,
        onDismiss = viewModel::closeSleepTimer,
        onSliderFinished = viewModel::onSleepTimerSliderFinished,
        onAfterNoteEnd = viewModel::applySleepTimerAfterNoteEnd,
        onApplyLastPreset = viewModel::applyLastSleepTimerPreset,
    )

    PracticeSheet(
        sheetState = uiState.practiceSheet,
        onDismiss = viewModel::dismissPracticeSheet,
        onSave = viewModel::savePractice,
        onClear = viewModel::clearPracticeToday,
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
                actions = {
                    uiState.note?.let { note ->
                        IconButton(
                            onClick = {
                                if (viewModel.prepareForEdit()) {
                                    onEdit(note.fileName)
                                }
                            },
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "编辑")
                        }
                    }
                },
            )
        },
        bottomBar = {
            ReaderPlaybackBar(
                uiState = uiState,
                onTogglePlayPause = {
                    viewModel.onPlayPauseClicked {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(NotificationPermission.permission)
                        }
                    }
                },
                onStop = viewModel::stop,
                onNextSegment = viewModel::nextSegment,
                onToggleLoop = viewModel::toggleLoop,
                onOpenTtsSettings = viewModel::openTtsSettings,
                onOpenSleepTimer = viewModel::openSleepTimer,
                onOpenNotificationSettings = viewModel::openNotificationSettings,
            )
        },
    ) { padding ->
        if (uiState.note == null) {
            Text(
                text = "笔记不存在",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
            )
        } else {
            BlockReaderContent(
                blocks = uiState.blocks,
                todayPractice = uiState.todayPractice,
                onTrackableBlockClick = viewModel::onTrackableBlockClick,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            )
        }
    }
}
