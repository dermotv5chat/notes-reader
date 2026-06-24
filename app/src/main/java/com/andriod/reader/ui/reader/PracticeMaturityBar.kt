package com.andriod.reader.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.andriod.reader.domain.PracticeMaturityTier

@Composable
fun PracticeMaturityBar(
    tier: PracticeMaturityTier,
    modifier: Modifier = Modifier,
) {
    val color = when (tier) {
        PracticeMaturityTier.GREEN -> MaterialTheme.colorScheme.primary
        PracticeMaturityTier.AMBER -> MaterialTheme.colorScheme.tertiary
        PracticeMaturityTier.RED -> MaterialTheme.colorScheme.error
        PracticeMaturityTier.NEUTRAL -> MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
    }
    Box(
        modifier = modifier
            .width(4.dp)
            .fillMaxHeight()
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}
