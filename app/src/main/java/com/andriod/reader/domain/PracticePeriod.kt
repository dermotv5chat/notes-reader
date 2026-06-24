package com.andriod.reader.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

object PracticePeriod {
    private val weekFields: WeekFields = WeekFields.of(DayOfWeek.MONDAY, 4)

    fun startOfPeriod(date: LocalDate, period: RepeatPeriod): LocalDate = when (period) {
        RepeatPeriod.DAY -> date
        RepeatPeriod.WEEK -> date.with(weekFields.dayOfWeek(), 1L)
        RepeatPeriod.MONTH -> date.withDayOfMonth(1)
    }

    fun isSamePeriod(a: LocalDate, b: LocalDate, period: RepeatPeriod): Boolean =
        startOfPeriod(a, period) == startOfPeriod(b, period)

    fun periodKey(date: LocalDate, period: RepeatPeriod): String = when (period) {
        RepeatPeriod.DAY -> date.toString()
        RepeatPeriod.WEEK -> {
            val start = startOfPeriod(date, RepeatPeriod.WEEK)
            "${start.year}-W${start.get(weekFields.weekOfWeekBasedYear())}"
        }
        RepeatPeriod.MONTH -> "${date.year}-${date.monthValue.toString().padStart(2, '0')}"
    }

    fun clearConfirmTitle(period: RepeatPeriod): String = when (period) {
        RepeatPeriod.DAY -> "清除今日记录？"
        RepeatPeriod.WEEK -> "清除本周记录？"
        RepeatPeriod.MONTH -> "清除本月记录？"
    }

    fun clearLabel(period: RepeatPeriod): String = when (period) {
        RepeatPeriod.DAY -> "清除今日记录"
        RepeatPeriod.WEEK -> "清除本周记录"
        RepeatPeriod.MONTH -> "清除本月记录"
    }

    fun clearConfirmBody(period: RepeatPeriod): String = when (period) {
        RepeatPeriod.DAY -> "将删除该准则今天的全部践行记录，此操作不可恢复。"
        RepeatPeriod.WEEK -> "将删除该准则本周的全部践行记录，此操作不可恢复。"
        RepeatPeriod.MONTH -> "将删除该准则本月的全部践行记录，此操作不可恢复。"
    }
}
