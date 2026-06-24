package com.andriod.reader.service

import android.content.Context
import com.andriod.reader.data.local.AppDiagnosticLog
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.service.edge.EdgeTtsClient
import com.andriod.reader.service.edge.EdgeTtsVoices
import com.andriod.reader.service.edge.ExoPlayerSpeechPlayer
import com.andriod.reader.service.edge.NetworkAvailability
import com.andriod.reader.service.synthesis.EdgeFullTextSynthesizer
import com.andriod.reader.service.synthesis.SpeechSynthesisBackend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

/**
 * Online neural speech via Microsoft Edge read-aloud API (Phase 1).
 */
class OnlineEdgeSpeechBackend(
    context: Context,
    private val settingsStore: SettingsStore,
    private val diagnosticLog: AppDiagnosticLog,
) : SpeechSynthesisBackend {
    private val appContext = context.applicationContext
    private val client = EdgeTtsClient(diagnosticLog)
    private val fullTextSynthesizer = EdgeFullTextSynthesizer(appContext, settingsStore, diagnosticLog)
    private val player = ExoPlayerSpeechPlayer(appContext, diagnosticLog)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var synthesisJob: Job? = null
    private var speechRate = 1.0f
    private var speechPitch = 1.0f
    private var networkReady = false

    suspend fun awaitReady(): Boolean {
        networkReady = NetworkAvailability.isConnected(appContext)
        diagnosticLog.d("OnlineEdge", "awaitReady networkReady=$networkReady")
        return networkReady
    }

    override suspend fun prepare(): Boolean = awaitReady()

    override fun isReady(): Boolean = networkReady

    override fun listVoiceOptions(): List<TtsVoiceOption> = EdgeTtsVoices.chineseNeural

    fun applySelectedVoice(voiceId: String?) {
        if (voiceId != null && EdgeTtsVoices.isKnownVoice(voiceId)) {
            settingsStore.saveEdgeTtsVoiceId(voiceId)
        }
    }

    override fun setSpeechRate(rate: Float) {
        speechRate = rate
        player.setSpeechRate(rate)
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
        speakInternal(text, utteranceId, onComplete, onError)
    }

    private fun speakInternal(
        text: String,
        utteranceId: String,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (!NetworkAvailability.isConnected(appContext)) {
            diagnosticLog.w("OnlineEdge", "speak blocked: no network utterance=$utteranceId")
            onError("无网络连接，已无法使用在线朗读")
            return
        }
        synthesisJob?.cancel()
        synthesisJob = scope.launch {
            try {
                val voiceId = settingsStore.getEdgeTtsVoiceId()
                val cacheFile = File(appContext.cacheDir, "edge-tts/$utteranceId.mp3")
                diagnosticLog.i(
                    "OnlineEdge",
                    "synthesis start utterance=$utteranceId voice=$voiceId chars=${text.length}",
                )
                withContext(Dispatchers.IO) {
                    withTimeout(SYNTHESIS_TIMEOUT_MS) {
                        client.synthesizeToFile(
                            text = text,
                            voiceId = voiceId,
                            outputFile = cacheFile,
                            speechRate = speechRate,
                            speechPitch = speechPitch,
                        )
                    }
                }
                val size = cacheFile.length()
                if (!cacheFile.exists() || size < 100L) {
                    diagnosticLog.e("OnlineEdge", "synthesis invalid file size=$size utterance=$utteranceId")
                    onError("在线合成失败：未生成有效音频")
                    return@launch
                }
                diagnosticLog.i("OnlineEdge", "synthesis ok utterance=$utteranceId bytes=$size")
                player.play(cacheFile, utteranceId, onDone, onError)
            } catch (e: CancellationException) {
                diagnosticLog.d("OnlineEdge", "synthesis cancelled utterance=$utteranceId")
            } catch (e: Exception) {
                val message = when {
                    e.message?.contains("Timed out", ignoreCase = true) == true ||
                        e.message?.contains("timeout", ignoreCase = true) == true ->
                        "在线合成超时，请检查网络后重试"
                    else -> "在线合成失败：${e.message ?: "未知错误"}"
                }
                diagnosticLog.e("OnlineEdge", message, e)
                onError(message)
            }
        }
    }

    override suspend fun synthesizeFullText(
        text: String,
        outputFile: File,
        speechRate: Float,
        speechPitch: Float,
    ): File = fullTextSynthesizer.synthesizeToFile(text, outputFile, speechRate, speechPitch)

    override fun pause() {
        player.pause()
    }

    override fun resume() {
        player.resume()
    }

    override fun stop() {
        synthesisJob?.cancel()
        synthesisJob = null
        player.stop()
    }

    override fun shutdown() {
        stop()
        player.release()
    }

    fun isPaused(): Boolean = player.isPaused()

    override fun diagnostics(): TtsHelper.TtsDiagnostics {
        val voiceId = settingsStore.getEdgeTtsVoiceId()
        return TtsHelper.TtsDiagnostics(
            enginePackage = "edge-tts",
            engineLabel = "在线高质量（Edge）",
            voiceName = voiceId,
            voiceLocale = "zh-CN",
            voiceQuality = null,
            chineseVoiceCount = EdgeTtsVoices.chineseNeural.size,
            isGoogleEngine = false,
            googleTtsInstalled = TtsHelper.isGoogleTtsInstalled(appContext),
            isLanguageFallback = false,
            isOnlineVoice = true,
            recommendation = if (networkReady) {
                "使用 Edge 在线 neural 语音；无网时将自动回退系统 TTS。"
            } else {
                "当前无网络，在线朗读不可用，将回退系统 TTS。"
            },
        )
    }

    companion object {
        private const val SYNTHESIS_TIMEOUT_MS = 45_000L
    }
}
