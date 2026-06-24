package com.andriod.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.andriod.reader.domain.PracticeEvent
import com.andriod.reader.domain.PracticeLogEntry
import com.andriod.reader.domain.PracticeMode
import com.andriod.reader.domain.isStatusEvent
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun PracticeHistoryCalendar(
    history: List<PracticeLogEntry>,
    mode: PracticeMode,
    modifier: Modifier = Modifier,
    zoneId: ZoneId = ZoneId.systemDefault(),
    onDaySelected: ((LocalDate, List<PracticeLogEntry>) -> Unit)? = null,
) {
    var month by remember(history) { mutableStateOf(YearMonth.now()) }
    val entriesByDate = remember(history, zoneId) {
        history
            .filter { it.event.isStatusEvent() }
            .groupBy { it.recordedAt.atZone(zoneId).toLocalDate() }
    }
    val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(PracticeSheetTestTags.HISTORY_CALENDAR),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "◀",
                modifier = Modifier
                    .clickable { month = month.minusMonths(1) }
                    .padding(8.dp),
            )
            Text(
                text = "${month.year}年${month.month.getDisplayName(TextStyle.SHORT, Locale.CHINA)}",
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = "▶",
                modifier = Modifier
                    .clickable { month = month.plusMonths(1) }
                    .padding(8.dp),
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            weekdayLabels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val firstDay = month.atDay(1)
        val leadingBlanks = (firstDay.dayOfWeek.value + 6) % 7
        val daysInMonth = month.lengthOfMonth()
        val cells = leadingBlanks + daysInMonth
        val rows = (cells + 6) / 7

        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        val dayNumber = cellIndex - leadingBlanks + 1
                        if (dayNumber in 1..daysInMonth) {
                            val date = month.atDay(dayNumber)
                            val dayEntries = entriesByDate[date].orEmpty()
                            val showDay = when (mode) {
                                PracticeMode.WHEN -> dayEntries.isNotEmpty()
                                PracticeMode.REPEATLY -> true
                            }
                            if (showDay) {
                                val dominant = dayEntries.maxByOrNull { it.recordedAt }?.event
                                val dotColor = dominant?.let { eventColor(it) }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .then(
                                            if (onDaySelected != null && dayEntries.isNotEmpty()) {
                                                Modifier.clickable { onDaySelected(date, dayEntries) }
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .padding(4.dp),
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (dayEntries.isEmpty()) {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    if (dotColor != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(dotColor),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun eventColor(event: PracticeEvent) = when (event) {
    PracticeEvent.FOLLOWED -> MaterialTheme.colorScheme.primary
    PracticeEvent.VIOLATED -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.outline
}
