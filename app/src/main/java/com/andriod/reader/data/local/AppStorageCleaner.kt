package com.andriod.reader.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class CleanResult(
    val freedBytes: Long,
    val cleanedIds: Set<String>,
    val failedIds: Set<String>,
)

@Singleton
class AppStorageCleaner @Inject constructor(
    @ApplicationContext context: Context,
    private val diagnosticLog: AppDiagnosticLog,
    private val storageAnalyzer: AppStorageAnalyzer,
) {
    private val appContext = context.applicationContext

    fun clean(selectedIds: Set<String>): CleanResult {
        if (selectedIds.isEmpty()) {
            return CleanResult(freedBytes = 0L, cleanedIds = emptySet(), failedIds = emptySet())
        }

        val before = storageAnalyzer.analyze()
        val beforeById = before.categories.associateBy { it.id }
        val cleaned = mutableSetOf<String>()
        val failed = mutableSetOf<String>()

        if (StorageCategoryIds.EDGE_TTS_CACHE in selectedIds) {
            if (clearEdgeTtsCache()) {
                cleaned += StorageCategoryIds.EDGE_TTS_CACHE
            } else {
                failed += StorageCategoryIds.EDGE_TTS_CACHE
            }
        }

        if (StorageCategoryIds.DIAGNOSTIC_LOGS in selectedIds) {
            runCatching { diagnosticLog.clear() }
                .onSuccess { cleaned += StorageCategoryIds.DIAGNOSTIC_LOGS }
                .onFailure { failed += StorageCategoryIds.DIAGNOSTIC_LOGS }
        }

        val after = storageAnalyzer.analyze()
        val afterById = after.categories.associateBy { it.id }
        val freedBytes = cleaned.sumOf { id ->
            val beforeSize = beforeById[id]?.sizeBytes ?: 0L
            val afterSize = afterById[id]?.sizeBytes ?: 0L
            (beforeSize - afterSize).coerceAtLeast(0L)
        }

        if (cleaned.isNotEmpty()) {
            diagnosticLog.i(
                "Storage",
                "cleaned ids=${cleaned.joinToString()} freed=$freedBytes bytes failed=${failed.joinToString()}",
            )
        }

        return CleanResult(
            freedBytes = freedBytes,
            cleanedIds = cleaned,
            failedIds = failed,
        )
    }

    private fun clearEdgeTtsCache(): Boolean {
        return runCatching {
            val dir = File(appContext.cacheDir, "edge-tts")
            if (!dir.exists()) return true
            dir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
            true
        }.getOrDefault(false)
    }
}
