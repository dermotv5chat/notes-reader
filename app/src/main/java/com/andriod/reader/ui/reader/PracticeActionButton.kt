package com.andriod.reader.ui.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun PracticeActionButton(
    label: String,
    containerColor: Color,
    contentColor: Color,
    onQuickTap: () -> Unit,
    onLongPressForNote: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Surface(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .then(
                if (testTag != null) Modifier.testTag(testTag) else Modifier,
            )
            .pointerInput(onQuickTap, onLongPressForNote) {
                detectTapGestures(
                    onTap = { onQuickTap() },
                    onLongPress = { onLongPressForNote() },
                )
            },
        shape = MaterialTheme.shapes.small,
        color = containerColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
            )
        }
    }
}
