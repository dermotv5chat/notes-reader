package com.andriod.reader.data.local

object BlockIdResolver {
    private val anchorSuffix = Regex("""\s+\^([a-zA-Z0-9_-]+)\s*$""")

    fun resolve(fileName: String, lineIndex: Int, rawLine: String): String {
        val anchor = anchorSuffix.find(rawLine)?.groupValues?.get(1)
        if (!anchor.isNullOrBlank()) {
            return "$fileName^$anchor"
        }
        val normalized = rawLine.trim().replace(Regex("\\s+"), " ")
        val fingerprint = normalized.hashCode().toUInt().toString(16)
        return "$fileName#$lineIndex#$fingerprint"
    }

    fun stripBlockAnchor(text: String): String =
        text.replace(anchorSuffix, "").trimEnd()
}
