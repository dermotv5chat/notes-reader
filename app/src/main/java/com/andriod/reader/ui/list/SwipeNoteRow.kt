package com.andriod.reader.ui.list

import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private val SwipeActionWidth = 128.dp

private enum class SwipeAnchor { Closed, Open }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeNoteRow(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { SwipeActionWidth.toPx() }
    val velocityThresholdPx = with(density) { 125.dp.toPx() }

    val dragState = remember(actionWidthPx) {
        AnchoredDraggableState(
            initialValue = SwipeAnchor.Closed,
            anchors = DraggableAnchors {
                SwipeAnchor.Closed at 0f
                SwipeAnchor.Open at -actionWidthPx
            },
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { velocityThresholdPx },
            snapAnimationSpec = tween(),
            decayAnimationSpec = exponentialDecay(),
        )
    }

    LaunchedEffect(isExpanded) {
        val target = if (isExpanded) SwipeAnchor.Open else SwipeAnchor.Closed
        if (dragState.currentValue != target) {
            dragState.animateTo(target)
        }
    }

    LaunchedEffect(dragState) {
        snapshotFlow { dragState.currentValue }.collect { anchor ->
            val expanded = anchor == SwipeAnchor.Open
            if (expanded != isExpanded) {
                onExpandedChange(expanded)
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(SwipeActionWidth)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    onExpandedChange(false)
                    onEdit()
                },
            ) {
                Icon(Icons.Default.Edit, contentDescription = "编辑")
            }
            IconButton(
                onClick = {
                    onExpandedChange(false)
                    onDelete()
                },
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(
                        x = dragState.requireOffset().roundToInt(),
                        y = 0,
                    )
                }
                .anchoredDraggable(state = dragState, orientation = Orientation.Horizontal)
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onOpen)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
