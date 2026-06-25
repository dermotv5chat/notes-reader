package com.andriod.reader.service

import android.content.Context
import com.andriod.reader.data.local.AppDiagnosticLog
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.domain.TtsVoiceOption
import com.andriod.reader.service.edge.ExoPlayerSpeechPlayer
import com.andriod.reader.service.synthesis.SherpaFullTextSynthesizer
import com.andriod.reader.service.synthesis.SherpaModelManager
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
 * Offline neural speech via Sherpa-onnx (presynth pipeline + segment playback).
 */
class OfflineSherpaSpeechBackend(
    context: Context,
    private val modelManager: SherpaModelManager,
    private val settingsStore: SettingsStore,
    private val synthesizer: SherpaFullTextSynthesizer,
    private val diagnosticLog: AppDiagnosticLog,
) : SpeechSynthesisBackend {
    private val appContext = context.applicationContext
    private val player = ExoPlayerSpeechPlayer(appContext, diagnosticLog)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var synthesisJob: Job? = null
    private var speechRate = 1.0f
    private var speechPitch = 1.0f

    fun isModelInstalled(): Boolean = modelManager.isCurrentPackInstalled()

    suspend fun awaitReady(): Boolean = modelManager.isCurrentPackInstalled()

    override suspend fun prepare(): Boolean = awaitReady()

    override fun isReady(): Boolean = modelManager.isCurrentPackInstalled()

    override fun listVoiceOptions(): List<TtsVoiceOption> {
        val pack = modelManager.currentPack()
        if (!modelManager.isPackInstalled(pack)) return emptyList()
        return if (pack.speakerCount > 1) {
            pack.speakerLabels.mapIndexed { index, label ->
                TtsVoiceOption(
                    id = "sherpa:$index",
                    label = "离线 · $label",
                    isOnline = false,
                )
            }
        } else {
            listOf(
                TtsVoiceOption(
                    id = "sherpa:0",
                    label = "离线 · ${pack.displayName}${pack.genderLabel?.let { " · $it" } ?: ""}",
                    isOnline = false,
                ),
            )
        }
    }

    fun onModelSettingsChanged() {
        synthesizer.release()
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
        if (!isModelInstalled()) {
            onError("请先在设置中下载离线语音包")
            return
        }
        synthesisJob?.cancel()
        synthesisJob = scope.launch {
            try {
                val cacheFile = File(appContext.cacheDir, "sherpa-segment/$utteranceId.wav")
                cacheFile.parentFile?.mkdirs()
                diagnosticLog.i(
                    "OfflineSherpa",
                    "synthesis start utterance=$utteranceId chars=${text.length}",
                )
                withContext(Dispatchers.IO) {
                    withTimeout(SYNTHESIS_TIMEOUT_MS) {
                        synthesizer.synthesizeToFile(text, cacheFile, speechRate, speechPitch)
                    }
                }
                val size = cacheFile.length()
                if (!cacheFile.exists() || size < 100L) {
                    diagnosticLog.e("OfflineSherpa", "synthesis invalid file size=$size utterance=$utteranceId")
                    onError("离线合成失败：未生成有效音频")
                    return@launch
                }
                diagnosticLog.i("OfflineSherpa", "synthesis ok utterance=$utteranceId bytes=$size")
                player.play(cacheFile, utteranceId, onComplete, onError)
            } catch (e: CancellationException) {
                diagnosticLog.d("OfflineSherpa", "synthesis cancelled utterance=$utteranceId")
            } catch (e: Exception) {
                val message = "离线合成失败：${e.message ?: "未知错误"}"
                diagnosticLog.e("OfflineSherpa", message, e)
                onError(message)
            }
        }
    }

    override suspend fun synthesizeFullText(
        text: String,
        outputFile: File,
        speechRate: Float,
        speechPitch: Float,
    ): File = synthesizer.synthesizeToFile(text, outputFile, speechRate, speechPitch)

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
        synthesizer.release()
    }

    fun currentPositionMs(): Long = player.currentPositionMs()

    fun currentDurationMs(): Long = player.durationMs()

    override fun diagnostics(): TtsHelper.TtsDiagnostics {
        val pack = modelManager.currentPack()
        val installed = modelManager.isPackInstalled(pack)
        val sid = settingsStore.getSherpaSpeakerId()
        return TtsHelper.TtsDiagnostics(
            enginePackage = "sherpa-onnx",
            engineLabel = "离线高质量（Sherpa）",
            voiceName = pack.speakerLabel(sid),
            voiceLocale = "zh-CN",
            voiceQuality = null,
            chineseVoiceCount = if (installed) pack.speakerCount else 0,
            isGoogleEngine = false,
            googleTtsInstalled = TtsHelper.isGoogleTtsInstalled(appContext),
            isLanguageFallback = false,
            isOnlineVoice = false,
            recommendation = if (installed) {
                "使用 ${pack.displayName} 离线 neural；整篇预合成后播放更连贯。"
            } else {
                "请下载「${pack.displayName}」语音包后使用。"
            },
        )
    }

    companion object {
        private const val SYNTHESIS_TIMEOUT_MS = 120_000L
    }
}
