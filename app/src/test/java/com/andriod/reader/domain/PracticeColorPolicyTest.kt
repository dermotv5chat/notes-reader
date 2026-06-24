package com.andriod.reader.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class PracticeColorPolicyTest {

    @Test
    fun compute_whenIdleHistoryStaysNeutral() {
        val tier = PracticeColorPolicy.compute(
            mode = PracticeMode.WHEN,
            repeatPeriod = RepeatPeriod.DAY,
            history = emptyList(),
        )
        assertEquals(PracticeMaturityTier.NEUTRAL, tier)
    }

    @Test
    fun compute_repeatlyDayFollowedTrendsGreen() {
        val today = LocalDate.of(2026, 6, 20)
        val history = (0 until 5).map { offset ->
            PracticeLogEntry(
                event = PracticeEvent.FOLLOWED,
                recordedAt = today.minusDays(offset.toLong()).atTime(12, 0)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant(),
            )
        }
        val tier = PracticeColorPolicy.compute(
            mode = PracticeMode.REPEATLY,
            repeatPeriod = RepeatPeriod.DAY,
            history = history,
            today = today,
        )
        assertEquals(PracticeMaturityTier.GREEN, tier)
    }

    @Test
    fun compute_whenViolationsTrendRed() {
        val today = LocalDate.of(2026, 6, 20)
        val history = listOf(
            entry(PracticeEvent.VIOLATED, today),
            entry(PracticeEvent.VIOLATED, today.minusDays(3)),
            entry(PracticeEvent.FOLLOWED, today.minusDays(5)),
        )
        val tier = PracticeColorPolicy.compute(
            mode = PracticeMode.WHEN,
            repeatPeriod = RepeatPeriod.DAY,
            history = history,
            today = today,
        )
        assertEquals(PracticeMaturityTier.RED, tier)
    }

    private fun entry(event: PracticeEvent, date: LocalDate): PracticeLogEntry =
        PracticeLogEntry(
            event = event,
            recordedAt = date.atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant(),
        )
}
