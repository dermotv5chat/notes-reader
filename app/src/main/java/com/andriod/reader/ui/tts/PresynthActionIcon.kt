package com.andriod.reader.ui.tts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.andriod.reader.domain.TtsPresynthUiState

@Composable
fun PresynthActionIcon(
    state: TtsPresynthUiState,
    progressFraction: Float?,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "预生成语音",
) {
    IconButton(
        onClick = onClick,
        enabled = enabled || state == TtsPresynthUiState.Preparing || state == TtsPresynthUiState.Queued,
        modifier = modifier.testTag("presynth_action_icon"),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (state) {
                TtsPresynthUiState.Preparing -> {
                    if (progressFraction != null) {
                        CircularProgressIndicator(
                            progress = { progressFraction.coerceIn(0f, 1f) },
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                }
                else -> Unit
            }
            val (icon, tint) = when (state) {
                TtsPresynthUiState.Ready -> Icons.Default.Check to MaterialTheme.colorScheme.primary
                TtsPresynthUiState.Failed -> Icons.Default.Refresh to MaterialTheme.colorScheme.error
                TtsPresynthUiState.Queued -> Icons.Default.AutoAwesome to MaterialTheme.colorScheme.outline
                TtsPresynthUiState.Preparing -> Icons.Default.AutoAwesome to MaterialTheme.colorScheme.onSurfaceVariant
                else -> Icons.Default.AutoAwesome to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Icon(
                imageVector = if (state == TtsPresynthUiState.Ready) Icons.Default.AutoAwesome else icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(if (state == TtsPresynthUiState.Preparing) 20.dp else 24.dp),
            )
        }
    }
}
