package com.andriod.reader.service.synthesis

import android.content.Context
import com.andriod.reader.data.local.AppDiagnosticLog
import com.andriod.reader.data.remote.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

class SherpaModelManager(
    context: Context,
    private val settingsStore: SettingsStore,
    private val diagnosticLog: AppDiagnosticLog,
) {
    private val appContext = context.applicationContext
    private val modelRoot = File(appContext.filesDir, "tts-models")

    fun currentPack(): SherpaModelPack =
        SherpaModelCatalog.packById(settingsStore.getSherpaModelPackId())
            ?: SherpaModelCatalog.defaultPack()

    fun modelDirectory(pack: SherpaModelPack = currentPack()): File = File(modelRoot, pack.dirName)

    fun isPackInstalled(packId: String): Boolean {
        val pack = SherpaModelCatalog.packById(packId) ?: return false
        return isPackInstalled(pack)
    }

    fun isPackInstalled(pack: SherpaModelPack): Boolean {
        val dir = modelDirectory(pack)
        return File(dir, pack.modelFileName).exists() && File(dir, pack.tokensFileName).exists()
    }

    fun isCurrentPackInstalled(): Boolean = isPackInstalled(currentPack())

    fun installedPackIds(): Set<String> =
        SherpaModelCatalog.all.filter { isPackInstalled(it) }.map { it.id }.toSet()

    /** Legacy alias for callers that predate multi-pack support. */
    fun isModelInstalled(): Boolean = isCurrentPackInstalled()

    suspend fun downloadAndInstall(
        packId: String,
        onProgress: suspend (SherpaDownloadProgress) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val pack = SherpaModelCatalog.packById(packId)
            ?: return@withContext Result.failure(IllegalArgumentException("未知语音包：$packId"))
        runCatching {
            onProgress(
                SherpaDownloadProgress(
                    phase = SherpaDownloadPhase.Downloading,
                    message = "正在下载 ${pack.displayName}…",
                ),
            )
            modelRoot.mkdirs()
            val archiveFile = File(appContext.cacheDir, pack.archiveFileName)
            val client = OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            val request = Request.Builder().url(pack.archiveUrl).build()
            onProgress(
                SherpaDownloadProgress(
                    phase = SherpaDownloadPhase.Downloading,
                    message = "正在连接下载服务器…",
                ),
            )
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("下载失败 (${response.code})")
                }
                val body = response.body ?: throw IllegalStateException("下载失败：空响应")
                val totalBytes = body.contentLength().takeIf { it > 0L }
                var bytesRead = 0L
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                body.byteStream().use { input ->
                    archiveFile.outputStream().use { output ->
                        while (true) {
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            bytesRead += read
                            onProgress(
                                SherpaDownloadProgress(
                                    phase = SherpaDownloadPhase.Downloading,
                                    bytesRead = bytesRead,
                                    totalBytes = totalBytes,
                                    message = "正在下载 ${pack.displayName}…",
                                ),
                            )
                        }
                    }
                }
            }
            onProgress(
                SherpaDownloadProgress(
                    phase = SherpaDownloadPhase.Extracting,
                    message = "正在解压…",
                ),
            )
            extractTarBz2(archiveFile, modelRoot)
            archiveFile.delete()
            normalizeModelLayout(pack)
            if (!isPackInstalled(pack)) {
                throw IllegalStateException("解压后未找到模型文件，请重试")
            }
            diagnosticLog.i(
                "SherpaModel",
                "pack ${pack.id} installed at ${modelDirectory(pack).absolutePath}",
            )
            onProgress(
                SherpaDownloadProgress(
                    phase = SherpaDownloadPhase.Idle,
                    message = "${pack.displayName} 已就绪",
                ),
            )
        }
    }

    internal fun normalizeModelLayout(pack: SherpaModelPack) {
        if (isPackInstalled(pack)) return
        val modelFile = modelRoot.walkTopDown()
            .firstOrNull { it.isFile && it.name == pack.modelFileName }
            ?: return
        val sourceDir = modelFile.parentFile ?: return
        val target = modelDirectory(pack)
        if (sourceDir.absolutePath == target.absolutePath) return
        target.mkdirs()
        sourceDir.listFiles()?.forEach { file ->
            val dest = File(target, file.name)
            if (file.isDirectory) {
                file.copyRecursively(dest, overwrite = true)
            } else {
                file.copyTo(dest, overwrite = true)
            }
        }
    }

    fun ruleFstsPath(pack: SherpaModelPack = currentPack()): String {
        val dir = modelDirectory(pack)
        return pack.ruleFsts
            .map { File(dir, it) }
            .filter { it.exists() }
            .joinToString(",") { it.absolutePath }
    }

    fun ruleFarsPath(pack: SherpaModelPack = currentPack()): String {
        if (pack.ruleFars.isBlank()) return ""
        val file = File(modelDirectory(pack), pack.ruleFars)
        return if (file.exists()) file.absolutePath else ""
    }

    private fun extractTarBz2(archive: File, destDir: File) {
        TarArchiveInputStream(
            BZip2CompressorInputStream(BufferedInputStream(archive.inputStream())),
        ).use { tar ->
            var entry = tar.nextTarEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out -> tar.copyTo(out) }
                }
                entry = tar.nextTarEntry
            }
        }
    }

    companion object {
        /** Legacy directory name for the default Melo pack. */
        const val MODEL_DIR_NAME = "vits-melo-tts-zh_en"
    }
}
