package com.andriod.reader.data.repository

import java.time.Instant

object SyncUploadPolicy {
    /** Push local without prompting when local is at least as new as remote. */
    fun shouldAutoUploadWithFreshSha(localUpdatedAt: Instant, remoteUpdatedAt: Instant): Boolean =
        !remoteUpdatedAt.isAfter(localUpdatedAt)
}
