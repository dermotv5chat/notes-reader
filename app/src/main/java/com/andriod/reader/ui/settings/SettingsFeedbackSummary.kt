package com.andriod.reader.ui.settings

internal fun feedbackSummaryFor(state: SettingsUiState): String {
    val sound = if (state.muyuSoundEnabled) {
        "木鱼声开（${state.muyuSoundPreset.label}）"
    } else {
        "木鱼声关"
    }
    val vibration = if (state.muyuVibrationEnabled) "震动开" else "震动关"
    return if (state.muyuSoundEnabled || state.muyuVibrationEnabled) {
        "$sound · $vibration"
    } else {
        "均已关闭"
    }
}
