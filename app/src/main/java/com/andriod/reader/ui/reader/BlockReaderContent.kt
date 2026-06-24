package com.andriod.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.andriod.reader.data.repository.BlockPracticeDisplayMeta
import com.andriod.reader.domain.NoteBlock
import com.andriod.reader.domain.PracticeMode
import com.andriod.reader.domain.PracticeDayEntry
import com.andriod.reader.domain.PracticeEvent
import com.andriod.reader.domain.PracticeLogEntry
import com.andriod.reader.domain.PracticePeriod
import com.andriod.reader.domain.RepeatPeriod
import com.andriod.reader.domain.isStatusEvent
import com.andriod.reader.domain.shouldDisplayInReader
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun BlockReaderContent(
    blocks: List<NoteBlock>,
    todayPractice: Map<String, PracticeDayEntry>,
    practiceMeta: Map<String, BlockPracticeDisplayMeta>,
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
                displayMeta = practiceMeta[block.id],
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
    displayMeta: BlockPracticeDisplayMeta?,
    onClick: () -> Unit,
) {
    val callout = block as? NoteBlock.Callout
    val mode = callout?.mode ?: PracticeMode.REPEATLY
    val repeatPeriod = callout?.repeatPeriod ?: RepeatPeriod.DAY
    val clickable = block.trackable
    val cardStyle = block.trackable
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(
                if (cardStyle) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
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
        if (block.trackable && displayMeta != null) {
            PracticeMaturityBar(tier = displayMeta.maturityTier)
        }
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
            PracticeBlockIndicator(
                mode = mode,
                periodEntry = todayEntry,
                lastStatusDate = displayMeta?.lastStatusDate,
            )
        }
    }
}

@Composable
private fun PracticeBlockIndicator(
    mode: PracticeMode,
    periodEntry: PracticeDayEntry?,
    lastStatusDate: LocalDate?,
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
    ) {
        val showIdleDot = mode == PracticeMode.REPEATLY
        if (showIdleDot || (periodEntry != null && periodEntry.event.isStatusEvent())) {
            PracticeStatusDot(
                periodEntry = periodEntry,
                showWhenEmpty = showIdleDot,
            )
        }
        if (mode == PracticeMode.WHEN) {
            lastStatusDate?.let { date ->
                Text(
                    text = "上次 ${date.monthValue}/${date.dayOfMonth}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(PracticeSheetTestTags.LAST_STATUS_DATE),
                )
            }
        }
    }
}

@Composable
private fun PracticeStatusDot(
    periodEntry: PracticeDayEntry?,
    showWhenEmpty: Boolean,
) {
    if (periodEntry == null && !showWhenEmpty) return
    val color = when (periodEntry?.event) {
        PracticeEvent.FOLLOWED -> MaterialTheme.colorScheme.primary
        PracticeEvent.VIOLATED -> MaterialTheme.colorScheme.error
        PracticeEvent.COMMENT, null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color),
    )
}

data class PracticeSheetState(
    val blockId: String,
    val blockLabel: String,
    val mode: PracticeMode = PracticeMode.REPEATLY,
    val repeatPeriod: RepeatPeriod = RepeatPeriod.DAY,
    val hasPeriodEntry: Boolean = false,
    val history: List<PracticeLogEntry> = emptyList(),
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
    var pendingNoteEvent by remember(sheetState.blockId) {
        mutableStateOf<PracticeEvent?>(null)
    }
    var showClearConfirm by remember(sheetState.blockId) {
        mutableStateOf(false)
    }
    var historyExpanded by remember(sheetState.blockId) {
        mutableStateOf(false)
    }
    var historyTab by remember(sheetState.blockId) {
        mutableIntStateOf(0)
    }
    var selectedDayEntries by remember(sheetState.blockId) {
        mutableStateOf<List<PracticeLogEntry>?>(null)
    }

    pendingNoteEvent?.let { event ->
        PracticeNoteDialog(
            event = event,
            onConfirm = { note ->
                pendingNoteEvent = null
                onSave(event, note)
            },
            onDismiss = { pendingNoteEvent = null },
        )
    }

    selectedDayEntries?.let { entries ->
        AlertDialog(
            onDismissRequest = { selectedDayEntries = null },
            title = { Text("当日记录") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entries.forEach { entry ->
                        PracticeHistoryRow(entry = entry)
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

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = {
                Text(
                    text = PracticePeriod.clearConfirmTitle(sheetState.repeatPeriod),
                    modifier = Modifier.testTag(PracticeSheetTestTags.CLEAR_CONFIRM_DIALOG),
                )
            },
            text = {
                Text(PracticePeriod.clearConfirmBody(sheetState.repeatPeriod))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirm = false
                        onClear()
                    },
                    modifier = Modifier.testTag(PracticeSheetTestTags.CLEAR_CONFIRM_BUTTON),
                ) {
                    Text("清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("取消")
                }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = PracticeSheetLabels.sheetTitle(sheetState.mode, sheetState.repeatPeriod),
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
            PracticeActionButton(
                label = PracticeSheetLabels.followedLabel(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onQuickTap = { onSave(PracticeEvent.FOLLOWED, "") },
                onLongPressForNote = { pendingNoteEvent = PracticeEvent.FOLLOWED },
                modifier = Modifier.weight(1f),
                testTag = PracticeSheetTestTags.FOLLOWED_BUTTON,
            )
            PracticeActionButton(
                label = PracticeSheetLabels.violatedLabel(),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
                onQuickTap = { onSave(PracticeEvent.VIOLATED, "") },
                onLongPressForNote = { pendingNoteEvent = PracticeEvent.VIOLATED },
                modifier = Modifier.weight(1f),
                testTag = PracticeSheetTestTags.VIOLATED_BUTTON,
            )
        }

        OutlinedButton(
            onClick = { pendingNoteEvent = PracticeEvent.COMMENT },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PracticeSheetTestTags.COMMENT_BUTTON),
        ) {
            Text("评论")
        }

        Text(
            text = "轻点快记 · 长按可加备注 · 评论写想法",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { historyExpanded = !historyExpanded }
                    .padding(vertical = 4.dp)
                    .testTag(PracticeSheetTestTags.HISTORY_SECTION),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "历史记录（${sheetState.history.size} 条）",
                    style = MaterialTheme.typography.titleSmall,
                )
                Icon(
                    imageVector = if (historyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (historyExpanded) "收起历史" else "展开历史",
                    modifier = Modifier.testTag(PracticeSheetTestTags.HISTORY_TOGGLE),
                )
            }

            if (historyExpanded) {
                TabRow(selectedTabIndex = historyTab) {
                    Tab(
                        selected = historyTab == 0,
                        onClick = { historyTab = 0 },
                        text = { Text("列表") },
                        modifier = Modifier.testTag(PracticeSheetTestTags.HISTORY_TAB_LIST),
                    )
                    Tab(
                        selected = historyTab == 1,
                        onClick = { historyTab = 1 },
                        text = { Text("月历") },
                        modifier = Modifier.testTag(PracticeSheetTestTags.HISTORY_TAB_CALENDAR),
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when (historyTab) {
                        0 -> {
                            if (sheetState.history.isEmpty()) {
                                Text(
                                    text = "暂无记录",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag(PracticeSheetTestTags.HISTORY_EMPTY),
                                )
                            } else {
                                sheetState.history.forEachIndexed { index, entry ->
                                    PracticeHistoryRow(
                                        entry = entry,
                                        modifier = if (index == 0) {
                                            Modifier.testTag(PracticeSheetTestTags.HISTORY_ITEM)
                                        } else {
                                            Modifier
                                        },
                                    )
                                }
                            }
                        }
                        1 -> {
                            PracticeHistoryCalendar(
                                history = sheetState.history,
                                mode = sheetState.mode,
                                onDaySelected = { _, entries -> selectedDayEntries = entries },
                            )
                        }
                    }

                    if (sheetState.hasPeriodEntry) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        )
                        TextButton(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag(PracticeSheetTestTags.CLEAR_TODAY_BUTTON),
                        ) {
                            Text(
                                text = PracticePeriod.clearLabel(sheetState.repeatPeriod),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PracticeHistoryRow(
    entry: PracticeLogEntry,
    modifier: Modifier = Modifier,
) {
    val eventLabel = when (entry.event) {
        PracticeEvent.FOLLOWED -> "遵守"
        PracticeEvent.VIOLATED -> "违背"
        PracticeEvent.COMMENT -> "评论"
    }
    val eventColor = when (entry.event) {
        PracticeEvent.FOLLOWED -> MaterialTheme.colorScheme.primary
        PracticeEvent.VIOLATED -> MaterialTheme.colorScheme.error
        PracticeEvent.COMMENT -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val timeLabel = formatPracticeTime(entry)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = eventLabel,
                style = MaterialTheme.typography.labelLarge,
                color = eventColor,
            )
        }
        if (entry.note.isNotBlank()) {
            Text(
                text = entry.note,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

internal fun formatPracticeTime(entry: PracticeLogEntry): String {
    val formatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    return entry.recordedAt.atZone(ZoneId.systemDefault()).format(formatter)
}

fun NoteBlock.displayLabel(): String = when (this) {
    is NoteBlock.Todo -> text
    is NoteBlock.Callout -> text
    else -> rawLine
}
