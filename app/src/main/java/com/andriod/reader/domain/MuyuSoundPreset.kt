package com.andriod.reader.domain

import androidx.annotation.RawRes
import com.andriod.reader.R

enum class MuyuSoundPreset(
    val storageKey: String,
    @RawRes val rawResId: Int,
    val label: String,
) {
    BRIGHT("bright", R.raw.muyu_bright, "清朗"),
    CLASSIC("classic", R.raw.muyu_classic, "标准"),
    DEEP("deep", R.raw.muyu_deep, "悠远"),
    ;

    companion object {
        val DEFAULT = CLASSIC

        fun fromStored(value: String?): MuyuSoundPreset =
            entries.find { it.storageKey == value } ?: DEFAULT
    }
}
