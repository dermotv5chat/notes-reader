package com.andriod.reader.service.synthesis

import android.content.Context
import com.andriod.reader.data.local.AppDiagnosticLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class SherpaDownloadUiSnapshot(
    val phase: SherpaDownloadPhase,
    val progress: Float?,
    val bytesLabel: String?,
    val hint: String?,
)

@Singleton
class SherpaModelDownloadCoordinator @Inject constructor(
    @ApplicationContext context: Context,
    private val diagnosticLog: AppDiagnosticLog,
) {
    private val modelManager = SherpaModelManager(context, diagnosticLog)

    fun isInstalled(): Boolean = modelManager.isModelInstalled()

    suspend fun download(
        onProgress: suspend (SherpaDownloadProgress) -> Unit,
    ): Result<Unit> = modelManager.downloadAndInstall { progress ->
        withContext(Dispatchers.Main.immediate) {
            onProgress(progress)
        }
    }

    fun uiSnapshot(progress: SherpaDownloadProgress): SherpaDownloadUiSnapshot {
        val bytesLabel = progress.bytesLabel()
        val hint = formatProgressHint(progress, bytesLabel)
        return SherpaDownloadUiSnapshot(
            phase = progress.phase,
            progress = progress.progressFraction,
            bytesLabel = bytesLabel,
            hint = hint,
        )
    }

    fun formatProgressHint(
        progress: SherpaDownloadProgress,
        bytesLabel: String? = progress.bytesLabel(),
    ): String = when (progress.phase) {
        SherpaDownloadPhase.Downloading -> buildString {
            append(progress.message.ifBlank { "正在下载离线语音包…" })
            bytesLabel?.let { append(" $it") }
        }
        SherpaDownloadPhase.Extracting -> "正在解压…"
        SherpaDownloadPhase.Idle -> progress.message
    }
}
