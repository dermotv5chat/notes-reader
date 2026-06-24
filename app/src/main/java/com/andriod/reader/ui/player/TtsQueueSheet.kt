package com.andriod.reader.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andriod.reader.data.local.TtsPlaylistSnapshot
import com.andriod.reader.domain.TtsPlaylistItem
import com.andriod.reader.domain.TtsQueueRepeatMode
import com.andriod.reader.service.TtsPlaybackSession
import com.andriod.reader.service.TtsPlaylistPolicy

object TtsQueueSheetTestTags {
    const val SHEET = "tts_queue_sheet"
    const val REPEAT_OFF = "tts_repeat_off"
    const val REPEAT_ONE = "tts_repeat_one"
    const val REPEAT_ALL = "tts_repeat_all"
    const val ITEM = "tts_queue_item"
    const val CLEAR_BUTTON = "tts_queue_clear"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TtsQueueSheet(
    visible: Boolean,
    snapshot: TtsPlaylistSnapshot,
    session: TtsPlaybackSession,
    onDismiss: () -> Unit,
    onPlayItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClear: () -> Unit,
    onRepeatModeChange: (TtsQueueRepeatMode) -> Unit,
) {
    if (!visible) return

    ModalBottomSheet(onDismissRequest = onDismiss) {
        TtsQueueSheetContent(
            snapshot = snapshot,
            session = session,
            onPlayItem = onPlayItem,
            onRemoveItem = onRemoveItem,
            onClear = onClear,
            onRepeatModeChange = onRepeatModeChange,
        )
    }
}

@Composable
internal fun TtsQueueSheetContent(
    snapshot: TtsPlaylistSnapshot,
    session: TtsPlaybackSession,
    onPlayItem: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClear: () -> Unit,
    onRepeatModeChange: (TtsQueueRepeatMode) -> Unit,
) {
    val canRepeatAll = TtsPlaylistPolicy.canSelectRepeatAll(snapshot.items)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
            .testTag(TtsQueueSheetTestTags.SHEET),
    ) {
        Text(
            text = "播放列表",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        if (snapshot.items.isEmpty()) {
            Text(
                text = "列表为空，从笔记列表或阅读页加入",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        } else {
            snapshot.items.forEachIndexed { index, item ->
                TtsQueueItemRow(
                    item = item,
                    isPlaying = session.fileName == item.fileName && session.hasActiveSession,
                    modifier = if (index == 0) Modifier.testTag(TtsQueueSheetTestTags.ITEM) else Modifier,
                    onPlay = { onPlayItem(item.fileName) },
                    onRemove = { onRemoveItem(item.fileName) },
                )
            }
        }
        Text(
            text = "循环模式",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = snapshot.repeatMode == TtsQueueRepeatMode.OFF,
                onClick = { onRepeatModeChange(TtsQueueRepeatMode.OFF) },
                label = { Text("关闭") },
                modifier = Modifier.testTag(TtsQueueSheetTestTags.REPEAT_OFF),
            )
            FilterChip(
                selected = snapshot.repeatMode == TtsQueueRepeatMode.REPEAT_ONE,
                onClick = { onRepeatModeChange(TtsQueueRepeatMode.REPEAT_ONE) },
                label = { Text("单曲") },
                modifier = Modifier.testTag(TtsQueueSheetTestTags.REPEAT_ONE),
            )
            FilterChip(
                selected = snapshot.repeatMode == TtsQueueRepeatMode.REPEAT_ALL,
                onClick = { onRepeatModeChange(TtsQueueRepeatMode.REPEAT_ALL) },
                label = { Text("列表") },
                enabled = canRepeatAll,
                modifier = Modifier.testTag(TtsQueueSheetTestTags.REPEAT_ALL),
            )
        }
        if (!canRepeatAll) {
            Text(
                text = "请先将多篇笔记加入播放列表",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        TextButton(
            onClick = onClear,
            enabled = snapshot.items.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.End)
                .testTag(TtsQueueSheetTestTags.CLEAR_BUTTON),
        ) {
            Text("清空列表")
        }
    }
}

@Composable
private fun TtsQueueItemRow(
    item: TtsPlaylistItem,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (isPlaying) {
                Text(
                    text = "正在播放",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        IconButton(onClick = onPlay) {
            Icon(Icons.Default.PlayArrow, contentDescription = "立即播放")
        }
        TextButton(onClick = onRemove) {
            Text("移除")
        }
    }
}
