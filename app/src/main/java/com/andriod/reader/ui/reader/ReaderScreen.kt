package com.andriod.reader.ui.reader

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.data.repository.NoteRepository
import com.andriod.reader.service.TtsPlaybackManager
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    fileName: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val entryPoint = EntryPointAccessors.fromApplication(
        context.applicationContext,
        ReaderEntryPoint::class.java,
    )
    val note = remember(fileName) { entryPoint.noteRepository().getNote(fileName) }
    val settingsStore = remember { entryPoint.settingsStore() }
    var isPlaying by remember { mutableStateOf(false) }
    var segmentIndex by remember { mutableIntStateOf(0) }
    var segmentTotal by remember { mutableIntStateOf(0) }
    var speechRate by remember { mutableFloatStateOf(settingsStore.getDefaultSpeechRate()) }
    var speechPitch by remember { mutableFloatStateOf(settingsStore.getDefaultSpeechPitch()) }
    val activity = LocalContext.current as? Activity

    DisposableEffect(settingsStore.isKeepScreenOn()) {
        if (settingsStore.isKeepScreenOn()) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            TtsPlaybackManager.release()
        }
    }

    val controller = remember {
        TtsPlaybackManager.getOrCreate(
            context = context,
            onSegmentChanged = { index, total ->
                segmentIndex = index
                segmentTotal = total
            },
            onPlaybackStateChanged = { playing -> isPlaying = playing },
        ).also {
            it.setSpeechRate(speechRate)
            it.setPitch(speechPitch)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(note?.title ?: "阅读") },
                navigationIcon = {
                    IconButton(onClick = {
                        controller.stop()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("语速 ${"%.1f".format(speechRate)}x")
                Slider(
                    value = speechRate,
                    onValueChange = {
                        speechRate = it
                        controller.setSpeechRate(it)
                        settingsStore.saveDefaultSpeechRate(it)
                    },
                    valueRange = 0.5f..2.0f,
                )
                if (segmentTotal > 0) {
                    Text("段落 ${segmentIndex + 1} / $segmentTotal")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            if (isPlaying) controller.pause() else {
                                if (segmentTotal == 0) {
                                    note?.content?.let { controller.start(it) }
                                } else {
                                    controller.resume()
                                }
                            }
                        },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "播放/暂停",
                            modifier = Modifier.padding(4.dp),
                        )
                    }
                    IconButton(onClick = { controller.stop() }) {
                        Icon(Icons.Default.Stop, contentDescription = "停止")
                    }
                    IconButton(onClick = { controller.nextSegment() }) {
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
            Text(
                text = note?.content ?: "笔记不存在",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4f,
            )
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface ReaderEntryPoint {
    fun noteRepository(): NoteRepository
    fun settingsStore(): SettingsStore
}
