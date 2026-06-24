package com.andriod.reader.service.synthesis

enum class SherpaDownloadPhase {
    Idle,
    Downloading,
    Extracting,
}

data class SherpaDownloadProgress(
    val phase: SherpaDownloadPhase,
    val bytesRead: Long = 0L,
    val totalBytes: Long? = null,
    val message: String = "",
) {
    val progressFraction: Float? =
        if (phase == SherpaDownloadPhase.Downloading && totalBytes != null && totalBytes > 0L) {
            (bytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            null
        }

    fun bytesLabel(): String? {
        if (phase != SherpaDownloadPhase.Downloading) return null
        val readMb = bytesRead / (1024.0 * 1024.0)
        return if (totalBytes != null && totalBytes > 0L) {
            val totalMb = totalBytes / (1024.0 * 1024.0)
            val percent = ((bytesRead * 100) / totalBytes).toInt().coerceIn(0, 100)
            "${"%.1f".format(readMb)} / ${"%.1f".format(totalMb)} MB（$percent%）"
        } else {
            "${"%.1f".format(readMb)} MB"
        }
    }
}
