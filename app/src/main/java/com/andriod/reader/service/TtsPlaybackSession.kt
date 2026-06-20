package com.andriod.reader.service

data class TtsPlaybackSession(
    val fileName: String? = null,
    val title: String? = null,
    val segmentIndex: Int = 0,
    val segmentTotal: Int = 0,
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
) {
    val hasActiveSession: Boolean
        get() = fileName != null && segmentTotal > 0 && (isPlaying || isPaused)
}
