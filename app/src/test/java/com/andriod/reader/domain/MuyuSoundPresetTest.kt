package com.andriod.reader.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MuyuSoundPresetTest {
    @Test
    fun fromStored_null_returnsClassicDefault() {
        assertEquals(MuyuSoundPreset.CLASSIC, MuyuSoundPreset.fromStored(null))
    }

    @Test
    fun fromStored_unknown_returnsClassicDefault() {
        assertEquals(MuyuSoundPreset.CLASSIC, MuyuSoundPreset.fromStored("unknown"))
    }

    @Test
    fun fromStored_validKeys() {
        assertEquals(MuyuSoundPreset.BRIGHT, MuyuSoundPreset.fromStored("bright"))
        assertEquals(MuyuSoundPreset.CLASSIC, MuyuSoundPreset.fromStored("classic"))
        assertEquals(MuyuSoundPreset.DEEP, MuyuSoundPreset.fromStored("deep"))
    }
}
