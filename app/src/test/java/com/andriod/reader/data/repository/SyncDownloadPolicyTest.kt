package com.andriod.reader.data.repository

import com.andriod.reader.domain.SyncFileState
import com.andriod.reader.domain.SyncStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDownloadPolicyTest {
    @Test
    fun shouldSkipRemoteItem_whenPathIsInTrash() {
        assertTrue(
            SyncDownloadPolicy.shouldSkipRemoteItem(
                localPath = "work/note.md",
                trashedPaths = setOf("work/note.md"),
                localState = null,
            ),
        )
    }

    @Test
    fun shouldSkipRemoteItem_whenPendingDelete() {
        assertTrue(
            SyncDownloadPolicy.shouldSkipRemoteItem(
                localPath = "note.md",
                trashedPaths = emptySet(),
                localState = SyncFileState(
                    githubSha = "sha",
                    syncStatus = SyncStatus.PENDING,
                    pendingDelete = true,
                ),
            ),
        )
    }

    @Test
    fun shouldNotSkipRemoteItem_forNormalDownload() {
        assertFalse(
            SyncDownloadPolicy.shouldSkipRemoteItem(
                localPath = "new.md",
                trashedPaths = emptySet(),
                localState = null,
            ),
        )
    }
}
