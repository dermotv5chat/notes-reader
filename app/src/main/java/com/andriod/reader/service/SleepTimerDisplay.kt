package com.andriod.reader.service

object SleepTimerDisplay {
    fun formatFixedRemainingMs(remainingMs: Long): String {
        val totalSeconds = ((remainingMs + 999) / 1000).coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    fun fixedCountdownLabel(remainingMs: Long): String =
        "${formatFixedRemainingMs(remainingMs)} 后关闭"

    fun fixedCountdownSheetStatus(remainingMs: Long): String =
        "剩余 ${formatFixedRemainingMs(remainingMs)}"

    fun afterNoteEndStatusLabel(estimatedMinutes: Int): String =
        if (estimatedMinutes > 0) {
            "本篇结束后关闭 · 约 $estimatedMinutes 分钟"
        } else {
            "本篇结束后关闭"
        }
}
