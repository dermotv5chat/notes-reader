package com.andriod.reader.data.local

/**
 * Converts stored Markdown note bodies into plain text suitable for TTS.
 *
 * Rules (keep simple):
 * - Strikethrough `~~text~~` is removed entirely and not spoken.
 * - Other inline markers (bold/italic) are stripped; their text is kept.
 * - Line prefixes (headings, lists, checkboxes, GitHub callouts) are stripped; their text is kept.
 * - Block anchors (`^id` at line end) are stripped.
 */
object MarkdownPlainText {
    private val strikethrough = Regex("""~~([^~]*)~~""")
    private val checkboxPrefix = Regex("""(?m)^- \[[ xX]\] """)
    private val calloutLine = Regex("""(?m)^>\s*\[!([^\]|]+)(?:\|([^\]]+))?\]\s*(.*)$""")
    private val blockquotePrefix = Regex("""(?m)^>\s+""")
    private val headingPrefix = Regex("""(?m)^#{1,6}\s+""")
    private val orderedListPrefix = Regex("""(?m)^\d+\.\s+""")
    private val unorderedListPrefix = Regex("""(?m)^-\s+""")
    private val boldAsterisk = Regex("""\*\*([^*]+)\*\*""")
    private val boldUnderscore = Regex("""__([^_]+)__""")
    private val italicAsterisk = Regex("""(?<!\*)\*([^*]+)\*(?!\*)""")
    private val extraBlankLines = Regex("""\n{3,}""")

    fun stripForSpeech(text: String): String {
        if (text.isBlank()) return ""

        var result = text
        result = result.replace(strikethrough, "")
        result = result.replace(checkboxPrefix, "")
        result = calloutLine.replace(result) { it.groupValues[3] }
        result = result.replace(blockquotePrefix, "")
        result = result.replace(headingPrefix, "")
        result = result.lines().joinToString("\n") { BlockIdResolver.stripBlockAnchor(it) }
        result = result.replace(orderedListPrefix, "")
        result = result.replace(unorderedListPrefix, "")
        result = result.replace(boldAsterisk, "$1")
        result = result.replace(boldUnderscore, "$1")
        result = result.replace(italicAsterisk, "$1")
        result = result.replace(extraBlankLines, "\n\n")
        return result.trim()
    }
}
