package com.andriod.reader.service.synthesis

import android.content.Context
import com.andriod.reader.data.local.AppDiagnosticLog
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
    private val diagnosticLog: AppDiagnosticLog,
) : FullTextSynthesizer {
    private val appContext = context.applicationContext
    private val engineMutex = Mutex()
    private var engine: OfflineTts? = null

    override fun isAvailable(): Boolean = modelManager.isModelInstalled()

    override suspend fun synthesizeToFile(
        text: String,
        outputFile: File,
        speechRate: Float,
        speechPitch: Float,
    ): File = withContext(Dispatchers.IO) {
        if (!isAvailable()) throw IllegalStateException("请先在设置中下载离线语音包")
        val tts = ensureEngine()
        val speed = speechRate.coerceIn(0.5f, 2.0f)
        val audio = tts.generateWithConfig(
            text = text.trim(),
            config = GenerationConfig(sid = 0, speed = speed),
        )
        if (audio.samples.isEmpty()) {
            throw IllegalStateException("离线合成未生成音频")
        }
        outputFile.parentFile?.mkdirs()
        val saved = audio.save(outputFile.absolutePath)
        if (!saved || !outputFile.exists()) {
            WavFileWriter.writeMono16BitPcm(audio.samples, audio.sampleRate, outputFile)
        }
        diagnosticLog.i("SherpaSynth", "wrote ${outputFile.length()} bytes")
        outputFile
    }

    private suspend fun ensureEngine(): OfflineTts = engineMutex.withLock {
        engine?.let { return it }
        val modelDir = modelManager.modelDirectory()
        val config = getOfflineTtsConfig(
            modelDir = modelDir.absolutePath,
            modelName = "model.onnx",
            acousticModelName = "",
            vocoder = "",
            voices = "",
            lexicon = "lexicon.txt",
            dataDir = "",
            dictDir = "",
            ruleFsts = "",
            ruleFars = "",
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
        tts
    }

    fun release() {
        engine?.release()
        engine = null
    }
}
