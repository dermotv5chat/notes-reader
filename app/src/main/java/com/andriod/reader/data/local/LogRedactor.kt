package com.andriod.reader.data.local

/**
 * Redacts sensitive data before writing diagnostic logs.
 */
object LogRedactor {
    private val githubPatClassic = Regex("""ghp_[A-Za-z0-9]+""")
    private val githubPatFine = Regex("""github_pat_[A-Za-z0-9_]+""")
    private val bearerAuth = Regex("""(?i)(Authorization\s*:\s*Bearer\s+)\S+""")
    private val tokenQuery = Regex("""(?i)(token=)[^&\s]+""")

    private const val MAX_MESSAGE_LENGTH = 2_000

    fun redact(message: String): String {
        var result = message
        result = githubPatClassic.replace(result, "ghp_***REDACTED***")
        result = githubPatFine.replace(result, "github_pat_***REDACTED***")
        result = bearerAuth.replace(result, "$1***REDACTED***")
        result = tokenQuery.replace(result, "$1***REDACTED***")
        if (result.length > MAX_MESSAGE_LENGTH) {
            result = result.take(MAX_MESSAGE_LENGTH) + "… [truncated]"
        }
        return result
    }
}
