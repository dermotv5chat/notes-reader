package com.andriod.reader.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TtsVoiceQualityTest {
    @Test
    fun assess_mechanicalWhenLanguageFallback() {
        val tier = TtsVoiceQuality.assess(
            diagnostics(
                isLanguageFallback = true,
                chineseVoiceCount = 0,
            ),
        )
        assertEquals(TtsVoiceQualityTier.MECHANICAL, tier)
    }

    @Test
    fun assess_neuralOnlineWhenOnlineVoice() {
        val tier = TtsVoiceQuality.assess(
            diagnostics(isOnlineVoice = true, voiceName = "zh-CN-XiaoxiaoNeural"),
        )
        assertEquals(TtsVoiceQualityTier.NEURAL_ONLINE, tier)
    }

    @Test
    fun assess_neuralOfflineFromVoiceName() {
        val tier = TtsVoiceQuality.assess(
            diagnostics(voiceName = "zh-cn-xiaoxiao-neural-local"),
        )
        assertEquals(TtsVoiceQualityTier.NEURAL_OFFLINE, tier)
    }

    @Test
    fun needsQualityHint_onlyForLowerTiers() {
        assertTrue(TtsVoiceQuality.needsQualityHint(TtsVoiceQualityTier.MECHANICAL))
        assertTrue(TtsVoiceQuality.needsQualityHint(TtsVoiceQualityTier.ACCEPTABLE))
        assertFalse(TtsVoiceQuality.needsQualityHint(TtsVoiceQualityTier.NEURAL_ONLINE))
    }

    private fun diagnostics(
        voiceName: String? = "zh-cn",
        isOnlineVoice: Boolean = false,
        isLanguageFallback: Boolean = false,
        chineseVoiceCount: Int = 2,
        isGoogleEngine: Boolean = true,
        voiceQuality: Int? = 400,
    ) = TtsHelper.TtsDiagnostics(
        enginePackage = "com.google.android.tts",
        engineLabel = "Google",
        voiceName = voiceName,
        voiceLocale = "zh-CN",
        voiceQuality = voiceQuality,
        chineseVoiceCount = chineseVoiceCount,
        isGoogleEngine = isGoogleEngine,
        googleTtsInstalled = true,
        isLanguageFallback = isLanguageFallback,
        isOnlineVoice = isOnlineVoice,
        recommendation = "",
    )
}
