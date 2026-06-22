package com.andriod.reader.data.repository

object SyncUploadPaths {
    /** Try stored remote path first, then fall back to the current local path. */
    fun candidateRemotePaths(storedRemotePath: String?, localPath: String): List<String> {
        val local = SyncPathUtils.normalize(localPath)
        val stored = storedRemotePath?.let { SyncPathUtils.normalize(it) }?.takeIf { it.isNotBlank() }
        return when {
            stored == null -> listOf(local)
            stored == local -> listOf(local)
            else -> listOf(stored, local)
        }
    }
}
