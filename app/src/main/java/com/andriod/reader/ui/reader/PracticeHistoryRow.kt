package com.andriod.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.andriod.reader.domain.PracticeEvent
import com.andriod.reader.domain.PracticeLogEntry
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun formatPracticeTime(entry: PracticeLogEntry): String {
    val formatter = DateTimeFormatter.ofPattern("M月d日 HH:mm")
    return entry.recordedAt.atZone(ZoneId.systemDefault()).format(formatter)
}

@Composable
internal fun PracticeHistoryRow(
    entry: PracticeLogEntry,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val eventLabel = PracticeSheetLabels.eventLabel(entry.event)
    val eventColor = when (entry.event) {
        PracticeEvent.FOLLOWED -> MaterialTheme.colorScheme.primary
        PracticeEvent.VIOLATED -> MaterialTheme.colorScheme.error
        PracticeEvent.MUYU -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val timeLabel = formatPracticeTime(entry)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
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
        } else if (onClick != null) {
            Text(
                text = "点击添加备注",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
