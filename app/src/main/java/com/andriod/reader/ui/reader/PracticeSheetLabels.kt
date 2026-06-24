package com.andriod.reader.ui.reader

import com.andriod.reader.domain.PracticeMode
import com.andriod.reader.domain.RepeatPeriod

object PracticeSheetLabels {
    fun sheetTitle(mode: PracticeMode, repeatPeriod: RepeatPeriod): String = when {
        mode == PracticeMode.WHEN -> "情境践行"
        repeatPeriod == RepeatPeriod.DAY -> "今日践行"
        repeatPeriod == RepeatPeriod.WEEK -> "本周践行"
        repeatPeriod == RepeatPeriod.MONTH -> "本月践行"
        else -> "周期践行"
    }

    fun followedLabel(): String = "遵守"

    fun violatedLabel(): String = "违背"
}
