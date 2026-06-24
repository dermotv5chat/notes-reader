package com.andriod.reader.service.synthesis

import com.andriod.reader.domain.TtsPresynthUiState

data class PresynthJobState(
    val fileName: String,
    val title: String,
    val uiState: TtsPresynthUiState = TtsPresynthUiState.NotPrepared,
    val hint: String? = null,
    val chunkProgress: String? = null,
    val progressFraction: Float? = null,
) {
    val isPreparing: Boolean get() = uiState == TtsPresynthUiState.Preparing
    val isReady: Boolean get() = uiState == TtsPresynthUiState.Ready
}
