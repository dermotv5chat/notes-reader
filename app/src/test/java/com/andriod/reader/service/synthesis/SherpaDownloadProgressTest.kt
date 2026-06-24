package com.andriod.reader.service.synthesis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SherpaDownloadProgressTest {
    @Test
    fun progressFraction_whenTotalKnown() {
        val progress = SherpaDownloadProgress(
            phase = SherpaDownloadPhase.Downloading,
            bytesRead = 25,
            totalBytes = 100,
        )
        assertEquals(0.25f, progress.progressFraction!!, 0.001f)
    }

    @Test
    fun progressFraction_nullWhenTotalUnknown() {
        val progress = SherpaDownloadProgress(
            phase = SherpaDownloadPhase.Downloading,
            bytesRead = 1024,
            totalBytes = null,
        )
        assertNull(progress.progressFraction)
    }

    @Test
    fun bytesLabel_includesPercentWhenTotalKnown() {
        val progress = SherpaDownloadProgress(
            phase = SherpaDownloadPhase.Downloading,
            bytesRead = 25L * 1024 * 1024,
            totalBytes = 100L * 1024 * 1024,
        )
        assertEquals(true, progress.bytesLabel()?.contains("25%") == true)
    }
}
