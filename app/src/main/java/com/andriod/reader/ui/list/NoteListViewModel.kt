package com.andriod.reader.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriod.reader.data.local.NotePathNames
import com.andriod.reader.data.repository.NoteRepository
import com.andriod.reader.data.repository.SyncRepository
import com.andriod.reader.domain.ConflictAction
import com.andriod.reader.domain.Note
import com.andriod.reader.domain.SyncConflict
import com.andriod.reader.domain.SyncResult
import com.andriod.reader.domain.SyncStatus
import com.andriod.reader.domain.TrashEntry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject

data class PendingDelete(
    val fileName: String,
    val title: String,
    val wasSynced: Boolean,
)

data class TrashEntryUi(
    val id: String,
    val title: String,
    val originalPath: String,
    val deletedAt: Instant,
    val wasSynced: Boolean,
)

sealed interface NameDialogState {
    data class RenameNote(
        val fileName: String,
        val currentName: String,
    ) : NameDialogState

    data class RenameFolder(
        val folderPath: String,
        val currentName: String,
        val childNoteCount: Int,
    ) : NameDialogState

    data class CreateFolder(
        val parentFolder: String,
    ) : NameDialogState
}

data class NoteListUiState(
    val notes: List<Note> = emptyList(),
    val virtualFolders: Set<String> = emptySet(),
    val currentFolder: String = "",
    val query: String = "",
    val isSyncing: Boolean = false,
    val message: String? = null,
    val conflict: SyncConflict? = null,
    val expandedRowKey: String? = null,
    val deleteConfirm: PendingDelete? = null,
    val permanentDeleteConfirm: TrashEntryUi? = null,
    val nameDialog: NameDialogState? = null,
    val showTrash: Boolean = false,
    val trashEntries: List<TrashEntryUi> = emptyList(),
    val lastTrashIdForUndo: String? = null,
    val undoTrashEvent: Long? = null,
)

@HiltViewModel
class NoteListViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val syncRepository: SyncRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(NoteListUiState())
    val uiState: StateFlow<NoteListUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update {
            it.copy(
                notes = noteRepository.listNotes(it.query),
                virtualFolders = noteRepository.listVirtualFolders(),
                trashEntries = loadTrashEntries(),
                message = null,
            )
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update {
            it.copy(
                query = query,
                notes = noteRepository.listNotes(query),
                currentFolder = if (query.isBlank()) it.currentFolder else "",
                expandedRowKey = null,
            )
        }
    }

    fun openFolder(path: String) {
        _uiState.update {
            it.copy(
                currentFolder = path,
                query = "",
                notes = noteRepository.listNotes(""),
                virtualFolders = noteRepository.listVirtualFolders(),
                expandedRowKey = null,
            )
        }
    }

    fun navigateUp() {
        _uiState.update {
            it.copy(
                currentFolder = NoteTreeBrowser.parentFolder(it.currentFolder),
                expandedRowKey = null,
            )
        }
    }

    fun onRowExpanded(rowKey: String) {
        _uiState.update { it.copy(expandedRowKey = rowKey) }
    }

    fun clearExpandedRow() {
        _uiState.update { it.copy(expandedRowKey = null) }
    }

    fun requestCreateFolder() {
        _uiState.update {
            it.copy(
                nameDialog = NameDialogState.CreateFolder(parentFolder = it.currentFolder),
                expandedRowKey = null,
            )
        }
    }

    fun requestRenameNote(fileName: String) {
        _uiState.update {
            it.copy(
                nameDialog = NameDialogState.RenameNote(
                    fileName = fileName,
                    currentName = NotePathNames.noteBaseName(fileName),
                ),
                expandedRowKey = null,
            )
        }
    }

    fun requestRenameFolder(folderPath: String) {
        _uiState.update {
            it.copy(
                nameDialog = NameDialogState.RenameFolder(
                    folderPath = folderPath,
                    currentName = folderPath.substringAfterLast('/'),
                    childNoteCount = noteRepository.countNotesUnderFolder(folderPath),
                ),
                expandedRowKey = null,
            )
        }
    }

    fun cancelNameDialog() {
        _uiState.update { it.copy(nameDialog = null) }
    }

    fun confirmNameDialog(newName: String) {
        val dialog = _uiState.value.nameDialog ?: return
        runCatching {
            when (dialog) {
                is NameDialogState.RenameNote -> {
                    noteRepository.renameNoteFile(dialog.fileName, newName)
                }
                is NameDialogState.RenameFolder -> {
                    val newPath = noteRepository.renameFolder(dialog.folderPath, newName)
                    val state = _uiState.value
                    if (state.currentFolder == dialog.folderPath) {
                        _uiState.update { it.copy(currentFolder = newPath) }
                    } else if (state.currentFolder.startsWith("${dialog.folderPath}/")) {
                        _uiState.update {
                            it.copy(
                                currentFolder = state.currentFolder.replaceFirst(
                                    dialog.folderPath,
                                    newPath,
                                ),
                            )
                        }
                    }
                }
                is NameDialogState.CreateFolder -> {
                    val path = noteRepository.createFolder(dialog.parentFolder, newName)
                    openFolder(path)
                }
            }
        }.onSuccess {
            refresh()
            _uiState.update {
                it.copy(
                    nameDialog = null,
                    message = when (dialog) {
                        is NameDialogState.CreateFolder -> "已创建文件夹"
                        else -> "已重命名"
                    },
                )
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(message = error.message ?: "操作失败")
            }
        }
    }

    fun onNoteRowExpanded(fileName: String) {
        onRowExpanded(fileName)
    }

    fun clearExpandedNote() {
        clearExpandedRow()
    }

    fun requestDelete(fileName: String) {
        val note = noteRepository.getNote(fileName) ?: _uiState.value.notes.find { it.fileName == fileName }
        if (note == null) return
        _uiState.update {
            it.copy(
                deleteConfirm = PendingDelete(
                    fileName = fileName,
                    title = note.title,
                    wasSynced = note.syncStatus == SyncStatus.SYNCED,
                ),
                expandedRowKey = null,
            )
        }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(deleteConfirm = null) }
    }

    fun confirmDelete() {
        val pending = _uiState.value.deleteConfirm ?: return
        val entry = noteRepository.moveToTrash(pending.fileName)
        refresh()
        _uiState.update {
            it.copy(
                deleteConfirm = null,
                expandedRowKey = null,
                lastTrashIdForUndo = entry.id,
                undoTrashEvent = System.currentTimeMillis(),
            )
        }
    }

    fun undoLastDelete() {
        val trashId = _uiState.value.lastTrashIdForUndo ?: return
        noteRepository.restoreFromTrash(trashId)
        refresh()
        _uiState.update {
            it.copy(
                lastTrashIdForUndo = null,
                undoTrashEvent = null,
                message = "已恢复笔记",
            )
        }
    }

    fun clearUndoTrash() {
        _uiState.update {
            it.copy(
                lastTrashIdForUndo = null,
                undoTrashEvent = null,
            )
        }
    }

    fun openTrash() {
        _uiState.update {
            it.copy(
                showTrash = true,
                expandedRowKey = null,
                trashEntries = loadTrashEntries(),
            )
        }
    }

    fun closeTrash() {
        _uiState.update { it.copy(showTrash = false) }
    }

    fun restoreTrash(entryId: String) {
        noteRepository.restoreFromTrash(entryId)
        refresh()
        _uiState.update { it.copy(message = "已恢复笔记") }
    }

    fun requestPermanentDelete(entry: TrashEntryUi) {
        _uiState.update { it.copy(permanentDeleteConfirm = entry) }
    }

    fun cancelPermanentDelete() {
        _uiState.update { it.copy(permanentDeleteConfirm = null) }
    }

    fun confirmPermanentDelete() {
        val entry = _uiState.value.permanentDeleteConfirm ?: return
        noteRepository.permanentDeleteFromTrash(entry.id)
        if (_uiState.value.lastTrashIdForUndo == entry.id) {
            clearUndoTrash()
        }
        refresh()
        _uiState.update {
            it.copy(
                permanentDeleteConfirm = null,
                message = "已永久删除",
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun upload() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, message = null) }
            when (val result = syncRepository.uploadPending()) {
                is SyncResult.Success -> {
                    refresh()
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            message = "上传完成：${result.uploaded} 篇，删除 ${result.deleted} 篇",
                        )
                    }
                }
                is SyncResult.Error -> {
                    _uiState.update { it.copy(isSyncing = false, message = result.message) }
                }
            }
        }
    }

    private var pendingConflict: CompletableDeferred<ConflictAction>? = null

    fun download() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, message = null) }
            when (val result = syncRepository.downloadRemote { conflict ->
                val deferred = CompletableDeferred<ConflictAction>()
                pendingConflict = deferred
                _uiState.update { it.copy(conflict = conflict) }
                deferred.await()
            }) {
                is SyncResult.Success -> {
                    refresh()
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            message = "下载完成：${result.downloaded} 篇",
                        )
                    }
                }
                is SyncResult.Error -> {
                    _uiState.update { it.copy(isSyncing = false, message = result.message) }
                }
            }
        }
    }

    fun resolveConflict(action: ConflictAction) {
        _uiState.update { it.copy(conflict = null) }
        pendingConflict?.complete(action)
        pendingConflict = null
    }

    private fun loadTrashEntries(): List<TrashEntryUi> {
        return noteRepository.listTrash().map { it.toUi() }
    }

    private fun TrashEntry.toUi(): TrashEntryUi = TrashEntryUi(
        id = id,
        title = noteRepository.getTrashNoteTitle(this),
        originalPath = originalPath,
        deletedAt = deletedAt,
        wasSynced = syncState.githubSha != null,
    )
}
