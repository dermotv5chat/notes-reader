package com.andriod.reader.ui.reader

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.andriod.reader.domain.PracticeEvent

@Composable
internal fun PracticeNoteDialog(
    event: PracticeEvent,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    initialNote: String = "",
    editMode: Boolean = false,
) {
    var note by remember(event, initialNote, editMode) { mutableStateOf(initialNote) }
    val title = when {
        editMode && initialNote.isBlank() -> "添加备注"
        editMode -> "编辑备注"
        else -> "添加备注"
    }
    val fieldLabel = when (event) {
        PracticeEvent.MUYU -> "备注（敲一下）"
        PracticeEvent.FOLLOWED -> "备注（遵守）"
        PracticeEvent.VIOLATED -> "备注（违背）"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = title,
                modifier = Modifier.testTag(PracticeSheetTestTags.NOTE_DIALOG),
            )
        },
        text = {
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(PracticeSheetTestTags.NOTE_FIELD),
                label = { Text(fieldLabel) },
                minLines = 2,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(note.trim()) },
                modifier = Modifier.testTag(PracticeSheetTestTags.NOTE_CONFIRM_BUTTON),
            ) {
                Text("确认")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}
