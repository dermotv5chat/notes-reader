package com.andriod.reader.data.local

import com.andriod.reader.domain.PracticeMode
import com.andriod.reader.domain.RepeatPeriod

data class CalloutPracticeInfo(
    val mode: PracticeMode,
    val repeatPeriod: RepeatPeriod = RepeatPeriod.DAY,
)

object CalloutCadenceResolver {
    fun resolve(variant: String, modifiers: List<String>): CalloutPracticeInfo {
        val normalized = modifiers.map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (normalized.contains("when") || normalized.contains("always")) {
            return CalloutPracticeInfo(mode = PracticeMode.WHEN)
        }
        val repeatPeriod = when {
            normalized.contains("month") || normalized.contains("monthly") -> RepeatPeriod.MONTH
            normalized.contains("week") || normalized.contains("weekly") -> RepeatPeriod.WEEK
            else -> RepeatPeriod.DAY
        }
        val mode = when {
            variant.equals("habit", ignoreCase = true) -> PracticeMode.REPEATLY
            normalized.contains("daily") -> PracticeMode.REPEATLY
            normalized.contains("week") || normalized.contains("weekly") -> PracticeMode.REPEATLY
            normalized.contains("month") || normalized.contains("monthly") -> PracticeMode.REPEATLY
            else -> PracticeMode.WHEN
        }
        return CalloutPracticeInfo(mode = mode, repeatPeriod = repeatPeriod)
    }

    fun parseModifiers(pipeSuffix: String): List<String> =
        pipeSuffix.trim()
            .split('|')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }

    fun fromRegistry(
        mode: String?,
        repeatPeriod: String?,
        legacyCadence: String?,
    ): CalloutPracticeInfo {
        parseStored(mode, repeatPeriod)?.let { return it }
        return migrateLegacyCadence(legacyCadence)
            ?: CalloutPracticeInfo(mode = PracticeMode.WHEN)
    }

    private fun parseStored(mode: String?, repeatPeriod: String?): CalloutPracticeInfo? {
        val parsedMode = mode?.let { runCatching { PracticeMode.valueOf(it) }.getOrNull() } ?: return null
        val parsedPeriod = repeatPeriod?.let { runCatching { RepeatPeriod.valueOf(it) }.getOrNull() }
            ?: RepeatPeriod.DAY
        return CalloutPracticeInfo(mode = parsedMode, repeatPeriod = parsedPeriod)
    }

    private fun migrateLegacyCadence(legacyCadence: String?): CalloutPracticeInfo? = when (legacyCadence) {
        "DAILY" -> CalloutPracticeInfo(PracticeMode.REPEATLY, RepeatPeriod.DAY)
        "WEEKLY" -> CalloutPracticeInfo(PracticeMode.REPEATLY, RepeatPeriod.WEEK)
        "WHEN" -> CalloutPracticeInfo(PracticeMode.WHEN)
        "ALWAYS" -> CalloutPracticeInfo(PracticeMode.WHEN)
        else -> null
    }
}
