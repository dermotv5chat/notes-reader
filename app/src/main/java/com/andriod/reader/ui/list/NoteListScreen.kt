package com.andriod.reader.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val canNavigateUp = !searchMode && uiState.currentFolder.isNotEmpty()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when {
                            searchMode -> "搜索「${uiState.query}」"
                            else -> NoteTreeBrowser.displayFolderTitle(uiState.currentFolder)
                        },
                    )
                },
                navigationIcon = {
                    if (canNavigateUp) {
                        IconButton(onClick = viewModel::navigateUp) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回上级")
                        }
                    }
                },
                actions = {
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
            FloatingActionButton(onClick = onCreateNote) {
                Icon(Icons.Default.Add, contentDescription = "新建")
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

            if (uiState.notes.isEmpty()) {
                EmptyNotesHint(searchMode = searchMode)
            } else if (searchMode) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(uiState.notes, key = { it.fileName }) { note ->
                        NoteRow(
                            title = note.title,
                            subtitle = note.fileName,
                            updatedAt = formatter.format(note.updatedAt.atZone(ZoneId.systemDefault())),
                            syncStatus = note.syncStatus,
                            onOpen = { onOpenNote(note.fileName) },
                            onEdit = { onEditNote(note.fileName) },
                            onDelete = { viewModel.deleteNote(note.fileName) },
                        )
                    }
                }
            } else {
                val entries = NoteTreeBrowser.listAt(uiState.notes, uiState.currentFolder)
                if (entries.isEmpty()) {
                    EmptyNotesHint(searchMode = false)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(
                            items = entries,
                            key = { entry ->
                                when (entry) {
                                    is NoteTreeBrowser.Entry.Folder -> "folder:${entry.path}"
                                    is NoteTreeBrowser.Entry.File -> entry.note.fileName
                                }
                            },
                        ) { entry ->
                            when (entry) {
                                is NoteTreeBrowser.Entry.Folder -> FolderRow(
                                    name = entry.name,
                                    onOpen = { viewModel.openFolder(entry.path) },
                                )
                                is NoteTreeBrowser.Entry.File -> NoteRow(
                                    title = entry.note.title,
                                    subtitle = null,
                                    updatedAt = formatter.format(
                                        entry.note.updatedAt.atZone(ZoneId.systemDefault()),
                                    ),
                                    syncStatus = entry.note.syncStatus,
                                    onOpen = { onOpenNote(entry.note.fileName) },
                                    onEdit = { onEditNote(entry.note.fileName) },
                                    onDelete = { viewModel.deleteNote(entry.note.fileName) },
                                )
                            }
                        }
                    }
                }
            }
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
private fun FolderRow(
    name: String,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
}

@Composable
private fun NoteRow(
    title: String,
    subtitle: String?,
    updatedAt: String,
    syncStatus: SyncStatus,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
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
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "编辑")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除")
        }
    }
}
