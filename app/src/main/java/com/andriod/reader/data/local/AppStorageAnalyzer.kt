package com.andriod.reader.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppStorageAnalyzer @Inject constructor(
    @ApplicationContext context: Context,
    private val diagnosticLog: AppDiagnosticLog,
) {
    private val appContext = context.applicationContext

    fun analyze(): StorageBreakdown {
        val filesDir = appContext.filesDir
        val cacheDir = appContext.cacheDir

        val notesDir = File(filesDir, "notes")
        val trashDir = File(filesDir, "trash")
        val metaDir = File(filesDir, ".meta")
        val logsDir = File(filesDir, "logs")
        val edgeTtsDir = File(cacheDir, "edge-tts")

        val notesBytes = directorySize(notesDir)
        val trashBytes = directorySize(trashDir)
        val metaBytes = directorySize(metaDir)
        val logsBytes = maxOf(directorySize(logsDir), diagnosticLog.stats().sizeBytes)
        val edgeTtsBytes = directorySize(edgeTtsDir)

        val knownFilesBytes = notesBytes + trashBytes + metaBytes + logsBytes
        val totalFilesBytes = directorySize(filesDir)
        val otherBytes = (totalFilesBytes - knownFilesBytes).coerceAtLeast(0L)

        val categories = listOf(
            StorageCategory(
                id = StorageCategoryIds.EDGE_TTS_CACHE,
                label = "在线朗读缓存",
                description = "Edge 在线合成 MP3，清理后下次播放会重新下载",
                sizeBytes = edgeTtsBytes,
                cleanable = true,
            ),
            StorageCategory(
                id = StorageCategoryIds.DIAGNOSTIC_LOGS,
                label = "诊断日志",
                description = "应用运行与朗读诊断记录",
                sizeBytes = logsBytes,
                cleanable = true,
            ),
            StorageCategory(
                id = StorageCategoryIds.NOTES,
                label = "笔记正文",
                description = "不可在此清理",
                sizeBytes = notesBytes,
                cleanable = false,
            ),
            StorageCategory(
                id = StorageCategoryIds.TRASH,
                label = "废纸篓",
                description = "不可在此清理",
                sizeBytes = trashBytes,
                cleanable = false,
            ),
            StorageCategory(
                id = StorageCategoryIds.META,
                label = "本机元数据",
                description = "践行记录、同步状态等，不可在此清理",
                sizeBytes = metaBytes,
                cleanable = false,
            ),
            StorageCategory(
                id = StorageCategoryIds.APP_DATA_OTHER,
                label = "其他应用数据",
                description = "不可在此清理",
                sizeBytes = otherBytes,
                cleanable = false,
            ),
        )

        return StorageBreakdown(
            categories = categories,
            totalBytes = totalFilesBytes + directorySize(cacheDir),
        )
    }

    internal fun directorySize(root: File): Long {
        if (!root.exists()) return 0L
        if (root.isFile) return root.length()
        var total = 0L
        root.listFiles()?.forEach { child ->
            total += directorySize(child)
        }
        return total
    }
}
