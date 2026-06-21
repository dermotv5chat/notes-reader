package com.andriod.reader.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepTimerPresetPolicyTest {
    @Test
    fun shouldPersistOnSheetClose_whenMinutesPositive() {
        assertTrue(SleepTimerPresetPolicy.shouldPersistOnSheetClose(30))
    }

    @Test
    fun shouldNotPersistOnSheetClose_whenOff() {
        assertFalse(SleepTimerPresetPolicy.shouldPersistOnSheetClose(0))
    }
}
