package com.andriod.reader.domain

import java.time.LocalDate
import java.time.ZoneId

object PracticeColorPolicy {
    const val WINDOW_DAYS: Long = 30L

    fun compute(
        mode: PracticeMode,
        repeatPeriod: RepeatPeriod,
        history: List<PracticeLogEntry>,
        today: LocalDate = LocalDate.now(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): PracticeMaturityTier {
        val cutoff = today.minusDays(WINDOW_DAYS)
        val statusEntries = history.filter { entry ->
            entry.event.isStatusEvent() &&
                !entry.recordedAt.atZone(zoneId).toLocalDate().isBefore(cutoff)
        }
        if (statusEntries.isEmpty()) return PracticeMaturityTier.NEUTRAL

        val samples = when (mode) {
            PracticeMode.REPEATLY -> {
                statusEntries
                    .groupBy { entry ->
                        val date = entry.recordedAt.atZone(zoneId).toLocalDate()
                        PracticePeriod.periodKey(date, repeatPeriod)
                    }
                    .values
                    .mapNotNull { periodEntries ->
                        periodEntries.maxByOrNull { it.recordedAt }?.event
                    }
            }
            PracticeMode.WHEN -> statusEntries.map { it.event }
        }
        if (samples.isEmpty()) return PracticeMaturityTier.NEUTRAL

        val totalScore = samples.sumOf { eventScore(it) }
        val ratio = totalScore / samples.size
        return tierFromRatio(ratio)
    }

    private fun eventScore(event: PracticeEvent): Double = when (event) {
        PracticeEvent.FOLLOWED -> 1.0
        PracticeEvent.VIOLATED -> -1.0
        else -> 0.0
    }

    private fun tierFromRatio(ratio: Double): PracticeMaturityTier = when {
        ratio >= 0.55 -> PracticeMaturityTier.GREEN
        ratio >= 0.15 -> PracticeMaturityTier.AMBER
        ratio <= -0.15 -> PracticeMaturityTier.RED
        else -> PracticeMaturityTier.NEUTRAL
    }
}
