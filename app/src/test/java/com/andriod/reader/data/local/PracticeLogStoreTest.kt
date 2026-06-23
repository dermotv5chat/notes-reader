package com.andriod.reader.data.local

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.andriod.reader.domain.PracticeEvent
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PracticeLogStoreTest {
    private lateinit var store: PracticeLogStore
    private val fileName = "principles.md"
    private val blockId = "principles.md^sleep11"
    private val today = LocalDate.of(2026, 6, 20)
    private val zoneId = ZoneId.systemDefault()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.filesDir.resolve(".meta/practice-logs.json").delete()
        store = PracticeLogStore(context, Gson())
    }

    private fun instantOn(date: LocalDate, hour: Int, minute: Int = 0): Instant =
        date.atTime(hour, minute).atZone(zoneId).toInstant()

    @Test
    fun appendEntry_persistsAndReadsBack() {
        store.appendEntry(
            fileName = fileName,
            blockId = blockId,
            event = PracticeEvent.FOLLOWED,
            note = "准时睡",
            recordedAt = instantOn(today, 22),
        )

        val entry = store.getTodayEntry(fileName, blockId, today)
        assertEquals(PracticeEvent.FOLLOWED, entry?.event)
        assertEquals("准时睡", entry?.note)
    }

    @Test
    fun getTodayEntriesForNote_returnsLatestPerBlockForDate() {
        store.appendEntry(
            fileName,
            blockId,
            PracticeEvent.FOLLOWED,
            recordedAt = instantOn(today, 8),
        )
        store.appendEntry(
            fileName,
            blockId,
            PracticeEvent.VIOLATED,
            recordedAt = instantOn(today, 23),
        )
        store.appendEntry(fileName, "other-id", PracticeEvent.VIOLATED, recordedAt = instantOn(today, 12))

        val entries = store.getTodayEntriesForNote(fileName, today)
        assertEquals(2, entries.size)
        assertEquals(PracticeEvent.VIOLATED, entries[blockId]?.event)
        assertEquals(PracticeEvent.VIOLATED, entries["other-id"]?.event)
    }

    @Test
    fun clearTodayEntry_removesAllRecordsForToday() {
        store.appendEntry(fileName, blockId, PracticeEvent.VIOLATED, recordedAt = instantOn(today, 10))
        store.appendEntry(fileName, blockId, PracticeEvent.FOLLOWED, recordedAt = instantOn(today, 22))
        store.clearTodayEntry(fileName, blockId, today)
        assertNull(store.getTodayEntry(fileName, blockId, today))
        assertTrue(store.getHistoryForBlock(fileName, blockId).isEmpty())
    }

    @Test
    fun appendEntry_keepsMultipleSameDayEntries() {
        store.appendEntry(
            fileName,
            blockId,
            PracticeEvent.FOLLOWED,
            note = "早上",
            recordedAt = instantOn(today, 8),
        )
        store.appendEntry(
            fileName,
            blockId,
            PracticeEvent.VIOLATED,
            note = "熬夜",
            recordedAt = instantOn(today, 23),
        )

        val history = store.getHistoryForBlock(fileName, blockId)
        assertEquals(2, history.size)
        assertEquals(PracticeEvent.VIOLATED, history.first().event)
        assertEquals("熬夜", history.first().note)
        assertEquals(PracticeEvent.FOLLOWED, history.last().event)
    }

    @Test
    fun getHistoryForBlock_isSortedNewestFirst() {
        val yesterday = today.minusDays(1)
        store.appendEntry(fileName, blockId, PracticeEvent.FOLLOWED, recordedAt = instantOn(yesterday, 12))
        store.appendEntry(fileName, blockId, PracticeEvent.VIOLATED, recordedAt = instantOn(today, 12))

        val history = store.getHistoryForBlock(fileName, blockId)
        assertEquals(2, history.size)
        assertEquals(PracticeEvent.VIOLATED, history[0].event)
        assertEquals(PracticeEvent.FOLLOWED, history[1].event)
    }

    @Test
    fun migrateBlockId_movesEntriesToNewKey() {
        store.appendEntry(
            fileName,
            "note.md#line:2",
            PracticeEvent.FOLLOWED,
            recordedAt = instantOn(today, 12),
        )
        store.migrateBlockId(fileName, "note.md#line:2", "note.md^b2")
        assertNull(store.getTodayEntry(fileName, "note.md#line:2", today))
        assertEquals(PracticeEvent.FOLLOWED, store.getTodayEntry(fileName, "note.md^b2", today)?.event)
    }

    @Test
    fun readRaw_migratesLegacyDateMapFormat() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val logFile = context.filesDir.resolve(".meta/practice-logs.json")
        logFile.parentFile?.mkdirs()
        logFile.writeText(
            """
            {
              "$fileName": {
                "$blockId": {
                  "2026-06-20": { "event": "FOLLOWED", "note": "legacy" }
                }
              }
            }
            """.trimIndent(),
        )
        val migratedStore = PracticeLogStore(context, Gson())

        val history = migratedStore.getHistoryForBlock(fileName, blockId)
        assertEquals(1, history.size)
        assertEquals(PracticeEvent.FOLLOWED, history.single().event)
        assertEquals("legacy", history.single().note)
        assertTrue(logFile.readText().contains("\"recordedAt\""))
    }
}
