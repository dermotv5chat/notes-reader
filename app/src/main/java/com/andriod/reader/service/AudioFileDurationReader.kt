package com.andriod.reader.service

import android.media.MediaMetadataRetriever
import java.io.File

object AudioFileDurationReader {
    fun readDurationMs(file: File): Long {
        if (!file.exists() || file.length() <= 0L) return 0L
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            runCatching { retriever.release() }
        }
    }

    fun readDurationsMs(files: List<File>): List<Long> = files.map(::readDurationMs)
}
