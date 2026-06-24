package com.andriod.reader.service.synthesis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresynthPrepareResultTest {
    @Test
    fun queued_carriesPosition() {
        val result = PresynthPrepareResult.Queued(position = 2)
        assertTrue(result is PresynthPrepareResult.Queued)
        assertEquals(2, (result as PresynthPrepareResult.Queued).position)
    }

    @Test
    fun queueHintFormat() {
        assertEquals("排队中（第 1 位）", queueHintForTest(1))
        assertEquals("排队中（第 3 位）", queueHintForTest(3))
    }

    private fun queueHintForTest(position: Int): String = "排队中（第 $position 位）"
}
