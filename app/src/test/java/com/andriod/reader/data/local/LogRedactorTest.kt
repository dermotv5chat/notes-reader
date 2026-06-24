package com.andriod.reader.data.local

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogRedactorTest {
    @Test
    fun redact_masksGitHubClassicToken() {
        val result = LogRedactor.redact("Authorization uses ghp_abc123XYZ789secret")
        assertTrue(result.contains("ghp_***REDACTED***"))
        assertFalse(result.contains("abc123XYZ789secret"))
    }

    @Test
    fun redact_masksFineGrainedPat() {
        val result = LogRedactor.redact("github_pat_11AAAAbbbbCCCC")
        assertTrue(result.contains("github_pat_***REDACTED***"))
    }

    @Test
    fun redact_masksAuthorizationHeader() {
        val result = LogRedactor.redact("Authorization: Bearer secret-token-value")
        assertTrue(result.contains("Authorization: Bearer ***REDACTED***"))
    }

    @Test
    fun redact_truncatesVeryLongMessages() {
        val long = "x".repeat(3_000)
        val result = LogRedactor.redact(long)
        assertTrue(result.length < 3_000)
        assertTrue(result.endsWith("… [truncated]"))
    }
}
