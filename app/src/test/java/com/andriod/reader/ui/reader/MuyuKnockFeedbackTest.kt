package com.andriod.reader.ui.reader

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MuyuKnockFeedbackTest {
    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun play_doesNotCrash_whenPreloaded() {
        MuyuKnockFeedback.preload(context)
        MuyuKnockFeedback.play(context)
    }

    @Test
    fun previewSound_doesNotCrash() {
        MuyuKnockFeedback.preload(context)
        MuyuKnockFeedback.previewSound(context)
    }

    @Test
    fun repeatedPlay_doesNotCrash() {
        MuyuKnockFeedback.preload(context)
        MuyuKnockFeedback.play(context)
        MuyuKnockFeedback.previewSound(context)
    }
}
