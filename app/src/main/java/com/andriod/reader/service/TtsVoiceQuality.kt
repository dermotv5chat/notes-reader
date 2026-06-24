package com.andriod.reader.service

import android.speech.tts.Voice

/**
 * Heuristic tier for how natural the current system TTS voice likely sounds.
 * Used for in-app guidance only (Phase 0).
 */
enum class TtsVoiceQualityTier {
    MECHANICAL,
    ACCEPTABLE,
    NEURAL_OFFLINE,
    NEURAL_ONLINE,
}

object TtsVoiceQuality {
    fun assess(diagnostics: TtsHelper.TtsDiagnostics): TtsVoiceQualityTier {
        if (diagnostics.isOnlineVoice) return TtsVoiceQualityTier.NEURAL_ONLINE
        val name = diagnostics.voiceName?.lowercase().orEmpty()
        if (name.contains("neural")) return TtsVoiceQualityTier.NEURAL_OFFLINE
        if (diagnostics.isLanguageFallback || diagnostics.chineseVoiceCount == 0) {
            return TtsVoiceQualityTier.MECHANICAL
        }
        if (!diagnostics.isGoogleEngine && diagnostics.voiceQuality != null &&
            diagnostics.voiceQuality < Voice.QUALITY_HIGH
        ) {
            return TtsVoiceQualityTier.MECHANICAL
        }
        return TtsVoiceQualityTier.ACCEPTABLE
    }

    fun qualityGuide(tier: TtsVoiceQualityTier, googleInstalled: Boolean): String = when (tier) {
        TtsVoiceQualityTier.NEURAL_ONLINE ->
            "当前为在线 neural 语音，自然度较好；隧道或无网时会降级，可在朗读设置切回离线。"
        TtsVoiceQualityTier.NEURAL_OFFLINE ->
            "当前为离线 neural 语音，比厂商默认引擎自然；若仍偏机械，可在阅读页选「在线」并联网。"
        TtsVoiceQualityTier.ACCEPTABLE ->
            buildString {
                append("听感一般。建议：")
                if (googleInstalled) {
                    append("阅读页语音偏好选「在线」，并选名称含 neural 的音色；")
                } else {
                    append("安装 Google 文字转语音；")
                }
                append("或在系统 TTS 设置下载中文神经网络语音包。")
            }
        TtsVoiceQualityTier.MECHANICAL ->
            buildString {
                append("当前语音偏机械（多为厂商离线拼接音）。建议：")
                if (!googleInstalled) {
                    append("1) 安装 Google 文字转语音；")
                }
                append("${if (googleInstalled) "1" else "2"}) 系统设置下载中文 neural 语音包；")
                append("${if (googleInstalled) "2" else "3"}) 阅读页偏好选「在线」；")
                append("${if (googleInstalled) "3" else "4"}) 或设置里切换「在线高质量（Edge）」。")
            }
    }

    fun needsQualityHint(tier: TtsVoiceQualityTier): Boolean =
        tier == TtsVoiceQualityTier.MECHANICAL || tier == TtsVoiceQualityTier.ACCEPTABLE
}
