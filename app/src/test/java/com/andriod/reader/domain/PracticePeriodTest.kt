package com.andriod.reader.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PracticePeriodTest {

    @Test
    fun isSamePeriod_day_matchesSameCalendarDay() {
        val a = LocalDate.of(2026, 6, 20)
        val b = LocalDate.of(2026, 6, 20)
        val c = LocalDate.of(2026, 6, 21)
        assertTrue(PracticePeriod.isSamePeriod(a, b, RepeatPeriod.DAY))
        assertFalse(PracticePeriod.isSamePeriod(a, c, RepeatPeriod.DAY))
    }

    @Test
    fun isSamePeriod_week_usesMondayStart() {
        val monday = LocalDate.of(2026, 6, 22)
        val wednesday = LocalDate.of(2026, 6, 24)
        val nextMonday = LocalDate.of(2026, 6, 29)
        assertTrue(PracticePeriod.isSamePeriod(monday, wednesday, RepeatPeriod.WEEK))
        assertFalse(PracticePeriod.isSamePeriod(monday, nextMonday, RepeatPeriod.WEEK))
    }

    @Test
    fun isSamePeriod_month_matchesCalendarMonth() {
        val first = LocalDate.of(2026, 6, 1)
        val last = LocalDate.of(2026, 6, 30)
        val july = LocalDate.of(2026, 7, 1)
        assertTrue(PracticePeriod.isSamePeriod(first, last, RepeatPeriod.MONTH))
        assertFalse(PracticePeriod.isSamePeriod(first, july, RepeatPeriod.MONTH))
    }

    @Test
    fun periodKey_day_isIsoDate() {
        val date = LocalDate.of(2026, 6, 20)
        assertEquals("2026-06-20", PracticePeriod.periodKey(date, RepeatPeriod.DAY))
    }
}
