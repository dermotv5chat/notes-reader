package com.andriod.reader.ui.player

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andriod.reader.data.local.TtsPlaylistSnapshot
import com.andriod.reader.domain.TtsQueueRepeatMode
import com.andriod.reader.service.TtsPlaybackManager
import com.andriod.reader.service.TtsPlaybackSession
import com.andriod.reader.util.NotificationPermission

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsPlaylistScreen(
    onOpenNote: (String) -> Unit,
    viewModel: TtsPlaylistViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.snapshot.collectAsState()
    val playlistUiState by viewModel.uiState.collectAsState()
    val session by TtsPlaybackManager.session.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingPlayback by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(playlistUiState.playbackError) {
        val error = playlistUiState.playbackError ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
        viewModel.clearPlaybackError()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        pendingPlayback?.invoke()
        pendingPlayback = null
    }

    fun runPlayback(action: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationPermission.hasPermission(context)
        ) {
            pendingPlayback = action
            notificationPermissionLauncher.launch(NotificationPermission.permission)
        } else {
            action()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) {
        TtsPlaylistScreenContent(
            snapshot = snapshot,
            session = session,
            isStartingPlayback = playlistUiState.isStartingPlayback,
            onPlayAll = { runPlayback { viewModel.playFromStart() } },
            onPlayItem = { fileName -> runPlayback { viewModel.playItem(fileName) } },
            onRemoveItem = viewModel::remove,
            onClear = viewModel::clear,
            onRepeatModeChange = viewModel::setRepeatMode,
            onOpenNote = onOpenNote,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TtsPlaylistScreenContent(
    snapshot: TtsPlaylistSnapshot,
    session: TtsPlaybackSession,
    isStartingPlayback: Boolean = false,
    onPlayAll: () -> Unit,
    onPlayItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClear: () -> Unit,
    onRepeatModeChange: (TtsQueueRepeatMode) -> Unit,
    onOpenNote: (String) -> Unit,
) {
    Scaffold(
        modifier = Modifier.testTag(TtsPlaylistScreenTestTags.SCREEN),
        topBar = {
            TopAppBar(title = { Text("播放列表") })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Button(
                onClick = onPlayAll,
                enabled = snapshot.items.isNotEmpty() && !isStartingPlayback,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .testTag(TtsPlaylistScreenTestTags.PLAY_ALL_BUTTON),
            ) {
                if (isStartingPlayback) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(end = 8.dp),
                        strokeWidth = 2.dp,
                    )
                }
                Text(if (isStartingPlayback) "正在启动…" else "播放全部")
            }
            if (isStartingPlayback) {
                Text(
                    text = "正在初始化语音引擎…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                )
            }
            TtsQueueSheetContent(
                snapshot = snapshot,
                session = session,
                onPlayItem = onPlayItem,
                onRemoveItem = onRemoveItem,
                onClear = onClear,
                onRepeatModeChange = onRepeatModeChange,
                showTitle = false,
                onOpenItem = onOpenNote,
            )
        }
    }
}
