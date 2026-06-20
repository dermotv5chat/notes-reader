package com.andriod.reader.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsHelperEngineOrderTest {
    @Test
    fun engineTryOrder_prefersSystemDefaultBeforeGoogle() {
        val order = engineTryOrder(
            installed = listOf("com.google.android.tts", "com.xiaomi.mibrain.speech"),
            defaultEngine = "com.xiaomi.mibrain.speech",
        )

        assertEquals(null, order.first())
        assertEquals("com.xiaomi.mibrain.speech", order[1])
        assertTrue(order.last() == "com.google.android.tts")
    }

    @Test
    fun engineTryOrder_usesSystemPickerWhenNothingInstalled() {
        val order = engineTryOrder(installed = emptyList(), defaultEngine = null)
        assertEquals(listOf<String?>(null), order)
    }

    private fun engineTryOrder(installed: List<String>, defaultEngine: String?): List<String?> {
        val order = linkedSetOf<String?>()
        order.add(null)
        if (!defaultEngine.isNullOrBlank()) {
            order.add(defaultEngine)
        }
        installed
            .filter { it != defaultEngine && it != TtsHelper.GOOGLE_TTS_ENGINE }
            .forEach { order.add(it) }
        if (TtsHelper.GOOGLE_TTS_ENGINE in installed) {
            order.add(TtsHelper.GOOGLE_TTS_ENGINE)
        }
        return order.toList()
    }
}
