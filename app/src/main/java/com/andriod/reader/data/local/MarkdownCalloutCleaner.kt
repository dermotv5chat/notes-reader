package com.andriod.reader.data.local

object MarkdownCalloutCleaner {
    fun stripLegacyAnchors(content: String): String =
        content.lines().joinToString("\n") { line ->
            if (BlockIdResolver.isCalloutLine(line)) {
                BlockIdResolver.stripBlockAnchor(line.trimEnd())
            } else {
                line
            }
        }
}
