package com.andriod.reader.data.local

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.andriod.reader.domain.PracticeEvent
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PracticeLogStoreTest {
    private lateinit var store: PracticeLogStore
    private val fileName = "principles.md"
    private val blockId = "principles.md^sleep11"
    private val today = LocalDate.of(2026, 6, 20)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.filesDir.resolve(".meta/practice-logs.json").delete()
        store = PracticeLogStore(context, Gson())
    }

    @Test
    fun saveTodayEntry_persistsAndReadsBack() {
        store.saveTodayEntry(
            fileName = fileName,
            blockId = blockId,
            event = PracticeEvent.FOLLOWED,
            note = "准时睡",
            date = today,
        )

        val entry = store.getTodayEntry(fileName, blockId, today)
        assertEquals(PracticeEvent.FOLLOWED, entry?.event)
        assertEquals("准时睡", entry?.note)
    }

    @Test
    fun getTodayEntriesForNote_returnsAllBlocksForDate() {
        store.saveTodayEntry(fileName, blockId, PracticeEvent.FOLLOWED, date = today)
        store.saveTodayEntry(fileName, "other-id", PracticeEvent.VIOLATED, date = today)

        val entries = store.getTodayEntriesForNote(fileName, today)
        assertEquals(2, entries.size)
        assertEquals(PracticeEvent.FOLLOWED, entries[blockId]?.event)
        assertEquals(PracticeEvent.VIOLATED, entries["other-id"]?.event)
    }

    @Test
    fun clearTodayEntry_removesRecord() {
        store.saveTodayEntry(fileName, blockId, PracticeEvent.VIOLATED, date = today)
        store.clearTodayEntry(fileName, blockId, today)
        assertNull(store.getTodayEntry(fileName, blockId, today))
    }

    @Test
    fun saveTodayEntry_overwritesSameDay() {
        store.saveTodayEntry(fileName, blockId, PracticeEvent.FOLLOWED, date = today)
        store.saveTodayEntry(fileName, blockId, PracticeEvent.VIOLATED, note = "熬夜", date = today)

        val entry = store.getTodayEntry(fileName, blockId, today)
        assertEquals(PracticeEvent.VIOLATED, entry?.event)
        assertEquals("熬夜", entry?.note)
    }
}
