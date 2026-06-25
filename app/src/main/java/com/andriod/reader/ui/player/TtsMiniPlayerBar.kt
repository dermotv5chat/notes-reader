package com.andriod.reader.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andriod.reader.service.TtsPlaybackProgress
import com.andriod.reader.service.TtsPlaybackSession

@Composable
fun TtsMiniPlayerBar(
    session: TtsPlaybackSession,
    queueCount: Int = 0,
    onOpenReader: () -> Unit,
    onOpenQueue: () -> Unit = {},
    onTogglePlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasTimedProgress = session.durationMs > 0
    val showIndeterminateProgress = session.presynthGenerating && !hasTimedProgress
    val showDeterminateProgress = hasTimedProgress || (
        session.segmentTotal > 0 && !session.presynthGenerating
        )
    val subtitle = buildMiniPlayerSubtitle(session)
    val displaySubtitle = session.sleepTimerLabel?.takeIf { session.sleepTimerActive }?.let {
        "$subtitle · $it"
    } ?: subtitle

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenReader)
                    .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = session.title ?: "语音朗读",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = displaySubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onOpenQueue) {
                    Icon(
                        Icons.Default.List,
                        contentDescription = if (queueCount > 0) {
                            "播放列表（$queueCount）"
                        } else {
                            "播放列表"
                        },
                    )
                }
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        if (session.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "播放/暂停",
                    )
                }
            }
            if (showIndeterminateProgress || showDeterminateProgress) {
                if (showIndeterminateProgress) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                } else {
                    MiniPlayerProgressRow(
                        positionMs = session.positionMs,
                        durationMs = session.durationMs,
                        progressFraction = session.progressFraction,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 0.dp)
                            .padding(bottom = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniPlayerProgressRow(
    positionMs: Long,
    durationMs: Long,
    progressFraction: Float,
    modifier: Modifier = Modifier,
) {
    val positionLabel = if (durationMs > 0) {
        TtsPlaybackProgress.formatPlaybackTime(positionMs)
    } else {
        "--:--"
    }
    val durationLabel = if (durationMs > 0) {
        TtsPlaybackProgress.formatPlaybackTime(durationMs)
    } else {
        "--:--"
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = positionLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LinearProgressIndicator(
            progress = { progressFraction.coerceIn(0f, 1f) },
            modifier = Modifier.weight(1f),
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Text(
            text = durationLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun buildMiniPlayerSubtitle(session: TtsPlaybackSession): String = when {
    session.presynthGenerating -> {
        val base = when {
            session.isPaused -> "已暂停"
            session.isPlaying -> "正在朗读"
            else -> "正在朗读"
        }
        "$base · 生成中"
    }
    session.isPaused -> {
        val mode = session.playbackMode.displayLabel()
        if (mode != null) "已暂停 · $mode" else "已暂停"
    }
    session.isPlaying -> {
        val mode = session.playbackMode.displayLabel()
        if (mode != null) "正在朗读 · $mode" else "正在朗读"
    }
    else -> "正在朗读"
}
