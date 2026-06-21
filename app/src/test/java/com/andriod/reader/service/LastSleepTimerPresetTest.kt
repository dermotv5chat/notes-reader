package com.andriod.reader.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LastSleepTimerPresetTest {
    @Test
    fun displaySubtitle_fixedMinutes() {
        assertEquals(
            "45 分钟",
            LastSleepTimerPreset.FixedMinutes(45).displaySubtitle(),
        )
    }

    @Test
    fun displaySubtitle_afterNoteEnd() {
        assertEquals(
            "本篇结束后关闭",
            LastSleepTimerPreset.AfterNoteEnd.displaySubtitle(),
        )
    }

    @Test
    fun fromStored_fixedMinutesWhenTypeMissing() {
        assertEquals(
            LastSleepTimerPreset.FixedMinutes(30),
            LastSleepTimerPreset.fromStored(type = null, minutes = 30),
        )
    }

    @Test
    fun fromStored_afterNoteEnd() {
        assertEquals(
            LastSleepTimerPreset.AfterNoteEnd,
            LastSleepTimerPreset.fromStored(
                type = LastSleepTimerPreset.TYPE_AFTER_NOTE_END,
                minutes = 30,
            ),
        )
    }

    @Test
    fun fromStored_clampsMinutes() {
        assertEquals(
            LastSleepTimerPreset.FixedMinutes(90),
            LastSleepTimerPreset.fromStored(type = LastSleepTimerPreset.TYPE_FIXED, minutes = 120),
        )
    }

    @Test
    fun storedTypeAndMinutes_fixedMinutes() {
        val preset = LastSleepTimerPreset.FixedMinutes(45)
        assertEquals(LastSleepTimerPreset.TYPE_FIXED, LastSleepTimerPreset.storedType(preset))
        assertEquals(45, LastSleepTimerPreset.storedMinutes(preset))
    }

    @Test
    fun storedTypeAndMinutes_afterNoteEnd() {
        assertEquals(
            LastSleepTimerPreset.TYPE_AFTER_NOTE_END,
            LastSleepTimerPreset.storedType(LastSleepTimerPreset.AfterNoteEnd),
        )
        assertNull(LastSleepTimerPreset.storedMinutes(LastSleepTimerPreset.AfterNoteEnd))
    }
}
