package com.andriod.reader.service.synthesis

import com.andriod.reader.domain.TtsSpeechBackend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TtsPreSynthCacheKeyTest {
    private val sampleText = "同一段笔记正文"

    @Test
    fun sameInputs_produceSameKey() {
        val a = key(speechRate = 1.0f, speechPitch = 1.0f)
        val b = key(speechRate = 1.0f, speechPitch = 1.0f)
        assertEquals(a, b)
    }

    @Test
    fun differentRate_producesDifferentKey() {
        val a = key(speechRate = 1.0f, speechPitch = 1.0f)
        val b = key(speechRate = 1.2f, speechPitch = 1.0f)
        assertNotEquals(a, b)
    }

    @Test
    fun differentBackend_producesDifferentKey() {
        val edge = TtsPreSynthCacheKey.compute(
            plainText = sampleText,
            backend = TtsSpeechBackend.ONLINE_EDGE,
            voiceId = "voice-a",
            speechRate = 1.0f,
            speechPitch = 1.0f,
        )
        val sherpa = TtsPreSynthCacheKey.compute(
            plainText = sampleText,
            backend = TtsSpeechBackend.OFFLINE_SHERPA,
            voiceId = "voice-a",
            speechRate = 1.0f,
            speechPitch = 1.0f,
        )
        assertNotEquals(edge, sherpa)
    }

    private fun key(speechRate: Float, speechPitch: Float): String =
        TtsPreSynthCacheKey.compute(
            plainText = sampleText,
            backend = TtsSpeechBackend.ONLINE_EDGE,
            voiceId = "zh-CN-XiaoxiaoNeural",
            speechRate = speechRate,
            speechPitch = speechPitch,
        )
}
