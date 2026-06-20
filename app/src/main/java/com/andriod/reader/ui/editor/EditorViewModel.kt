package com.andriod.reader.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.andriod.reader.data.repository.NoteRepository
import com.andriod.reader.domain.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class EditorUiState(
    val fileName: String? = null,
    val title: String = "",
    val content: String = "",
    val isNew: Boolean = true,
    val saved: Boolean = false,
)

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val noteRepository: NoteRepository,
) : ViewModel() {
    private val initialFileName: String? = savedStateHandle.get<String>("fileName")

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        if (initialFileName != null) {
            val note = noteRepository.getNote(initialFileName)
            if (note != null) {
                _uiState.value = EditorUiState(
                    fileName = note.fileName,
                    title = note.title,
                    content = note.content,
                    isNew = false,
                )
            }
        }
    }

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, saved = false) }
    }

    fun onContentChange(value: String) {
        _uiState.update { it.copy(content = value, saved = false) }
    }

    fun save(): String? {
        val state = _uiState.value
        return if (state.isNew) {
            val note = noteRepository.createNote(state.title, state.content)
            _uiState.update {
                it.copy(fileName = note.fileName, isNew = false, saved = true)
            }
            note.fileName
        } else {
            val existing = noteRepository.getNote(state.fileName!!) ?: return null
            val updated = existing.copy(title = state.title, content = state.content)
            noteRepository.saveNote(updated)
            _uiState.update { it.copy(saved = true) }
            updated.fileName
        }
    }
}
