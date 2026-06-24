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
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class AppStorageCleanerTest {
    private lateinit var log: AppDiagnosticLog
    private lateinit var analyzer: AppStorageAnalyzer
    private lateinit var cleaner: AppStorageCleaner

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        log = AppDiagnosticLog(context)
        log.clear()
        drainLogWrites()
        analyzer = AppStorageAnalyzer(context, log)
        cleaner = AppStorageCleaner(context, log, analyzer)
    }

    @Test
    fun clean_clearsEdgeTtsAndLogs() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val edgeDir = File(context.cacheDir, "edge-tts").apply { mkdirs() }
        File(edgeDir, "seg-0.mp3").writeBytes(ByteArray(4096))
        log.i("Test", "storage cleaner test")
        drainLogWrites()

        val beforeEdge = analyzer.analyze().categories
            .first { it.id == StorageCategoryIds.EDGE_TTS_CACHE }.sizeBytes
        assertTrue(beforeEdge >= 4096)
        assertTrue(log.stats().lineCount > 0)

        val result = cleaner.clean(
            setOf(StorageCategoryIds.EDGE_TTS_CACHE, StorageCategoryIds.DIAGNOSTIC_LOGS),
        )

        drainLogWrites()
        assertTrue(result.cleanedIds.contains(StorageCategoryIds.EDGE_TTS_CACHE))
        assertTrue(result.cleanedIds.contains(StorageCategoryIds.DIAGNOSTIC_LOGS))
        assertEquals(0L, analyzer.analyze().categories
            .first { it.id == StorageCategoryIds.EDGE_TTS_CACHE }.sizeBytes)
        assertTrue(log.stats().lineCount <= 1)
        assertTrue(result.freedBytes > 0)
    }

    private fun drainLogWrites() {
        val latch = CountDownLatch(1)
        Thread {
            Thread.sleep(200)
            latch.countDown()
        }.start()
        latch.await(2, TimeUnit.SECONDS)
    }
}
