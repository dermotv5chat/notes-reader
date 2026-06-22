package com.andriod.reader.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncUploadPathsTest {
    @Test
    fun candidateRemotePaths_usesLocalWhenStoredMissing() {
        assertEquals(
            listOf("content/rules.md"),
            SyncUploadPaths.candidateRemotePaths(storedRemotePath = null, localPath = "content/rules.md"),
        )
    }

    @Test
    fun candidateRemotePaths_prefersStoredThenLocal() {
        assertEquals(
            listOf("notes/rules.md", "content/rules.md"),
            SyncUploadPaths.candidateRemotePaths(
                storedRemotePath = "notes/rules.md",
                localPath = "content/rules.md",
            ),
        )
    }

    @Test
    fun candidateRemotePaths_deduplicatesWhenSame() {
        assertEquals(
            listOf("content/rules.md"),
            SyncUploadPaths.candidateRemotePaths(
                storedRemotePath = "content/rules.md",
                localPath = "content/rules.md",
            ),
        )
    }
}
