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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private enum class SwipeAnchor { Closed, Open }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeRevealRow(
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    actionWidth: Dp,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    backgroundActions: @Composable RowScope.() -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { actionWidth.toPx() }
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
                .width(actionWidth)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
            content = backgroundActions,
        )
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
                .padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

object SwipeRowKeys {
    fun folder(path: String): String = "folder:$path"
    fun isFolder(key: String): Boolean = key.startsWith("folder:")
}
