package com.andriod.reader.service

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TtsSleepTimerTest {
    @Test
    fun setMinutes_doesNotThrowWhenRescheduledRapidly() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        var sessionChangeCount = 0
        val timer = TtsSleepTimer(
            context = context,
            onSessionChanged = { sessionChangeCount++ },
            onExpired = {},
        )

        timer.setMinutes(30) {}
        timer.setMinutes(45) {}
        timer.setMinutes(15) {}
        timer.cancel()

        assertEquals(SleepTimerMode.Off, timer.snapshot().mode)
        assert(sessionChangeCount > 0)
    }
}
