package com.andriod.reader.data.local

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = android.app.Application::class)
class AppStorageAnalyzerTest {
    private lateinit var analyzer: AppStorageAnalyzer
    private lateinit var log: AppDiagnosticLog

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        log = AppDiagnosticLog(context)
        log.clear()
        analyzer = AppStorageAnalyzer(context, log)
    }

    @Test
    fun analyze_reportsEdgeTtsCacheSize() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val edgeDir = File(context.cacheDir, "edge-tts").apply { mkdirs() }
        File(edgeDir, "seg-0.mp3").writeBytes(ByteArray(2048))

        val category = analyzer.analyze().categories
            .first { it.id == StorageCategoryIds.EDGE_TTS_CACHE }

        assertTrue(category.sizeBytes >= 2048)
        assertTrue(category.cleanable)
    }

    @Test
    fun analyze_marksNotesAsNotCleanable() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val notesDir = File(context.filesDir, "notes").apply { mkdirs() }
        File(notesDir, "test.md").writeText("# hello")

        val category = analyzer.analyze().categories
            .first { it.id == StorageCategoryIds.NOTES }

        assertTrue(category.sizeBytes > 0)
        assertFalse(category.cleanable)
    }
}
