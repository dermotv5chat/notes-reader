package com.andriod.reader.domain

import java.time.Instant

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val fileName: String,
    val updatedAt: Instant,
    val syncStatus: SyncStatus,
)

enum class SyncStatus {
    LOCAL_ONLY,
    SYNCED,
    PENDING,
}

data class SyncFileState(
    val githubSha: String? = null,
    val remotePath: String? = null,
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
    val pendingDelete: Boolean = false,
)

data class GitHubSettings(
    val owner: String = "dermotv5chat",
    val repo: String = "notes",
)

sealed class SyncResult {
    data class Success(val uploaded: Int, val downloaded: Int, val deleted: Int) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

sealed class ConflictAction {
    data object KeepLocal : ConflictAction()
    data object KeepRemote : ConflictAction()
    data class SaveCopy(val newFileName: String) : ConflictAction()
}

data class SyncConflict(
    val fileName: String,
    val localUpdatedAt: Instant,
    val remoteUpdatedAt: Instant,
)

data class TrashEntry(
    val id: String,
    val originalPath: String,
    val deletedAt: Instant,
    val syncState: SyncFileState,
)
