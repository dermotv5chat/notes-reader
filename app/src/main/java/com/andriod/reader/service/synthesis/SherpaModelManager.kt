package com.andriod.reader.service.synthesis

import android.content.Context
import com.andriod.reader.data.local.AppDiagnosticLog
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
    private val diagnosticLog: AppDiagnosticLog,
) {
    private val appContext = context.applicationContext
    private val modelRoot = File(appContext.filesDir, "tts-models")

    fun modelDirectory(): File = File(modelRoot, MODEL_DIR_NAME)

    fun isModelInstalled(): Boolean {
        val dir = modelDirectory()
        return File(dir, "model.onnx").exists() && File(dir, "tokens.txt").exists()
    }

    suspend fun downloadAndInstall(
        onProgress: suspend (SherpaDownloadProgress) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress(
                SherpaDownloadProgress(
                    phase = SherpaDownloadPhase.Downloading,
                    message = "正在下载离线语音包…",
                ),
            )
            modelRoot.mkdirs()
            val archiveFile = File(appContext.cacheDir, "sherpa-vits-zh.tar.bz2")
            val client = OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build()
            val request = Request.Builder().url(MODEL_ARCHIVE_URL).build()
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
                                    message = "正在下载离线语音包…",
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
            normalizeModelLayout()
            if (!isModelInstalled()) {
                throw IllegalStateException("解压后未找到 model.onnx，请重试")
            }
            diagnosticLog.i("SherpaModel", "model installed at ${modelDirectory().absolutePath}")
            onProgress(
                SherpaDownloadProgress(
                    phase = SherpaDownloadPhase.Idle,
                    message = "离线语音包已就绪",
                ),
            )
        }
    }

    internal fun normalizeModelLayout() {
        if (isModelInstalled()) return
        val modelOnnx = modelRoot.walkTopDown()
            .firstOrNull { it.isFile && it.name == "model.onnx" }
            ?: return
        val sourceDir = modelOnnx.parentFile ?: return
        val target = modelDirectory()
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
        const val MODEL_DIR_NAME = "vits-melo-tts-zh_en"
        const val MODEL_ARCHIVE_URL =
            "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-melo-tts-zh_en.tar.bz2"
    }
}
