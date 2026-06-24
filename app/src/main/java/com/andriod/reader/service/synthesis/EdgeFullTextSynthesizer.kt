package com.andriod.reader.service.synthesis

import com.andriod.reader.data.local.AppDiagnosticLog
import com.andriod.reader.data.remote.SettingsStore
import com.andriod.reader.service.edge.EdgeTtsClient
import com.andriod.reader.service.edge.NetworkAvailability
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

class EdgeFullTextSynthesizer(
    context: Context,
    private val settingsStore: SettingsStore,
    private val diagnosticLog: AppDiagnosticLog,
) : FullTextSynthesizer {
    private val appContext = context.applicationContext
    private val client = EdgeTtsClient(diagnosticLog)

    override fun isAvailable(): Boolean = NetworkAvailability.isConnected(appContext)

    override suspend fun synthesizeToFile(
        text: String,
        outputFile: File,
        speechRate: Float,
        speechPitch: Float,
    ): File = withContext(Dispatchers.IO) {
        if (!isAvailable()) throw IllegalStateException("当前无网络，无法生成在线语音")
        withTimeout(SYNTHESIS_TIMEOUT_MS) {
            client.synthesizeToFile(
                text = text,
                voiceId = settingsStore.getEdgeTtsVoiceId(),
                outputFile = outputFile,
                speechRate = speechRate,
                speechPitch = speechPitch,
            )
        }
    }

    companion object {
        private const val SYNTHESIS_TIMEOUT_MS = 120_000L
    }
}
