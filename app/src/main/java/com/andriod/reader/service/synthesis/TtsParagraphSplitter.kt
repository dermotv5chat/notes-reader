package com.andriod.reader.service.synthesis

/**
 * Splits long text into paragraph-level chunks for presynth (no comma-level splits).
 */
object TtsParagraphSplitter {
    const val DEFAULT_MAX_CHARS = 4000

    fun split(text: String, maxChars: Int = DEFAULT_MAX_CHARS): List<String> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (trimmed.length <= maxChars) return listOf(trimmed)

        val paragraphs = trimmed.split(Regex("\\n\\n+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (paragraphs.isEmpty()) return splitBySentences(trimmed, maxChars)

        val chunks = mutableListOf<String>()
        val buffer = StringBuilder()

        fun flush() {
            if (buffer.isNotEmpty()) {
                chunks.add(buffer.toString().trim())
                buffer.clear()
            }
        }

        for (paragraph in paragraphs) {
            if (paragraph.length <= maxChars) {
                if (buffer.isEmpty()) {
                    buffer.append(paragraph)
                } else if (buffer.length + 2 + paragraph.length <= maxChars) {
                    buffer.append("\n\n").append(paragraph)
                } else {
                    flush()
                    buffer.append(paragraph)
                }
            } else {
                flush()
                chunks.addAll(splitBySentences(paragraph, maxChars))
            }
        }
        flush()
        return chunks.ifEmpty { listOf(trimmed.take(maxChars)) }
    }

    private fun splitBySentences(text: String, maxChars: Int): List<String> {
        val sentences = text.split(Regex("(?<=[。！？；;.!?\\n])"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (sentences.isEmpty()) return listOf(text.take(maxChars))

        val chunks = mutableListOf<String>()
        val buffer = StringBuilder()
        for (sentence in sentences) {
            if (sentence.length > maxChars) {
                if (buffer.isNotEmpty()) {
                    chunks.add(buffer.toString())
                    buffer.clear()
                }
                var start = 0
                while (start < sentence.length) {
                    val end = (start + maxChars).coerceAtMost(sentence.length)
                    chunks.add(sentence.substring(start, end))
                    start = end
                }
            } else if (buffer.isEmpty()) {
                buffer.append(sentence)
            } else if (buffer.length + sentence.length <= maxChars) {
                buffer.append(sentence)
            } else {
                chunks.add(buffer.toString())
                buffer.clear()
                buffer.append(sentence)
            }
        }
        if (buffer.isNotEmpty()) chunks.add(buffer.toString())
        return chunks
    }
}
