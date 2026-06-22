package com.andriod.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriod.reader.domain.NoteBlock
import com.andriod.reader.domain.PracticeDayEntry
import com.andriod.reader.domain.PracticeEvent
import com.andriod.reader.domain.shouldDisplayInReader

@Composable
fun BlockReaderContent(
    blocks: List<NoteBlock>,
    todayPractice: Map<String, PracticeDayEntry>,
    onTrackableBlockClick: (NoteBlock) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visibleBlocks = blocks.filter { it.shouldDisplayInReader() }
    if (visibleBlocks.isEmpty()) {
        Text("笔记为空", modifier = modifier.padding(8.dp))
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(visibleBlocks, key = { it.id }) { block ->
            NoteBlockRow(
                block = block,
                todayEntry = todayPractice[block.id],
                onClick = {
                    if (block.trackable) {
                        onTrackableBlockClick(block)
                    }
                },
            )
        }
    }
}

@Composable
private fun NoteBlockRow(
    block: NoteBlock,
    todayEntry: PracticeDayEntry?,
    onClick: () -> Unit,
) {
    val clickable = block.trackable
    val cardStyle = block.trackable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (cardStyle) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (block) {
                                is NoteBlock.Callout ->
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                        )
                } else {
                    Modifier
                },
            )
            .then(
                if (clickable) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(
                horizontal = if (cardStyle) 12.dp else 0.dp,
                vertical = if (cardStyle) 10.dp else 4.dp,
            ),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            when (block) {
                is NoteBlock.Heading -> {
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = when (block.level) {
                                1 -> 26.sp
                                2 -> 22.sp
                                else -> 20.sp
                            },
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                is NoteBlock.Todo -> {
                    Text(
                        text = "${if (block.checked) "☑" else "☐"} ${block.text}",
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (block.checked) TextDecoration.LineThrough else null,
                    )
                }
                is NoteBlock.Callout -> {
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                is NoteBlock.Bullet -> {
                    Text(
                        text = "• ${block.text}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                is NoteBlock.Ordered -> {
                    Text(
                        text = "${block.number}. ${block.text}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                is NoteBlock.Paragraph -> {
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
        if (block.trackable) {
            PracticeStatusDot(todayEntry = todayEntry)
        }
    }
}

@Composable
private fun PracticeStatusDot(todayEntry: PracticeDayEntry?) {
    val color = when (todayEntry?.event) {
        PracticeEvent.FOLLOWED -> MaterialTheme.colorScheme.primary
        PracticeEvent.VIOLATED -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    Box(
        modifier = Modifier
            .padding(start = 8.dp, top = 6.dp)
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

data class PracticeSheetState(
    val blockId: String,
    val blockLabel: String,
    val existingNote: String = "",
    val existingEvent: PracticeEvent? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeSheet(
    sheetState: PracticeSheetState?,
    onDismiss: () -> Unit,
    onSave: (PracticeEvent, String) -> Unit,
    onClear: () -> Unit,
) {
    if (sheetState == null) return

    val modalState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = modalState,
    ) {
        PracticeSheetContent(
            sheetState = sheetState,
            onSave = onSave,
            onClear = onClear,
        )
    }
}

@Composable
internal fun PracticeSheetContent(
    sheetState: PracticeSheetState,
    onSave: (PracticeEvent, String) -> Unit,
    onClear: () -> Unit,
) {
    var note by remember(sheetState.blockId) {
        mutableStateOf(sheetState.existingNote)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "今日践行",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = sheetState.blockLabel,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag(PracticeSheetTestTags.SHEET_TITLE),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { onSave(PracticeEvent.FOLLOWED, note.trim()) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(PracticeSheetTestTags.FOLLOWED_BUTTON),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("遵守")
            }
            Button(
                onClick = { onSave(PracticeEvent.VIOLATED, note.trim()) },
                modifier = Modifier
                    .weight(1f)
                    .testTag(PracticeSheetTestTags.VIOLATED_BUTTON),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("违背")
            }
        }

        OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PracticeSheetTestTags.NOTE_FIELD),
            label = { Text("备注（可选）") },
            minLines = 2,
        )

        if (sheetState.existingEvent != null) {
            OutlinedButton(
                onClick = onClear,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("清除今日记录")
            }
        }
    }
}

fun NoteBlock.displayLabel(): String = when (this) {
    is NoteBlock.Todo -> text
    is NoteBlock.Callout -> text
    else -> rawLine
}
