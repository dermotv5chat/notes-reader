package com.andriod.reader.service.synthesis

import java.io.File

sealed class TtsPreSynthResult {
    abstract val cacheKey: String
    abstract val audioFiles: List<File>
    abstract val chunked: Boolean

    data class Whole(
        override val cacheKey: String,
        val file: File,
    ) : TtsPreSynthResult() {
        override val audioFiles: List<File> = listOf(file)
        override val chunked: Boolean = false
    }

    data class Chunked(
        override val cacheKey: String,
        override val audioFiles: List<File>,
    ) : TtsPreSynthResult() {
        override val chunked: Boolean = true
    }
}
