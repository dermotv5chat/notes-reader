package com.andriod.reader.service.synthesis

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

object WavFileWriter {
    fun writeMono16BitPcm(samples: FloatArray, sampleRate: Int, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        val pcm = ShortArray(samples.size) { i ->
            (samples[i].coerceIn(-1f, 1f) * 32767f).toInt().toShort()
        }
        val dataSize = pcm.size * 2
        RandomAccessFile(outputFile, "rw").use { raf ->
            raf.setLength(0)
            raf.write(buildHeader(dataSize, sampleRate))
            val buffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in pcm) {
                buffer.putShort(sample)
            }
            raf.write(buffer.array())
        }
    }

    private fun buildHeader(dataSize: Int, sampleRate: Int): ByteArray {
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray())
        header.putInt(36 + dataSize)
        header.put("WAVE".toByteArray())
        header.put("fmt ".toByteArray())
        header.putInt(16)
        header.putShort(1)
        header.putShort(1)
        header.putInt(sampleRate)
        header.putInt(sampleRate * 2)
        header.putShort(2)
        header.putShort(16)
        header.put("data".toByteArray())
        header.putInt(dataSize)
        return header.array()
    }
}
