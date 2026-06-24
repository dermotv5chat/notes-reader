package com.andriod.reader.domain

enum class PracticeMode {
    REPEATLY,
    WHEN,
}

enum class RepeatPeriod {
    DAY,
    WEEK,
    MONTH,
}

enum class PracticeMaturityTier {
    NEUTRAL,
    RED,
    AMBER,
    GREEN,
}

fun PracticeEvent.isStatusEvent(): Boolean = when (this) {
    PracticeEvent.FOLLOWED,
    PracticeEvent.VIOLATED,
    -> true
    PracticeEvent.COMMENT -> false
}
