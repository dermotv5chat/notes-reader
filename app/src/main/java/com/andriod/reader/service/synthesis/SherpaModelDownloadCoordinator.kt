package com.andriod.reader.service.synthesis

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
    private val modelManager: SherpaModelManager,
) {

    @Volatile
    private var activeDownloadPackId: String? = null

    fun currentPack(): SherpaModelPack = modelManager.currentPack()

    fun catalog(): List<SherpaModelPack> = SherpaModelCatalog.all

    fun isPackInstalled(packId: String): Boolean = modelManager.isPackInstalled(packId)

    fun isCurrentPackInstalled(): Boolean = modelManager.isCurrentPackInstalled()

    fun installedPackIds(): Set<String> = modelManager.installedPackIds()

    fun isDownloading(): Boolean = activeDownloadPackId != null

    fun downloadingPackId(): String? = activeDownloadPackId

    /** @deprecated use [isCurrentPackInstalled] */
    fun isInstalled(): Boolean = isCurrentPackInstalled()

    suspend fun download(
        packId: String,
        onProgress: suspend (SherpaDownloadProgress) -> Unit,
    ): Result<Unit> {
        if (activeDownloadPackId != null) {
            return Result.failure(IllegalStateException("已有下载任务进行中"))
        }
        activeDownloadPackId = packId
        return try {
            modelManager.downloadAndInstall(packId) { progress ->
                withContext(Dispatchers.Main.immediate) {
                    onProgress(progress)
                }
            }
        } finally {
            activeDownloadPackId = null
        }
    }

    suspend fun download(
        onProgress: suspend (SherpaDownloadProgress) -> Unit,
    ): Result<Unit> = download(currentPack().id, onProgress)

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
