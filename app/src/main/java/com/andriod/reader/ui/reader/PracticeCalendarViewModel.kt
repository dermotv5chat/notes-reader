package com.andriod.reader.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.andriod.reader.data.repository.PracticeRepository
import com.andriod.reader.domain.PracticeLogEntry
import com.andriod.reader.domain.PracticeMode
import com.andriod.reader.domain.RepeatPeriod
import com.andriod.reader.ui.NavArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import javax.inject.Inject

data class PracticeCalendarUiState(
    val blockLabel: String = "",
    val mode: PracticeMode = PracticeMode.REPEATLY,
    val history: List<PracticeLogEntry> = emptyList(),
)

@HiltViewModel
class PracticeCalendarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val practiceRepository: PracticeRepository,
) : ViewModel() {
    private val fileName: String = NavArgs.decodeFileName(savedStateHandle.get<String>("fileName"))
    private val blockId: String = savedStateHandle.get<String>("blockId").orEmpty()
    private val blockLabel: String = NavArgs.decodeFileName(savedStateHandle.get<String>("blockLabel"))
    private val mode: PracticeMode = runCatching {
        PracticeMode.valueOf(savedStateHandle.get<String>("mode").orEmpty())
    }.getOrDefault(PracticeMode.REPEATLY)

    private val _uiState = MutableStateFlow(
        PracticeCalendarUiState(
            blockLabel = blockLabel,
            mode = mode,
            history = practiceRepository.getBlockHistory(fileName, blockId),
        ),
    )
    val uiState: StateFlow<PracticeCalendarUiState> = _uiState.asStateFlow()

    fun updateEntryNote(recordedAt: Instant, note: String) {
        practiceRepository.updateEntryNote(fileName, blockId, recordedAt, note)
        refreshHistory()
    }

    private fun refreshHistory() {
        _uiState.update {
            it.copy(history = practiceRepository.getBlockHistory(fileName, blockId))
        }
    }
}
