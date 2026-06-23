package com.andriod.reader.data.local

object BlockIdResolver {
    private val anchorSuffix = Regex("""\s+\^([a-zA-Z0-9_-]+)\s*$""")
    private val calloutLine = Regex("""^>\s*\[!([^\]|]+)(?:\|([^\]]+))?\]\s*""")

    fun findAnchor(rawLine: String): String? =
        anchorSuffix.find(rawLine.trimEnd())?.groupValues?.get(1)?.takeIf { it.isNotBlank() }

    fun isCalloutLine(rawLine: String): Boolean =
        calloutLine.containsMatchIn(rawLine.trim())

    fun resolve(fileName: String, lineIndex: Int, rawLine: String): String {
        findAnchor(rawLine)?.let { return anchorId(fileName, it) }
        val normalized = rawLine.trim().replace(Regex("\\s+"), " ")
        val fingerprint = normalized.hashCode().toUInt().toString(16)
        return "$fileName#$lineIndex#$fingerprint"
    }

    fun anchorId(fileName: String, anchor: String): String = "$fileName^$anchor"

    fun lineIndexId(fileName: String, lineIndex: Int): String = "$fileName#line:$lineIndex"

    fun stripBlockAnchor(text: String): String =
        text.replace(anchorSuffix, "").trimEnd()
}
