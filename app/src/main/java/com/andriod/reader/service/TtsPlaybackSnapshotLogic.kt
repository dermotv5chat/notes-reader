package com.andriod.reader.service

internal object TtsPlaybackSnapshotLogic {
    fun segmentTotal(
        presynthActive: Boolean,
        presynthSegmentTotal: Int,
        segmentsSize: Int,
    ): Int = when {
        presynthActive -> presynthSegmentTotal.coerceAtLeast(1)
        else -> segmentsSize
    }
}
