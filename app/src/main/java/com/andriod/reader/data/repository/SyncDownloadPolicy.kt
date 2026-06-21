package com.andriod.reader.data.repository

import com.andriod.reader.domain.SyncFileState

object SyncDownloadPolicy {
    /** Skip download when the user deleted the note locally (trash) or marked it pending delete. */
    fun shouldSkipRemoteItem(
        localPath: String,
        trashedPaths: Set<String>,
        localState: SyncFileState?,
    ): Boolean {
        val normalized = SyncPathUtils.normalize(localPath)
        if (normalized in trashedPaths) return true
        if (localState?.pendingDelete == true) return true
        return false
    }
}
