package com.andriod.reader.service.synthesis

import android.content.Context
import com.andriod.reader.data.local.AppDiagnosticLog
import com.andriod.reader.data.local.MarkdownPlainText
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.domain.TtsPresynthUiState
import com.andriod.reader.domain.TtsSpeechBackend
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class TtsPreSynthProgress(
    val state: TtsPresynthUiState = TtsPresynthUiState.Hidden,
    val hint: String? = null,
    val chunkProgress: String? = null,
    val charCount: Int = 0,
    val contentHash: String? = null,
)

class TtsPreSynthPipeline(
    context: Context,
    private val settingsStore: SettingsStore,
    private val diagnosticLog: AppDiagnosticLog,
    private val edgeSynthesizer: EdgeFullTextSynthesizer,
    private val sherpaSynthesizer: SherpaFullTextSynthesizer,
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val cacheDir = File(appContext.cacheDir, "tts-presynth").also { it.mkdirs() }

    private val _progress = MutableStateFlow(TtsPreSynthProgress())
    val progress: StateFlow<TtsPreSynthProgress> = _progress.asStateFlow()

    private var prepareJob: Job? = null
    private var currentResult: TtsPreSynthResult? = null
    private var lastPlainText: String? = null
    private var lastContentHash: String? = null
    private var autoPlayAfterReady: Boolean = false
    private var onReadyPlay: (() -> Unit)? = null

    fun usesPresynthBackend(): Boolean {
        val backend = settingsStore.getTtsSpeechBackend()
        return backend == TtsSpeechBackend.ONLINE_EDGE || backend == TtsSpeechBackend.OFFLINE_SHERPA
    }

    fun refreshUiStateForNote(plainText: String) {
        if (!usesPresynthBackend()) {
            _progress.value = TtsPreSynthProgress(state = TtsPresynthUiState.Hidden)
            return
        }
        val hash = contentHash(plainText)
        val charCount = plainText.length
        val cacheKey = cacheKeyFor(plainText)
        val cached = findCachedResult(cacheKey)
        when {
            prepareJob?.isActive == true -> Unit
            cached != null && hash == lastContentHash -> {
                currentResult = cached
                _progress.value = TtsPreSynthProgress(
                    state = TtsPresynthUiState.Ready,
                    hint = "语音已就绪，可直接播放",
                    charCount = charCount,
                    contentHash = hash,
                )
            }
            cached != null && hash != lastContentHash -> {
                currentResult = null
                _progress.value = TtsPreSynthProgress(
                    state = TtsPresynthUiState.Stale,
                    hint = "笔记已修改，请重新生成",
                    charCount = charCount,
                    contentHash = hash,
                )
            }
            currentResult != null && cacheKey == currentResult?.cacheKey -> {
                _progress.value = TtsPreSynthProgress(
                    state = TtsPresynthUiState.Ready,
                    hint = "语音已就绪，可直接播放",
                    charCount = charCount,
                    contentHash = hash,
                )
            }
            else -> {
                currentResult = null
                _progress.value = TtsPreSynthProgress(
                    state = TtsPresynthUiState.NotPrepared,
                    hint = presynthHintForBackend(),
                    charCount = charCount,
                    contentHash = hash,
                )
            }
        }
        lastPlainText = plainText
        lastContentHash = hash
    }

    fun isReady(): Boolean = currentResult != null && _progress.value.state == TtsPresynthUiState.Ready

    fun currentPresynthResult(): TtsPreSynthResult? = currentResult

    fun prepare(
        plainText: String,
        forceRegenerate: Boolean = false,
        autoPlayWhenReady: Boolean = false,
        onAutoPlay: (() -> Unit)? = null,
    ) {
        if (!usesPresynthBackend()) return
        autoPlayAfterReady = autoPlayWhenReady
        onReadyPlay = onAutoPlay
        prepareJob?.cancel()
        prepareJob = scope.launch {
            try {
                val hash = contentHash(plainText)
                val cacheKey = cacheKeyFor(plainText)
                if (!forceRegenerate) {
                    findCachedResult(cacheKey)?.let { cached ->
                        currentResult = cached
                        lastPlainText = plainText
                        lastContentHash = hash
                        _progress.value = TtsPreSynthProgress(
                            state = TtsPresynthUiState.Ready,
                            hint = "语音已就绪，可直接播放",
                            charCount = plainText.length,
                            contentHash = hash,
                        )
                        if (autoPlayAfterReady) onReadyPlay?.invoke()
                        return@launch
                    }
                }
                _progress.value = TtsPreSynthProgress(
                    state = TtsPresynthUiState.Preparing,
                    hint = "正在生成语音…（约 ${plainText.length} 字）",
                    charCount = plainText.length,
                    contentHash = hash,
                )
                val result = synthesizeAll(plainText, cacheKey)
                currentResult = result
                lastPlainText = plainText
                lastContentHash = hash
                _progress.value = TtsPreSynthProgress(
                    state = TtsPresynthUiState.Ready,
                    hint = "语音已就绪，可直接播放",
                    charCount = plainText.length,
                    contentHash = hash,
                )
                diagnosticLog.i("TtsPreSynth", "ready chunks=${result.audioFiles.size} chunked=${result.chunked}")
                if (autoPlayAfterReady) onReadyPlay?.invoke()
            } catch (e: CancellationException) {
                diagnosticLog.d("TtsPreSynth", "cancelled")
                refreshUiStateForNote(plainText)
            } catch (e: Exception) {
                diagnosticLog.e("TtsPreSynth", "failed", e)
                currentResult = null
                _progress.value = TtsPreSynthProgress(
                    state = TtsPresynthUiState.Failed,
                    hint = "生成失败：${e.message ?: "未知错误"}",
                    charCount = plainText.length,
                    contentHash = contentHash(plainText),
                )
            }
        }
    }

    fun cancelPrepare() {
        prepareJob?.cancel()
        prepareJob = null
        lastPlainText?.let { refreshUiStateForNote(it) }
    }

    fun invalidateForContentChange() {
        currentResult = null
        lastPlainText?.let { refreshUiStateForNote(it) }
    }

    fun invalidateForSettingsChange() {
        currentResult = null
        lastPlainText?.let {
            _progress.value = TtsPreSynthProgress(
                state = TtsPresynthUiState.NotPrepared,
                hint = "朗读设置已变更，请重新生成",
                charCount = it.length,
                contentHash = contentHash(it),
            )
        }
    }

    private suspend fun synthesizeAll(plainText: String, cacheKey: String): TtsPreSynthResult =
        withContext(Dispatchers.IO) {
            val synthesizer = synthesizerForBackend()
            if (!synthesizer.isAvailable()) {
                throw IllegalStateException(unavailableMessage())
            }
            val rate = settingsStore.getDefaultSpeechRate()
            val pitch = settingsStore.getDefaultSpeechPitch()
            val chunks = TtsParagraphSplitter.split(plainText)
            if (chunks.isEmpty()) throw IllegalStateException("没有可朗读的正文")

            val outDir = File(cacheDir, cacheKey).also {
                it.deleteRecursively()
                it.mkdirs()
            }
            val ext = extensionForBackend()
            if (chunks.size == 1) {
                val outFile = File(outDir, "whole.$ext")
                synthesizer.synthesizeToFile(chunks.first(), outFile, rate, pitch)
                TtsPreSynthResult.Whole(cacheKey, outFile)
            } else {
                val files = chunks.mapIndexed { index, chunk ->
                    _progress.value = _progress.value.copy(
                        chunkProgress = "${index + 1}/${chunks.size}",
                        hint = "正在生成语音…（第 ${index + 1}/${chunks.size} 段）",
                    )
                    val outFile = File(outDir, "chunk-$index.$ext")
                    synthesizer.synthesizeToFile(chunk, outFile, rate, pitch)
                    outFile
                }
                TtsPreSynthResult.Chunked(cacheKey, files)
            }
        }

    private fun findCachedResult(cacheKey: String): TtsPreSynthResult? {
        val dir = File(cacheDir, cacheKey)
        if (!dir.isDirectory) return null
        val wholeMp3 = File(dir, "whole.mp3")
        val wholeWav = File(dir, "whole.wav")
        when {
            wholeMp3.exists() && wholeMp3.length() > 100 -> return TtsPreSynthResult.Whole(cacheKey, wholeMp3)
            wholeWav.exists() && wholeWav.length() > 100 -> return TtsPreSynthResult.Whole(cacheKey, wholeWav)
        }
        val chunks = dir.listFiles()?.filter { it.name.startsWith("chunk-") && it.length() > 100 }?.sortedBy { it.name }
        if (!chunks.isNullOrEmpty()) {
            return TtsPreSynthResult.Chunked(cacheKey, chunks)
        }
        return null
    }

    private fun synthesizerForBackend(): FullTextSynthesizer = when (settingsStore.getTtsSpeechBackend()) {
        TtsSpeechBackend.ONLINE_EDGE -> edgeSynthesizer
        TtsSpeechBackend.OFFLINE_SHERPA -> sherpaSynthesizer
        TtsSpeechBackend.SYSTEM -> edgeSynthesizer
    }

    private fun extensionForBackend(): String = when (settingsStore.getTtsSpeechBackend()) {
        TtsSpeechBackend.OFFLINE_SHERPA -> "wav"
        else -> "mp3"
    }

    private fun cacheKeyFor(plainText: String): String = TtsPreSynthCacheKey.compute(
        plainText = plainText,
        backend = settingsStore.getTtsSpeechBackend(),
        voiceId = voiceIdForBackend(),
        speechRate = settingsStore.getDefaultSpeechRate(),
        speechPitch = settingsStore.getDefaultSpeechPitch(),
    )

    private fun voiceIdForBackend(): String = when (settingsStore.getTtsSpeechBackend()) {
        TtsSpeechBackend.ONLINE_EDGE -> settingsStore.getEdgeTtsVoiceId()
        TtsSpeechBackend.OFFLINE_SHERPA -> "sherpa-vits-zh"
        TtsSpeechBackend.SYSTEM -> settingsStore.getSelectedVoiceId() ?: "system"
    }

    private fun contentHash(plainText: String): String =
        TtsPreSynthCacheKey.compute(
            plainText = plainText,
            backend = TtsSpeechBackend.SYSTEM,
            voiceId = "content",
            speechRate = 0f,
            speechPitch = 0f,
        ).take(16)

    private fun presynthHintForBackend(): String = when (settingsStore.getTtsSpeechBackend()) {
        TtsSpeechBackend.ONLINE_EDGE -> "整篇预合成，朗读更连贯（需联网生成）"
        TtsSpeechBackend.OFFLINE_SHERPA -> "整篇预合成，朗读更连贯（离线可用）"
        TtsSpeechBackend.SYSTEM -> ""
    }

    private fun unavailableMessage(): String = when (settingsStore.getTtsSpeechBackend()) {
        TtsSpeechBackend.ONLINE_EDGE -> "当前无网络，无法生成在线语音"
        TtsSpeechBackend.OFFLINE_SHERPA -> "请先在设置中下载离线语音包"
        TtsSpeechBackend.SYSTEM -> "系统朗读不可用"
    }

    companion object {
        fun plainTextFromMarkdown(markdown: String): String = MarkdownPlainText.stripForSpeech(markdown)
    }
}
