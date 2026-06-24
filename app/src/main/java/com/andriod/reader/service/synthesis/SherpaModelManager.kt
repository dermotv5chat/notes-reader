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
        onProgress: (String) -> Unit = {},
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            onProgress("正在下载离线语音包…")
            modelRoot.mkdirs()
            val archiveFile = File(appContext.cacheDir, "sherpa-vits-zh.tar.bz2")
            val client = OkHttpClient.Builder().build()
            val request = Request.Builder().url(MODEL_ARCHIVE_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("下载失败 (${response.code})")
                }
                val body = response.body ?: throw IllegalStateException("下载失败：空响应")
                archiveFile.outputStream().use { out -> body.byteStream().copyTo(out) }
            }
            onProgress("正在解压…")
            extractTarBz2(archiveFile, modelRoot)
            archiveFile.delete()
            if (!isModelInstalled()) {
                throw IllegalStateException("解压后未找到 model.onnx，请重试")
            }
            diagnosticLog.i("SherpaModel", "model installed at ${modelDirectory().absolutePath}")
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
