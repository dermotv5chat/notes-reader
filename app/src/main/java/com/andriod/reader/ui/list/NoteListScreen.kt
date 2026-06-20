package com.andriod.reader.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.andriod.reader.domain.ConflictAction
import com.andriod.reader.domain.SyncStatus
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteListScreen(
    onOpenNote: (String) -> Unit,
    onEditNote: (String) -> Unit,
    onCreateNote: () -> Unit,
    viewModel: NoteListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm") }
    val lifecycleOwner = LocalLifecycleOwner.current
    val searchMode = NoteTreeBrowser.isSearchMode(uiState.query)
    val canNavigateUp = !searchMode && !uiState.showTrash && uiState.currentFolder.isNotEmpty()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(uiState.undoTrashEvent) {
        uiState.undoTrashEvent ?: return@LaunchedEffect
        val result = snackbar.showSnackbar(
            message = "已移入回收站",
            actionLabel = "撤销",
            duration = SnackbarDuration.Long,
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoLastDelete()
        } else {
            viewModel.clearUndoTrash()
        }
    }

    uiState.conflict?.let { conflict ->
        AlertDialog(
            onDismissRequest = { viewModel.resolveConflict(ConflictAction.KeepLocal) },
            title = { Text("同步冲突") },
            text = { Text("「${conflict.fileName}」本地与远程都有修改，如何处理？") },
            confirmButton = {
                TextButton(onClick = { viewModel.resolveConflict(ConflictAction.KeepRemote) }) {
                    Text("保留远程")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.resolveConflict(ConflictAction.KeepLocal) }) {
                        Text("保留本地")
                    }
                    TextButton(
                        onClick = {
                            val base = conflict.fileName.substringAfterLast('/')
                            val dir = conflict.fileName.substringBeforeLast('/', "")
                            val copyName = if (dir.isEmpty()) "copy-$base" else "$dir/copy-$base"
                            viewModel.resolveConflict(ConflictAction.SaveCopy(copyName))
                        },
                    ) {
                        Text("另存副本")
                    }
                }
            },
        )
    }

    uiState.deleteConfirm?.let { pending ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("删除笔记？") },
            text = {
                Column {
                    Text("「${pending.title}」")
                    Text(
                        pending.fileName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    if (pending.wasSynced) {
                        Text(
                            "该笔记已同步，上传后将从 GitHub 删除。",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text("取消")
                }
            },
        )
    }

    uiState.permanentDeleteConfirm?.let { entry ->
        AlertDialog(
            onDismissRequest = viewModel::cancelPermanentDelete,
            title = { Text("永久删除？") },
            text = {
                Column {
                    Text("「${entry.title}」将无法恢复。")
                    if (entry.wasSynced) {
                        Text(
                            "上传后将从 GitHub 删除远程副本。",
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmPermanentDelete) {
                    Text("永久删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelPermanentDelete) {
                    Text("取消")
                }
            },
        )
    }

    uiState.nameDialog?.let { dialog ->
        NameInputDialog(
            dialog = dialog,
            onDismiss = viewModel::cancelNameDialog,
            onConfirm = viewModel::confirmNameDialog,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            uiState.showTrash -> "回收站"
                            searchMode -> "搜索「${uiState.query}」"
                            else -> NoteTreeBrowser.displayFolderTitle(uiState.currentFolder)
                        },
                    )
                },
                navigationIcon = {
                    when {
                        uiState.showTrash -> {
                            IconButton(onClick = viewModel::closeTrash) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                            }
                        }
                        canNavigateUp -> {
                            IconButton(onClick = viewModel::navigateUp) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上级")
                            }
                        }
                    }
                },
                actions = {
                    if (!uiState.showTrash && !searchMode) {
                        IconButton(onClick = viewModel::requestCreateFolder) {
                            Icon(Icons.Default.CreateNewFolder, contentDescription = "新建文件夹")
                        }
                        IconButton(onClick = viewModel::openTrash) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "回收站")
                        }
                    }
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.padding(12.dp))
                    } else {
                        IconButton(onClick = viewModel::download) {
                            Icon(Icons.Default.Download, contentDescription = "下载")
                        }
                        IconButton(onClick = viewModel::upload) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "上传")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (!uiState.showTrash) {
                FloatingActionButton(onClick = onCreateNote) {
                    Icon(Icons.Default.Add, contentDescription = "新建")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (!uiState.showTrash) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    placeholder = { Text("搜索笔记") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                )
            }

            if (uiState.showTrash) {
                TrashList(
                    entries = uiState.trashEntries,
                    formatter = formatter,
                    onRestore = viewModel::restoreTrash,
                    onPermanentDelete = viewModel::requestPermanentDelete,
                )
            } else if (searchMode) {
                if (uiState.notes.isEmpty()) {
                    EmptyNotesHint(searchMode = true)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(uiState.notes, key = { it.fileName }) { note ->
                            SwipeNoteRow(
                                isExpanded = uiState.expandedRowKey == note.fileName,
                                onExpandedChange = { expanded ->
                                    if (expanded) {
                                        viewModel.onRowExpanded(note.fileName)
                                    } else if (uiState.expandedRowKey == note.fileName) {
                                        viewModel.clearExpandedRow()
                                    }
                                },
                                onOpen = { onOpenNote(note.fileName) },
                                onEdit = { onEditNote(note.fileName) },
                                onRename = { viewModel.requestRenameNote(note.fileName) },
                                onDelete = { viewModel.requestDelete(note.fileName) },
                            ) {
                                NoteRowContent(
                                    title = note.title,
                                    subtitle = note.fileName,
                                    updatedAt = formatter.format(note.updatedAt.atZone(ZoneId.systemDefault())),
                                    syncStatus = note.syncStatus,
                                )
                            }
                        }
                    }
                }
            } else {
                val entries = NoteTreeBrowser.listAt(
                    uiState.notes,
                    uiState.currentFolder,
                    uiState.virtualFolders,
                )
                if (entries.isEmpty()) {
                    EmptyNotesHint(searchMode = false)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(
                            items = entries,
                            key = { entry ->
                                when (entry) {
                                    is NoteTreeBrowser.Entry.Folder -> SwipeRowKeys.folder(entry.path)
                                    is NoteTreeBrowser.Entry.File -> entry.note.fileName
                                }
                            },
                        ) { entry ->
                            when (entry) {
                                is NoteTreeBrowser.Entry.Folder -> {
                                    val folderKey = SwipeRowKeys.folder(entry.path)
                                    SwipeFolderRow(
                                        isExpanded = uiState.expandedRowKey == folderKey,
                                        onExpandedChange = { expanded ->
                                            if (expanded) {
                                                viewModel.onRowExpanded(folderKey)
                                            } else if (uiState.expandedRowKey == folderKey) {
                                                viewModel.clearExpandedRow()
                                            }
                                        },
                                        onOpen = { viewModel.openFolder(entry.path) },
                                        onRename = { viewModel.requestRenameFolder(entry.path) },
                                    ) {
                                        FolderRowContent(name = entry.name)
                                    }
                                }
                                is NoteTreeBrowser.Entry.File -> SwipeNoteRow(
                                    isExpanded = uiState.expandedRowKey == entry.note.fileName,
                                    onExpandedChange = { expanded ->
                                        if (expanded) {
                                            viewModel.onRowExpanded(entry.note.fileName)
                                        } else if (uiState.expandedRowKey == entry.note.fileName) {
                                            viewModel.clearExpandedRow()
                                        }
                                    },
                                    onOpen = { onOpenNote(entry.note.fileName) },
                                    onEdit = { onEditNote(entry.note.fileName) },
                                    onRename = { viewModel.requestRenameNote(entry.note.fileName) },
                                    onDelete = { viewModel.requestDelete(entry.note.fileName) },
                                ) {
                                    NoteRowContent(
                                        title = entry.note.title,
                                        subtitle = null,
                                        updatedAt = formatter.format(
                                            entry.note.updatedAt.atZone(ZoneId.systemDefault()),
                                        ),
                                        syncStatus = entry.note.syncStatus,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NameInputDialog(
    dialog: NameDialogState,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val initial = when (dialog) {
        is NameDialogState.RenameNote -> dialog.currentName
        is NameDialogState.RenameFolder -> dialog.currentName
        is NameDialogState.CreateFolder -> ""
    }
    var text by remember(dialog) { mutableStateOf(initial) }

    val title = when (dialog) {
        is NameDialogState.RenameNote -> "重命名笔记"
        is NameDialogState.RenameFolder -> "重命名文件夹"
        is NameDialogState.CreateFolder -> "新建文件夹"
    }
    val label = when (dialog) {
        is NameDialogState.RenameNote -> "文件名（不含 .md）"
        is NameDialogState.RenameFolder -> "文件夹名称"
        is NameDialogState.CreateFolder -> "文件夹名称"
    }
    val hint = when (dialog) {
        is NameDialogState.RenameFolder ->
            if (dialog.childNoteCount > 0) {
                "将同时更新其下 ${dialog.childNoteCount} 篇笔记的路径。"
            } else {
                "空文件夹仅更新名称。"
            }
        is NameDialogState.CreateFolder -> "将在当前目录下创建。"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                hint?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) {
                Text(
                    when (dialog) {
                        is NameDialogState.CreateFolder -> "创建"
                        else -> "确定"
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun TrashList(
    entries: List<TrashEntryUi>,
    formatter: DateTimeFormatter,
    onRestore: (String) -> Unit,
    onPermanentDelete: (TrashEntryUi) -> Unit,
) {
    if (entries.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("回收站是空的")
        }
        return
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items(entries, key = { it.id }) { entry ->
            TrashRow(
                entry = entry,
                deletedAt = formatter.format(entry.deletedAt.atZone(ZoneId.systemDefault())),
                onRestore = { onRestore(entry.id) },
                onPermanentDelete = { onPermanentDelete(entry) },
            )
        }
    }
}

@Composable
private fun TrashRow(
    entry: TrashEntryUi,
    deletedAt: String,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.title, style = MaterialTheme.typography.titleMedium)
            Text(
                entry.originalPath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
            Text(deletedAt, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onRestore) {
            Icon(Icons.Default.Restore, contentDescription = "恢复")
        }
        IconButton(onClick = onPermanentDelete) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "永久删除",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun EmptyNotesHint(searchMode: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (searchMode) "没有匹配的笔记" else "这个文件夹还没有笔记，点右下角新建",
        )
    }
}

@Composable
private fun RowScope.FolderRowContent(name: String) {
    Icon(
        Icons.Default.Folder,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.secondary,
    )
    Text(
        text = name,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 12.dp),
    )
}

@Composable
private fun RowScope.NoteRowContent(
    title: String,
    subtitle: String?,
    updatedAt: String,
    syncStatus: SyncStatus,
) {
    val icon = when (syncStatus) {
        SyncStatus.SYNCED -> Icons.Default.Cloud
        SyncStatus.PENDING -> Icons.Default.CloudUpload
        SyncStatus.LOCAL_ONLY -> Icons.Default.CloudOff
    }
    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        }
        Text(updatedAt, style = MaterialTheme.typography.bodySmall)
    }
}
