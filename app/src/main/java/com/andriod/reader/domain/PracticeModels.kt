package com.andriod.reader.domain

import java.time.Instant

enum class PracticeEvent {
    FOLLOWED,
    VIOLATED,
    MUYU,
}

/** Latest practice summary for a block in the current period (used for status dots). */
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

fun parsePracticeEvent(raw: String): PracticeEvent? = when (raw) {
    "FOLLOWED" -> PracticeEvent.FOLLOWED
    "VIOLATED" -> PracticeEvent.VIOLATED
    "MUYU" -> PracticeEvent.MUYU
    "COMMENT" -> PracticeEvent.MUYU
    "PARTIAL" -> PracticeEvent.FOLLOWED
    "NOT_ENCOUNTERED" -> null
    else -> runCatching { PracticeEvent.valueOf(raw) }.getOrNull()
}
