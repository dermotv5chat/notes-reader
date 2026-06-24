package com.andriod.reader.domain

enum class TtsPresynthUiState {
    /** System TTS — no presynth UI */
    Hidden,
    NotPrepared,
    Queued,
    Preparing,
    Ready,
    Stale,
    Failed,
}
