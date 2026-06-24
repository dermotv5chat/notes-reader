package com.andriod.reader.data.local

import com.andriod.reader.domain.PracticeMode
import com.andriod.reader.domain.RepeatPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class CalloutCadenceResolverTest {

    @Test
    fun resolve_habitDefaultsToRepeatlyDay() {
        val info = CalloutCadenceResolver.resolve("habit", emptyList())
        assertEquals(PracticeMode.REPEATLY, info.mode)
        assertEquals(RepeatPeriod.DAY, info.repeatPeriod)
    }

    @Test
    fun resolve_ruleDefaultsToWhen() {
        val info = CalloutCadenceResolver.resolve("rule", emptyList())
        assertEquals(PracticeMode.WHEN, info.mode)
    }

    @Test
    fun resolve_explicitWeekAndMonth() {
        assertEquals(
            RepeatPeriod.WEEK,
            CalloutCadenceResolver.resolve("habit", listOf("week")).repeatPeriod,
        )
        assertEquals(
            RepeatPeriod.MONTH,
            CalloutCadenceResolver.resolve("habit", listOf("month")).repeatPeriod,
        )
    }

    @Test
    fun resolve_alwaysMapsToWhen() {
        assertEquals(
            PracticeMode.WHEN,
            CalloutCadenceResolver.resolve("rule", listOf("always")).mode,
        )
    }

    @Test
    fun parseModifiers_splitsPipeSuffix() {
        assertEquals(
            listOf("daily", "week"),
            CalloutCadenceResolver.parseModifiers("daily|week"),
        )
    }

    @Test
    fun fromRegistry_migratesLegacyCadence() {
        assertEquals(
            CalloutPracticeInfo(PracticeMode.REPEATLY, RepeatPeriod.DAY),
            CalloutCadenceResolver.fromRegistry(null, null, "DAILY"),
        )
        assertEquals(
            CalloutPracticeInfo(PracticeMode.REPEATLY, RepeatPeriod.WEEK),
            CalloutCadenceResolver.fromRegistry(null, null, "WEEKLY"),
        )
        assertEquals(
            CalloutPracticeInfo(PracticeMode.WHEN, RepeatPeriod.DAY),
            CalloutCadenceResolver.fromRegistry(null, null, "WHEN"),
        )
    }

    @Test
    fun parseCalloutLine_extractsWeekModifier() {
        val key = CalloutLineParser.parseCallout("> [!habit|week] 每周复盘")
        requireNotNull(key)
        assertEquals("habit", key.variant)
        assertEquals(listOf("week"), key.modifiers)
        val info = CalloutCadenceResolver.resolve(key.variant, key.modifiers)
        assertEquals(PracticeMode.REPEATLY, info.mode)
        assertEquals(RepeatPeriod.WEEK, info.repeatPeriod)
    }
}
