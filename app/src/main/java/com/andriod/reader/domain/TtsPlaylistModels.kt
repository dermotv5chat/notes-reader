package com.andriod.reader.domain

import java.time.Instant

data class TtsPlaylistItem(
    val fileName: String,
    val title: String,
    val addedAt: Instant = Instant.now(),
)

enum class TtsQueueRepeatMode {
    OFF,
    REPEAT_ONE,
    REPEAT_ALL,
}
