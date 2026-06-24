package com.andriod.reader.ui.reader

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.andriod.reader.domain.PracticeLogEntry
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeCalendarScreen(
    onBack: () -> Unit,
    viewModel: PracticeCalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedDayEntries by remember { mutableStateOf<List<PracticeLogEntry>?>(null) }
    var editingEntry by remember { mutableStateOf<PracticeLogEntry?>(null) }

    editingEntry?.let { entry ->
        PracticeNoteDialog(
            event = entry.event,
            initialNote = entry.note,
            editMode = true,
            onConfirm = { note ->
                viewModel.updateEntryNote(entry.recordedAt, note)
                editingEntry = null
                selectedDayEntries = selectedDayEntries?.map { item ->
                    if (item.recordedAt == entry.recordedAt) item.copy(note = note) else item
                }
            },
            onDismiss = { editingEntry = null },
        )
    }

    selectedDayEntries?.let { entries ->
        AlertDialog(
            onDismissRequest = { selectedDayEntries = null },
            title = { Text("当日记录") },
            text = {
                Column {
                    entries.forEach { entry ->
                        PracticeHistoryRow(
                            entry = entry,
                            onClick = { editingEntry = entry },
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedDayEntries = null }) {
                    Text("关闭")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("践行月历")
                        if (uiState.blockLabel.isNotBlank()) {
                            Text(
                                text = uiState.blockLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        PracticeHistoryCalendar(
            history = uiState.history,
            mode = uiState.mode,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            onDaySelected = { _: LocalDate, entries -> selectedDayEntries = entries },
        )
    }
}
