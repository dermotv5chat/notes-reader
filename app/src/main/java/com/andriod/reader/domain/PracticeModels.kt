package com.andriod.reader.domain

import java.time.Instant

enum class PracticeEvent {
    FOLLOWED,
    VIOLATED,
    COMMENT,
}

/** Latest practice summary for a block on a given day (used for status dots). */
data class PracticeDayEntry(
    val event: PracticeEvent,
    val note: String = "",
)

/** A single append-only practice log line with timestamp. */
data class PracticeLogEntry(
    val event: PracticeEvent,
    val note: String = "",
    val recordedAt: Instant,
)
