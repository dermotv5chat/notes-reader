package com.andriod.reader.service.synthesis

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsPresynthJobStateLogicTest {
    @Test
    fun preparingJobState_trustedOnlyForActiveFileWhilePipelineRunning() {
        assertTrue(
            isTrustedPreparingJob(
                fileName = "note-a.md",
                activeFileName = "note-a.md",
                isPrepareInProgress = true,
            ),
        )
        assertFalse(
            isTrustedPreparingJob(
                fileName = "note-a.md",
                activeFileName = "note-b.md",
                isPrepareInProgress = true,
            ),
        )
        assertFalse(
            isTrustedPreparingJob(
                fileName = "note-a.md",
                activeFileName = "note-a.md",
                isPrepareInProgress = false,
            ),
        )
    }

    private fun isTrustedPreparingJob(
        fileName: String,
        activeFileName: String?,
        isPrepareInProgress: Boolean,
    ): Boolean = activeFileName == fileName && isPrepareInProgress
}

class TtsPreSynthPipelineRefreshGuardTest {
    @Test
    fun refreshSkippedWhenDifferentNoteIsPreparing() {
        assertTrue(
            shouldSkipRefreshDuringPrepare(
                isPrepareInProgress = true,
                preparingPlainText = "note A body",
                requestedPlainText = "note B body",
            ),
        )
    }

    @Test
    fun refreshSkippedWhenSameNoteIsPreparing() {
        assertTrue(
            shouldSkipRefreshDuringPrepare(
                isPrepareInProgress = true,
                preparingPlainText = "note A body",
                requestedPlainText = "note A body",
            ),
        )
    }

    @Test
    fun refreshAllowedWhenIdle() {
        assertFalse(
            shouldSkipRefreshDuringPrepare(
                isPrepareInProgress = false,
                preparingPlainText = null,
                requestedPlainText = "note B body",
            ),
        )
    }

    private fun shouldSkipRefreshDuringPrepare(
        isPrepareInProgress: Boolean,
        preparingPlainText: String?,
        requestedPlainText: String,
    ): Boolean {
        if (!isPrepareInProgress) return false
        val activePlain = preparingPlainText ?: return false
        if (activePlain != requestedPlainText) return true
        return true
    }
}
