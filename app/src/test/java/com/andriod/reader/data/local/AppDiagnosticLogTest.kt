package com.andriod.reader.data.local

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class AppDiagnosticLogTest {
    private lateinit var log: AppDiagnosticLog

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.filesDir.resolve("logs/diagnostic.log").delete()
        log = AppDiagnosticLog(context)
        log.clear()
        drainWrites()
    }

    @Test
    fun clear_resetsStats() {
        log.i("Test", "hello")
        drainWrites()
        assertTrue(log.stats().lineCount > 0)

        log.clear()
        drainWrites()

        assertEquals(0, log.stats().lineCount)
        assertEquals(0L, log.stats().sizeBytes)
    }

    @Test
    fun snapshotText_containsLoggedMessage() {
        log.w("TTS", "online synthesis started")
        drainWrites()
        assertTrue(log.snapshotText().contains("online synthesis started"))
        assertTrue(log.snapshotText().contains("[TTS]"))
    }

    @Test
    fun ringBuffer_evictsOldestWhenOverLimit() {
        repeat(800) { index ->
            log.d("Bulk", "line-$index-${"p".repeat(800)}")
        }
        drainWrites()
        val stats = log.stats()
        assertTrue("buffer bytes=${stats.sizeBytes}", stats.sizeBytes <= 512 * 1024)
        assertTrue("buffer lines=${stats.lineCount}", stats.lineCount < 800)
    }

    private fun drainWrites() {
        val latch = CountDownLatch(1)
        Thread {
            Thread.sleep(200)
            latch.countDown()
        }.start()
        latch.await(2, TimeUnit.SECONDS)
    }
}
