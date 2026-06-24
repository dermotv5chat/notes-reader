package com.andriod.reader.service

import android.content.Context
import com.andriod.reader.data.local.AppDiagnosticLog
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.service.synthesis.SherpaFullTextSynthesizer
import com.andriod.reader.service.synthesis.SherpaModelManager
import com.andriod.reader.service.synthesis.SpeechSynthesisBackend
import java.io.File

/**
 * Offline neural speech via Sherpa-onnx (presynth pipeline).
 */
class OfflineSherpaSpeechBackend(
    context: Context,
    private val modelManager: SherpaModelManager,
    private val synthesizer: SherpaFullTextSynthesizer,
    private val diagnosticLog: AppDiagnosticLog,
) : SpeechSynthesisBackend {
    private val appContext = context.applicationContext
    private var speechRate = 1.0f
    private var speechPitch = 1.0f

    fun isModelInstalled(): Boolean = modelManager.isModelInstalled()

    suspend fun awaitReady(): Boolean = modelManager.isModelInstalled()

    override suspend fun prepare(): Boolean = awaitReady()

    override fun isReady(): Boolean = modelManager.isModelInstalled()

    override fun listVoiceOptions(): List<TtsVoiceOption> = listOf(
        TtsVoiceOption(
            id = "sherpa-vits-zh",
            label = "离线 · 中文 VITS",
            isOnline = false,
        ),
    )

    override fun setSpeechRate(rate: Float) {
        speechRate = rate
    }

    override fun setPitch(value: Float) {
        speechPitch = value
    }

    override fun speak(
        text: String,
        utteranceId: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit,
    ) {
        onError("Sherpa 离线引擎请使用整篇预合成播放")
    }

    override suspend fun synthesizeFullText(
        text: String,
        outputFile: File,
        speechRate: Float,
        speechPitch: Float,
    ): File = synthesizer.synthesizeToFile(text, outputFile, speechRate, speechPitch)

    override fun pause() = Unit

    override fun resume() = Unit

    override fun stop() = Unit

    override fun shutdown() {
        synthesizer.release()
    }

    override fun diagnostics(): TtsHelper.TtsDiagnostics {
        val installed = modelManager.isModelInstalled()
        return TtsHelper.TtsDiagnostics(
            enginePackage = "sherpa-onnx",
            engineLabel = "离线高质量（Sherpa）",
            voiceName = "sherpa-vits-zh",
            voiceLocale = "zh-CN",
            voiceQuality = null,
            chineseVoiceCount = if (installed) 1 else 0,
            isGoogleEngine = false,
            googleTtsInstalled = TtsHelper.isGoogleTtsInstalled(appContext),
            isLanguageFallback = false,
            isOnlineVoice = false,
            recommendation = if (installed) {
                "使用 Sherpa 离线 neural；整篇预合成后播放更连贯。"
            } else {
                "请下载离线语音包后使用。"
            },
        )
    }
}
