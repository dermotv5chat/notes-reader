package com.andriod.reader.service

object TtsRemainingEstimate {
    private const val CHARS_PER_MINUTE_AT_1X = 150.0

    fun estimateNoteRemainingMinutes(
        segments: List<String>,
        fromIndex: Int,
        speechRate: Float,
    ): Int {
        if (segments.isEmpty() || fromIndex >= segments.size) return 0
        val chars = segments.drop(fromIndex).sumOf { it.length }
        if (chars == 0) return 1
        val rate = speechRate.coerceAtLeast(0.5f)
        return maxOf(1, (chars / (CHARS_PER_MINUTE_AT_1X * rate)).toInt())
    }

    fun estimateTotalDurationMs(
        segments: List<String>,
        speechRate: Float,
    ): Long {
        if (segments.isEmpty()) return 0L
        val chars = segments.sumOf { it.length }
        return estimateDurationMsForChars(chars, speechRate)
    }

    fun estimateSegmentDurationMs(
        text: String,
        speechRate: Float,
    ): Long = estimateDurationMsForChars(text.length, speechRate)

    fun estimateDurationMsForChars(chars: Int, speechRate: Float): Long {
        if (chars <= 0) return 0L
        val rate = speechRate.coerceAtLeast(0.5f)
        val minutes = chars / (CHARS_PER_MINUTE_AT_1X * rate)
        return (minutes * 60_000.0).toLong().coerceAtLeast(1_000L)
    }
}
