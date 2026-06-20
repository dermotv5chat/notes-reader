package com.andriod.reader.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriod.reader.data.repository.NoteRepository
import com.andriod.reader.data.repository.SyncRepository
import com.andriod.reader.domain.ConflictAction
import com.andriod.reader.domain.Note
import com.andriod.reader.domain.SyncConflict
import com.andriod.reader.domain.SyncResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NoteListUiState(
    val notes: List<Note> = emptyList(),
    val currentFolder: String = "",
    val query: String = "",
    val isSyncing: Boolean = false,
    val message: String? = null,
    val conflict: SyncConflict? = null,
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
            )
        }
    }

    fun openFolder(path: String) {
        _uiState.update {
            it.copy(
                currentFolder = path,
                query = "",
                notes = noteRepository.listNotes(""),
            )
        }
    }

    fun navigateUp() {
        _uiState.update {
            it.copy(currentFolder = NoteTreeBrowser.parentFolder(it.currentFolder))
        }
    }

    fun deleteNote(fileName: String) {
        noteRepository.deleteNote(fileName)
        refresh()
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
}
