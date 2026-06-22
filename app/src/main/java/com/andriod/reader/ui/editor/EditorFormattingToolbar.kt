package com.andriod.reader.ui.editor

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.StrikethroughS
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorFormattingToolbar(
    onFormat: (FormatAction) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    canUndo: Boolean,
    canRedo: Boolean,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
    ) {
        HorizontalDivider()
        Row(
            modifier = Modifier
                .horizontalScroll(scrollState)
                .height(48.dp)
                .padding(horizontal = 4.dp),
        ) {
            FormatIconButton(
                icon = { Icon(Icons.Default.FormatBold, contentDescription = "加粗") },
                onClick = { onFormat(FormatAction.Bold) },
            )
            FormatIconButton(
                icon = { Icon(Icons.Default.FormatItalic, contentDescription = "斜体") },
                onClick = { onFormat(FormatAction.Italic) },
            )
            FormatIconButton(
                icon = { Icon(Icons.Default.StrikethroughS, contentDescription = "删除线") },
                onClick = { onFormat(FormatAction.Strikethrough) },
            )
            FormatIconButton(
                icon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "无序列表") },
                onClick = { onFormat(FormatAction.BulletList) },
            )
            FormatIconButton(
                icon = { Icon(Icons.Default.FormatListNumbered, contentDescription = "有序列表") },
                onClick = { onFormat(FormatAction.NumberedList) },
            )
            FormatIconButton(
                icon = { Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = "待办") },
                onClick = { onFormat(FormatAction.Checkbox) },
            )
            FormatTextButton(
                label = "准则",
                onClick = { onFormat(FormatAction.RuleCallout) },
            )
            FormatTextButton(
                label = "习惯",
                onClick = { onFormat(FormatAction.HabitCallout) },
            )
            FormatIconButton(
                icon = { Icon(Icons.Default.Title, contentDescription = "小标题") },
                onClick = { onFormat(FormatAction.Heading) },
            )
            FormatIconButton(
                icon = { Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "撤销") },
                onClick = onUndo,
                enabled = canUndo,
            )
            FormatIconButton(
                icon = { Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "重做") },
                onClick = onRedo,
                enabled = canRedo,
            )
        }
    }
}

@Composable
private fun FormatTextButton(
    label: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
        )
    }
}

@Composable
private fun FormatIconButton(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(48.dp),
    ) {
        icon()
    }
}
