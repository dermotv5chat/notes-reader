package com.andriod.reader.ui.editor

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.andriod.reader.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class EditorUiState(
    val fileName: String? = null,
    val title: String = "",
    val body: TextFieldValue = TextFieldValue(""),
    val isNew: Boolean = true,
    val isDirty: Boolean = false,
    val lastSavedAt: Instant? = null,
    val bodyFocused: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
) : ViewModel() {
    private val initialFileName: String? = savedStateHandle.get<String>("fileName")

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var saveJob: Job? = null
    private val undoHistory = EditorUndoHistory()
    private var isUndoRedo = false

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm")
        .withZone(ZoneId.systemDefault())

    init {
        if (initialFileName != null) {
            val note = noteRepository.getNote(initialFileName)
            if (note != null) {
                _uiState.value = EditorUiState(
                    fileName = note.fileName,
                    title = note.title,
                    body = TextFieldValue(note.content),
                    isNew = false,
                    lastSavedAt = note.updatedAt,
                )
            }
        }
    }

    fun formattedLastSavedAt(): String? {
        val instant = _uiState.value.lastSavedAt ?: return null
        return dateFormatter.format(instant)
    }

    fun onTitleChange(value: String) {
        val current = _uiState.value
        if (!isUndoRedo && value != current.title) {
            undoHistory.recordBeforeChange(current.toSnapshot())
        }
        _uiState.value = current.copy(
            title = value,
            isDirty = true,
            canUndo = undoHistory.canUndo,
            canRedo = undoHistory.canRedo,
        )
        scheduleSave()
    }

    fun onBodyChange(value: TextFieldValue) {
        val current = _uiState.value
        if (!isUndoRedo && value.text != current.body.text) {
            undoHistory.recordBeforeChange(current.toSnapshot())
        }
        _uiState.value = current.copy(
            body = value,
            isDirty = true,
            canUndo = undoHistory.canUndo,
            canRedo = undoHistory.canRedo,
        )
        scheduleSave()
    }

    fun onBodyFocusChanged(focused: Boolean) {
        _uiState.update { it.copy(bodyFocused = focused) }
    }

    fun applyFormat(action: FormatAction) {
        val current = _uiState.value
        if (!isUndoRedo) {
            undoHistory.recordBeforeChange(current.toSnapshot())
        }
        val updated = MarkdownEditorActions.apply(current.body, action)
        _uiState.value = current.copy(
            body = updated,
            isDirty = true,
            canUndo = undoHistory.canUndo,
            canRedo = undoHistory.canRedo,
        )
        scheduleSave()
    }

    fun undo() {
        if (!undoHistory.canUndo) return
        val current = _uiState.value
        val restored = undoHistory.undo(current.toSnapshot()) ?: return
        isUndoRedo = true
        _uiState.value = current.copy(
            title = restored.title,
            body = restored.body,
            isDirty = true,
            canUndo = undoHistory.canUndo,
            canRedo = undoHistory.canRedo,
        )
        isUndoRedo = false
        scheduleSave()
    }

    fun redo() {
        if (!undoHistory.canRedo) return
        val current = _uiState.value
        val restored = undoHistory.redo(current.toSnapshot()) ?: return
        isUndoRedo = true
        _uiState.value = current.copy(
            title = restored.title,
            body = restored.body,
            isDirty = true,
            canUndo = undoHistory.canUndo,
            canRedo = undoHistory.canRedo,
        )
        isUndoRedo = false
        scheduleSave()
    }

    fun onBack(): String? {
        saveJob?.cancel()
        return saveIfDirty()
    }

    override fun onCleared() {
        saveJob?.cancel()
        saveIfDirty()
        super.onCleared()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DELAY_MS)
            saveIfDirty()
        }
    }

    private fun saveIfDirty(): String? {
        val state = _uiState.value
        if (!state.isDirty) return state.fileName

        val title = state.title.trim()
        val content = state.body.text.trim()
        if (state.isNew && title.isEmpty() && content.isEmpty()) {
            _uiState.update { it.copy(isDirty = false) }
            return null
        }

        return if (state.isNew) {
            val note = noteRepository.createNote(
                title = state.title,
                content = state.body.text,
            )
            _uiState.update {
                it.copy(
                    fileName = note.fileName,
                    isNew = false,
                    isDirty = false,
                    lastSavedAt = note.updatedAt,
                )
            }
            note.fileName
        } else {
            val existing = noteRepository.getNote(state.fileName!!) ?: return null
            val updated = existing.copy(
                title = state.title,
                content = state.body.text,
            )
            val saved = noteRepository.saveNote(updated)
            _uiState.update {
                it.copy(
                    isDirty = false,
                    lastSavedAt = saved.updatedAt,
                )
            }
            saved.fileName
        }
    }

    private fun EditorUiState.toSnapshot() = EditorSnapshot(title = title, body = body)

    companion object {
        private const val AUTO_SAVE_DELAY_MS = 600L
    }
}
