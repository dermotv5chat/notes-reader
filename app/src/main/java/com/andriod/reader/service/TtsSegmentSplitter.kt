package com.andriod.reader.service

object TtsSegmentSplitter {
    fun split(text: String): List<String> {
        return text.split(Regex("(?<=[。！？；;.!?\\n])|(?<=[，,])"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty { listOf(text.trim()) }
            .filter { it.isNotEmpty() }
    }
}
