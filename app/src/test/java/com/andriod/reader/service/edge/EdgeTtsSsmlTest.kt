package com.andriod.reader.service.edge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeTtsSsmlTest {
    @Test
    fun build_escapesSpecialCharacters() {
        val ssml = EdgeTtsSsml.build(
            text = "A & B <test> \"quote\"",
            voiceId = "zh-CN-XiaoxiaoNeural",
            rate = "+0%",
            pitch = "+0Hz",
        )
        assertTrue(ssml.contains("A &amp; B"))
        assertTrue(ssml.contains("&lt;test&gt;"))
        assertTrue(ssml.contains("zh-CN-XiaoxiaoNeural"))
        assertTrue(ssml.contains("rate='+0%'"))
    }

    @Test
    fun rateOffset_scalesSpeechRate() {
        assertEquals("+50%", EdgeTtsSsml.rateOffset(1.5f))
        assertEquals("-50%", EdgeTtsSsml.rateOffset(0.5f))
    }

    @Test
    fun pitchOffset_scalesSpeechPitch() {
        assertEquals("+10Hz", EdgeTtsSsml.pitchOffset(1.2f))
        assertEquals("-10Hz", EdgeTtsSsml.pitchOffset(0.8f))
    }
}
