package com.andriod.reader.service.synthesis

import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.service.TtsHelper

/**
 * Pluggable speech synthesis backend (Phase 1 abstraction).
 * System TTS remains in [com.andriod.reader.service.TtsController];
 * online Edge uses [com.andriod.reader.service.OnlineEdgeSpeechBackend].
 */
interface SpeechSynthesisBackend {
    suspend fun prepare(): Boolean
    fun isReady(): Boolean
    fun speak(
        text: String,
        utteranceId: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    )
    fun pause()
    fun resume()
    fun stop()
    fun shutdown()
    fun setSpeechRate(rate: Float)
    fun setPitch(pitch: Float)
    fun listVoiceOptions(): List<TtsVoiceOption>
    fun diagnostics(): TtsHelper.TtsDiagnostics
}
