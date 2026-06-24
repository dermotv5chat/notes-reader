package com.andriod.reader.data.local

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiagnosticLogExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val diagnosticLog: AppDiagnosticLog,
) {
    fun export(): Result<String> = runCatching {
        val content = buildExportContent()
        if (content.isBlank()) {
            error("暂无日志可导出")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportToDownloads(content)
        } else {
            exportToAppDocuments(content)
        }
    }

    private fun buildExportContent(): String {
        val exportedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val body = diagnosticLog.snapshotText()
        return buildString {
            appendLine("# Andriod Reader diagnostic log")
            appendLine("# Exported: $exportedAt")
            appendLine()
            append(body)
        }
    }

    private fun exportToDownloads(content: String): String {
        val fileName = "andriod-reader-log-${timestamp()}.txt"
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("无法写入 Downloads")
        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(content.toByteArray(Charsets.UTF_8))
        } ?: error("无法打开导出文件")
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return "Downloads/$fileName"
    }

    private fun exportToAppDocuments(content: String): String {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: error("无法访问导出目录")
        dir.mkdirs()
        val file = File(dir, "andriod-reader-log-${timestamp()}.txt")
        file.writeText(content)
        return file.absolutePath
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
}
