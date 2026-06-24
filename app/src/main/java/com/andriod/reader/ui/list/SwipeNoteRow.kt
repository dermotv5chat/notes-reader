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
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val actionCount = if (onAddToQueue != null) 4 else 3
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
