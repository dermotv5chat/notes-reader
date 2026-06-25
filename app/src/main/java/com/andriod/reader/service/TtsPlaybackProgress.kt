package com.andriod.reader.service

object TtsPlaybackProgress {
    fun formatPlaybackTime(ms: Long): String {
        if (ms < 0) return "--:--"
        val totalSeconds = (ms / 1_000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    fun formatProgressRange(positionMs: Long, durationMs: Long): String =
        "${formatPlaybackTime(positionMs)} / ${formatPlaybackTime(durationMs)}"

    fun computeNoteDurationMs(
        segments: List<String>,
        speechRate: Float,
    ): Long = TtsRemainingEstimate.estimateTotalDurationMs(segments, speechRate)

    fun computeNotePositionMs(
        completedMs: Long,
        currentSegmentPositionMs: Long,
        segmentStartElapsedMs: Long,
        segmentEstimateMs: Long,
        isPaused: Boolean,
        frozenPositionMs: Long,
    ): Long {
        if (isPaused) return frozenPositionMs.coerceAtLeast(0L)
        val segmentProgress = if (segmentEstimateMs > 0) {
            segmentStartElapsedMs.coerceAtMost(segmentEstimateMs)
        } else {
            segmentStartElapsedMs
        }
        return (completedMs + currentSegmentPositionMs + segmentProgress).coerceAtLeast(0L)
    }

    fun overallExoPositionMs(
        completedMs: Long,
        currentSegmentPositionMs: Long,
        isPaused: Boolean,
        frozenPositionMs: Long,
    ): Long {
        if (isPaused) return frozenPositionMs.coerceAtLeast(0L)
        return (completedMs + currentSegmentPositionMs).coerceAtLeast(0L)
    }

    fun progressFraction(positionMs: Long, durationMs: Long): Float =
        if (durationMs > 0) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
