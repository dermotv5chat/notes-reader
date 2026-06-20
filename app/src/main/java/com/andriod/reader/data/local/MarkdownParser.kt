package com.andriod.reader.data.local

import com.andriod.reader.domain.Note
import com.andriod.reader.domain.SyncStatus
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

object MarkdownParser {
    private val isoFormatter = DateTimeFormatter.ISO_INSTANT

    data class ParsedNote(
        val id: String,
        val title: String,
        val content: String,
        val updatedAt: Instant,
    )

    fun parse(fileName: String, raw: String): ParsedNote {
        val baseName = fileName.substringAfterLast('/').removeSuffix(".md")
        if (!raw.startsWith("---")) {
            return ParsedNote(
                id = baseName,
                title = baseName,
                content = raw.trim(),
                updatedAt = Instant.now(),
            )
        }

        val end = raw.indexOf("\n---", 3)
        if (end < 0) {
            return ParsedNote(
                id = baseName,
                title = baseName,
                content = raw.trim(),
                updatedAt = Instant.now(),
            )
        }

        val frontMatter = raw.substring(3, end).trim()
        val body = raw.substring(end + 4).trimStart('\n', '\r')
        val fields = parseFrontMatter(frontMatter)

        return ParsedNote(
            id = fields["id"] ?: baseName,
            title = fields["title"] ?: baseName,
            content = body,
            updatedAt = fields["updatedAt"]?.let { runCatching { Instant.parse(it) }.getOrNull() }
                ?: Instant.now(),
        )
    }

    fun serialize(note: Note): String {
        val updatedAt = isoFormatter.format(note.updatedAt.atOffset(ZoneOffset.UTC))
        return buildString {
            appendLine("---")
            appendLine("id: \"${note.id}\"")
            appendLine("title: \"${escapeYaml(note.title)}\"")
            appendLine("updatedAt: \"$updatedAt\"")
            appendLine("---")
            if (note.content.isNotEmpty()) {
                append(note.content)
            }
        }
    }

    private fun parseFrontMatter(block: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        block.lineSequence().forEach { line ->
            val idx = line.indexOf(':')
            if (idx > 0) {
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim().removeSurrounding("\"")
                result[key] = value
            }
        }
        return result
    }

    private fun escapeYaml(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"")

    fun newFileName(): String {
        val date = java.time.LocalDate.now().toString()
        val shortId = UUID.randomUUID().toString().substring(0, 8)
        return "$date-$shortId.md"
    }
}
