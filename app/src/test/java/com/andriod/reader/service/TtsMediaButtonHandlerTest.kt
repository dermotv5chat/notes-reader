package com.andriod.reader.service

import android.content.Intent
import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TtsMediaButtonHandlerTest {

    @Test
    fun keyCodeFrom_readsKeyEventParcelable_notIntExtra() {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE))
        }
        assertEquals(KeyEvent.KEYCODE_MEDIA_PAUSE, TtsMediaButtonHandler.keyCodeFrom(intent))
    }

    @Test
    fun keyCodeFrom_ignoresKeyUp() {
        val intent = Intent(Intent.ACTION_MEDIA_BUTTON).apply {
            putExtra(Intent.EXTRA_KEY_EVENT, KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
        }
        assertNull(TtsMediaButtonHandler.keyCodeFrom(intent))
    }

    @Test
    fun keyCodeFrom_returnsNullWhenMissingKeyEvent() {
        assertNull(TtsMediaButtonHandler.keyCodeFrom(Intent(Intent.ACTION_MEDIA_BUTTON)))
    }
}
