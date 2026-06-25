package com.andriod.reader.data.remote

import com.andriod.reader.domain.MuyuSoundPreset
import com.andriod.reader.ui.settings.SettingsUiState
import com.andriod.reader.ui.settings.feedbackSummaryFor
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsFeedbackSummaryTest {
    @Test
    fun feedbackSummary_bothEnabled_includesPresetLabel() {
        assertEquals(
            "木鱼声开（标准） · 震动开",
            feedbackSummaryFor(
                SettingsUiState(
                    muyuSoundEnabled = true,
                    muyuVibrationEnabled = true,
                    muyuSoundPreset = MuyuSoundPreset.CLASSIC,
                ),
            ),
        )
    }

    @Test
    fun feedbackSummary_deepPreset() {
        assertEquals(
            "木鱼声开（悠远） · 震动开",
            feedbackSummaryFor(
                SettingsUiState(
                    muyuSoundEnabled = true,
                    muyuVibrationEnabled = true,
                    muyuSoundPreset = MuyuSoundPreset.DEEP,
                ),
            ),
        )
    }

    @Test
    fun feedbackSummary_allDisabled() {
        assertEquals(
            "均已关闭",
            feedbackSummaryFor(
                SettingsUiState(muyuSoundEnabled = false, muyuVibrationEnabled = false),
            ),
        )
    }
}
