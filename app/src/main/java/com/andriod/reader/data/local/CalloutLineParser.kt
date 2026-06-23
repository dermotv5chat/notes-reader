package com.andriod.reader.data.local

object CalloutLineParser {
    private val calloutRegex = Regex("""^>\s*\[!([^\]|]+)(?:\|([^\]]+))?\]\s*(.*)$""")

    fun parseCallout(rawLine: String): CalloutKey? {
        val match = calloutRegex.find(rawLine.trim()) ?: return null
        val text = BlockIdResolver.stripBlockAnchor(match.groupValues[3].trim())
        if (text.isBlank()) return null
        return CalloutKey(
            lineIndex = -1,
            variant = match.groupValues[1].lowercase(),
            text = text,
            rawLine = rawLine,
        )
    }

    fun extractCallouts(content: String): List<CalloutKey> {
        return content.split('\n').mapIndexedNotNull { index, rawLine ->
            parseCallout(rawLine)?.copy(lineIndex = index)
        }
    }
}
