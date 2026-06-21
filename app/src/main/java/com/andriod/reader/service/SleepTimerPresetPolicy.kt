package com.andriod.reader.service

object SleepTimerPresetPolicy {
    /** Persist last preset when the sleep timer sheet closes with a minute value selected. */
    fun shouldPersistOnSheetClose(roundedMinutes: Int): Boolean = roundedMinutes > 0
}
