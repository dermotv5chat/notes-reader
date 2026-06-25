package com.andriod.reader.data.remote

import com.andriod.reader.domain.MuyuSoundPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsStoreMuyuDefaultsTest {
    @Test
    fun muyuFeedbackDefaults_areEnabled() {
        assertTrue(SettingsStore.DEFAULT_MUYU_SOUND_ENABLED)
        assertTrue(SettingsStore.DEFAULT_MUYU_VIBRATION_ENABLED)
    }

    @Test
    fun defaultMuyuSoundPreset_isClassic() {
        assertEquals(MuyuSoundPreset.CLASSIC, MuyuSoundPreset.DEFAULT)
    }
}
