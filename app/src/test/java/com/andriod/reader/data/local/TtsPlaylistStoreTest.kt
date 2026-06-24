package com.andriod.reader.data.local

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.andriod.reader.domain.TtsPlaylistItem
import com.andriod.reader.domain.TtsQueueRepeatMode
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TtsPlaylistStoreTest {
    private lateinit var store: TtsPlaylistStore

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.filesDir.resolve(".meta/tts-playlist.json").delete()
        store = TtsPlaylistStore(context, Gson())
    }

    @Test
    fun writeAndRead_persistsItemsAndRepeatMode() {
        val snapshot = TtsPlaylistSnapshot(
            items = listOf(
                TtsPlaylistItem("note.md", "标题", Instant.parse("2026-06-20T10:00:00Z")),
            ),
            repeatMode = TtsQueueRepeatMode.REPEAT_ONE,
        )
        store.write(snapshot)

        val read = store.read()
        assertEquals(TtsQueueRepeatMode.REPEAT_ONE, read.repeatMode)
        assertEquals(1, read.items.size)
        assertEquals("note.md", read.items.first().fileName)
        assertEquals("标题", read.items.first().title)
    }

    @Test
    fun writeEmptyOff_deletesFile() {
        store.write(
            TtsPlaylistSnapshot(
                items = listOf(TtsPlaylistItem("a.md", "A")),
                repeatMode = TtsQueueRepeatMode.OFF,
            ),
        )
        store.write(TtsPlaylistSnapshot())
        assertEquals(TtsPlaylistSnapshot(), store.read())
    }
}
