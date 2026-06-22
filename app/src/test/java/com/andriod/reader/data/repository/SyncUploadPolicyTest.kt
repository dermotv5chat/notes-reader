package com.andriod.reader.data.repository

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SyncUploadPolicyTest {
    @Test
    fun shouldAutoUpload_whenLocalIsNewer() {
        val local = Instant.parse("2026-06-21T10:00:00Z")
        val remote = Instant.parse("2026-06-21T09:00:00Z")
        assertTrue(SyncUploadPolicy.shouldAutoUploadWithFreshSha(local, remote))
    }

    @Test
    fun shouldAutoUpload_whenTimestampsEqual() {
        val time = Instant.parse("2026-06-21T10:00:00Z")
        assertTrue(SyncUploadPolicy.shouldAutoUploadWithFreshSha(time, time))
    }

    @Test
    fun shouldAskUser_whenRemoteIsNewer() {
        val local = Instant.parse("2026-06-21T09:00:00Z")
        val remote = Instant.parse("2026-06-21T10:00:00Z")
        assertFalse(SyncUploadPolicy.shouldAutoUploadWithFreshSha(local, remote))
    }
}
