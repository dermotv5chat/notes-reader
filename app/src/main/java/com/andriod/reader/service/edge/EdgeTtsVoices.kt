package com.andriod.reader.service.edge

import com.andriod.reader.domain.TtsVoiceOption
import kotlin.math.roundToInt

object EdgeTtsVoices {
    val chineseNeural: List<TtsVoiceOption> = listOf(
        TtsVoiceOption("zh-CN-XiaoxiaoNeural", "在线 · 晓晓", isOnline = true),
        TtsVoiceOption("zh-CN-YunxiNeural", "在线 · 云希", isOnline = true),
        TtsVoiceOption("zh-CN-YunyangNeural", "在线 · 云扬", isOnline = true),
        TtsVoiceOption("zh-CN-XiaoyiNeural", "在线 · 晓伊", isOnline = true),
    )

    fun isKnownVoice(voiceId: String): Boolean = chineseNeural.any { it.id == voiceId }
}

object EdgeTtsSsml {
    fun build(text: String, voiceId: String, rate: String, pitch: String): String {
        val escaped = text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
        return "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='zh-CN'>" +
            "<voice name='$voiceId'>" +
            "<prosody pitch='$pitch' rate='$rate'>$escaped</prosody>" +
            "</voice></speak>"
    }

    /** Edge TTS expects relative rate like +0%, +50%, -25%. */
    fun rateOffset(speechRate: Float): String {
        val percent = ((speechRate - 1.0f) * 100f).toInt().coerceIn(-50, 100)
        return if (percent >= 0) "+$percent%" else "$percent%"
    }

    /** Edge TTS expects relative pitch like +0Hz, +10Hz. */
    fun pitchOffset(speechPitch: Float): String {
        val hz = ((speechPitch - 1.0f) * 50f).roundToInt().coerceIn(-20, 20)
        return if (hz >= 0) "+${hz}Hz" else "${hz}Hz"
    }
}
