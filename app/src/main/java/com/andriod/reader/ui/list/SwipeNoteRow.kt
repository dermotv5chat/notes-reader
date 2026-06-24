package com.andriod.reader.ui.list

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andriod.reader.domain.TtsPresynthUiState
import com.andriod.reader.ui.tts.PresynthActionIcon

private val SwipeActionButtonWidth = 52.dp

@Composable
fun SwipeNoteRow(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    onPresynth: (() -> Unit)? = null,
    presynthState: TtsPresynthUiState = TtsPresynthUiState.Hidden,
    presynthProgressFraction: Float? = null,
    presynthEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val presynthVisible = onPresynth != null && presynthState != TtsPresynthUiState.Hidden
    val actionCount = (if (onAddToQueue != null) 1 else 0) +
        (if (presynthVisible) 1 else 0) + 3
    val actionWidth = SwipeActionButtonWidth * actionCount
    SwipeRevealRow(
        isExpanded = isExpanded,
        onExpandedChange = onExpandedChange,
        actionWidth = actionWidth,
        onOpen = onOpen,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
        backgroundActions = {
            if (onAddToQueue != null) {
                IconButton(
                    onClick = {
                        onExpandedChange(false)
                        onAddToQueue()
                    },
                ) {
                    Icon(Icons.Default.PlaylistAdd, contentDescription = "加入播放列表")
                }
            }
            if (presynthVisible) {
                PresynthActionIcon(
                    state = presynthState,
                    progressFraction = presynthProgressFraction,
                    enabled = presynthEnabled,
                    onClick = {
                        onExpandedChange(false)
                        onPresynth()
                    },
                    modifier = Modifier,
                    contentDescription = "预生成语音",
                )
            }
            IconButton(
                onClick = {
                    onExpandedChange(false)
                    onEdit()
                },
            ) {
                Icon(Icons.Default.Edit, contentDescription = "编辑")
            }
            IconButton(
                onClick = {
                    onExpandedChange(false)
                    onRename()
                },
            ) {
                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "重命名")
            }
            IconButton(
                onClick = {
                    onExpandedChange(false)
                    onDelete()
                },
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
        content = content,
    )
}

@Composable
fun SwipeFolderRow(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    SwipeRevealRow(
        isExpanded = isExpanded,
        onExpandedChange = onExpandedChange,
        actionWidth = SwipeActionButtonWidth,
        onOpen = onOpen,
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 14.dp),
        backgroundActions = {
            IconButton(
                onClick = {
                    onExpandedChange(false)
                    onRename()
                },
            ) {
                Icon(Icons.Default.DriveFileRenameOutline, contentDescription = "重命名")
            }
        },
        content = content,
    )
}
