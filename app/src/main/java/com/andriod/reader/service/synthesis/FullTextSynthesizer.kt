package com.andriod.reader.service.synthesis

import java.io.File

interface FullTextSynthesizer {
    suspend fun synthesizeToFile(
        text: String,
        outputFile: File,
        speechRate: Float,
        speechPitch: Float,
    ): File

    fun isAvailable(): Boolean
}
