package com.andriod.reader.data.local

data class StorageCategory(
    val id: String,
    val label: String,
    val description: String,
    val sizeBytes: Long,
    val cleanable: Boolean,
)

data class StorageBreakdown(
    val categories: List<StorageCategory>,
    val totalBytes: Long,
)

object StorageCategoryIds {
    const val EDGE_TTS_CACHE = "edge_tts_cache"
    const val DIAGNOSTIC_LOGS = "diagnostic_logs"
    const val NOTES = "notes"
    const val TRASH = "trash"
    const val META = "meta"
    const val APP_DATA_OTHER = "app_data_other"
}
