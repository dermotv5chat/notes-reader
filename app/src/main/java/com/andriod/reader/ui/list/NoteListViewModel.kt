package com.andriod.reader.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

data class NoteListUiState(
    val notes: List<Note> = emptyList(),
    val currentFolder: String = "",
    val query: String = "",
    val isSyncing: Boolean = false,
    val message: String? = null,
    val conflict: SyncConflict? = null,
    val expandedNoteKey: String? = null,
    val deleteConfirm: PendingDelete? = null,
    val permanentDeleteConfirm: TrashEntryUi? = null,
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
                expandedNoteKey = null,
            )
        }
    }

    fun openFolder(path: String) {
        _uiState.update {
            it.copy(
                currentFolder = path,
                query = "",
                notes = noteRepository.listNotes(""),
                expandedNoteKey = null,
            )
        }
    }

    fun navigateUp() {
        _uiState.update {
            it.copy(
                currentFolder = NoteTreeBrowser.parentFolder(it.currentFolder),
                expandedNoteKey = null,
            )
        }
    }

    fun onNoteRowExpanded(fileName: String) {
        _uiState.update { it.copy(expandedNoteKey = fileName) }
    }

    fun clearExpandedNote() {
        _uiState.update { it.copy(expandedNoteKey = null) }
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
                expandedNoteKey = null,
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
                expandedNoteKey = null,
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
                expandedNoteKey = null,
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
