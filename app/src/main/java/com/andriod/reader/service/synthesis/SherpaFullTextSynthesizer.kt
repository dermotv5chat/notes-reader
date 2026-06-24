package com.andriod.reader.service.synthesis

import android.content.Context
import com.andriod.reader.data.local.AppDiagnosticLog
import com.andriod.reader.data.remote.SettingsStore
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.getOfflineTtsConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class SherpaFullTextSynthesizer(
    context: Context,
    private val modelManager: SherpaModelManager,
    private val settingsStore: SettingsStore,
    private val diagnosticLog: AppDiagnosticLog,
) : FullTextSynthesizer {
    private val engineMutex = Mutex()
    private var engine: OfflineTts? = null
    private var loadedPackId: String? = null

    override fun isAvailable(): Boolean = modelManager.isCurrentPackInstalled()

    override suspend fun synthesizeToFile(
        text: String,
        outputFile: File,
        speechRate: Float,
        speechPitch: Float,
    ): File = withContext(Dispatchers.IO) {
        if (!isAvailable()) throw IllegalStateException("请先在设置中下载离线语音包")
        val pack = modelManager.currentPack()
        val sid = settingsStore.getSherpaSpeakerId().coerceIn(0, pack.speakerCount - 1)
        val tts = ensureEngine(pack)
        val speed = speechRate.coerceIn(0.5f, 2.0f)
        val audio = tts.generateWithConfig(
            text = text.trim(),
            config = GenerationConfig(sid = sid, speed = speed),
        )
        if (audio.samples.isEmpty()) {
            throw IllegalStateException("离线合成未生成音频")
        }
        outputFile.parentFile?.mkdirs()
        val saved = audio.save(outputFile.absolutePath)
        if (!saved || !outputFile.exists()) {
            WavFileWriter.writeMono16BitPcm(audio.samples, audio.sampleRate, outputFile)
        }
        diagnosticLog.i("SherpaSynth", "wrote ${outputFile.length()} bytes pack=${pack.id} sid=$sid")
        outputFile
    }

    private suspend fun ensureEngine(pack: SherpaModelPack): OfflineTts = engineMutex.withLock {
        if (engine != null && loadedPackId == pack.id) {
            return engine!!
        }
        engine?.release()
        engine = null
        loadedPackId = null

        val modelDir = modelManager.modelDirectory(pack)
        val config = getOfflineTtsConfig(
            modelDir = modelDir.absolutePath,
            modelName = pack.modelFileName,
            acousticModelName = "",
            vocoder = "",
            voices = "",
            lexicon = pack.lexiconFileName,
            dataDir = "",
            dictDir = "",
            ruleFsts = modelManager.ruleFstsPath(pack),
            ruleFars = modelManager.ruleFarsPath(pack),
            isKitten = false,
            isSupertonic = false,
            durationPredictor = "",
            textEncoder = "",
            vectorEstimator = "",
            supertonicVocoder = "",
            ttsJson = "",
            unicodeIndexer = "",
            voiceStyle = "",
        ) ?: throw IllegalStateException("无法初始化 Sherpa 离线引擎")
        val tts = OfflineTts(assetManager = null, config = config)
        engine = tts
        loadedPackId = pack.id
        tts
    }

    fun release() {
        engine?.release()
        engine = null
        loadedPackId = null
    }
}
