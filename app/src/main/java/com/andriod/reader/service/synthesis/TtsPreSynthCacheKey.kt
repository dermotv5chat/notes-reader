package com.andriod.reader.service.synthesis

import com.andriod.reader.domain.TtsSpeechBackend
import java.security.MessageDigest

object TtsPreSynthCacheKey {
    fun compute(
        plainText: String,
        backend: TtsSpeechBackend,
        voiceId: String,
        speechRate: Float,
        speechPitch: Float,
    ): String {
        val raw = buildString {
            append(backend.name)
            append('\u0000')
            append(voiceId)
            append('\u0000')
            append(speechRate)
            append('\u0000')
            append(speechPitch)
            append('\u0000')
            append(plainText)
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
