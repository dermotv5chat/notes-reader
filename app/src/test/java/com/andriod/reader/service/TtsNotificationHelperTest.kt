package com.andriod.reader.service

import android.app.Application
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TtsNotificationHelperTest {
    @Test
    fun albumArtBitmap_isCachedAcrossCalls() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val first: Bitmap = TtsNotificationHelper.albumArtBitmap(context)
        val second: Bitmap = TtsNotificationHelper.albumArtBitmap(context)
        assertSame(first, second)
    }
}
