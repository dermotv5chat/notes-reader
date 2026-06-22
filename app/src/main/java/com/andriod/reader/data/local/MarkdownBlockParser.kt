package com.andriod.reader.data.local

import com.andriod.reader.domain.NoteBlock

object MarkdownBlockParser {
    private val headingRegex = Regex("""^(#{1,6})\s+(.*)$""")
    private val todoUncheckedRegex = Regex("""^- \[ \] (.*)$""")
    private val todoCheckedRegex = Regex("""^- \[[xX]\] (.*)$""")
    private val orderedRegex = Regex("""^(\d+)\.\s+(.*)$""")
    private val bulletRegex = Regex("""^-\s+(.*)$""")
    private val calloutRegex = Regex("""^>\s*\[!([^\]|]+)(?:\|([^\]]+))?\]\s*(.*)$""")

    fun parse(content: String, fileName: String): List<NoteBlock> =
        content.split('\n').mapIndexed { index, rawLine ->
            parseLine(fileName, index, rawLine)
        }

    internal fun parseLine(fileName: String, lineIndex: Int, rawLine: String): NoteBlock {
        val id = BlockIdResolver.resolve(fileName, lineIndex, rawLine)

        headingRegex.find(rawLine)?.let { match ->
            return NoteBlock.Heading(
                id = id,
                lineIndex = lineIndex,
                rawLine = rawLine,
                level = match.groupValues[1].length,
                text = BlockIdResolver.stripBlockAnchor(match.groupValues[2]),
            )
        }

        todoUncheckedRegex.find(rawLine)?.let { match ->
            return NoteBlock.Todo(
                id = id,
                lineIndex = lineIndex,
                rawLine = rawLine,
                checked = false,
                text = BlockIdResolver.stripBlockAnchor(match.groupValues[1]),
            )
        }

        todoCheckedRegex.find(rawLine)?.let { match ->
            return NoteBlock.Todo(
                id = id,
                lineIndex = lineIndex,
                rawLine = rawLine,
                checked = true,
                text = BlockIdResolver.stripBlockAnchor(match.groupValues[1]),
            )
        }

        calloutRegex.find(rawLine)?.let { match ->
            return NoteBlock.Callout(
                id = id,
                lineIndex = lineIndex,
                rawLine = rawLine,
                variant = match.groupValues[1].lowercase(),
                text = BlockIdResolver.stripBlockAnchor(match.groupValues[3]),
            )
        }

        orderedRegex.find(rawLine)?.let { match ->
            return NoteBlock.Ordered(
                id = id,
                lineIndex = lineIndex,
                rawLine = rawLine,
                number = match.groupValues[1],
                text = BlockIdResolver.stripBlockAnchor(match.groupValues[2]),
            )
        }

        bulletRegex.find(rawLine)?.let { match ->
            return NoteBlock.Bullet(
                id = id,
                lineIndex = lineIndex,
                rawLine = rawLine,
                text = BlockIdResolver.stripBlockAnchor(match.groupValues[1]),
            )
        }

        return NoteBlock.Paragraph(
            id = id,
            lineIndex = lineIndex,
            rawLine = rawLine,
            text = rawLine,
        )
    }
}
