package com.andriod.reader.domain

enum class PracticeEvent {
    FOLLOWED,
    VIOLATED,
}

data class PracticeDayEntry(
    val event: PracticeEvent,
    val note: String = "",
)
