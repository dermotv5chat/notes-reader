package com.andriod.reader.service.edge

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeTtsAudioFrameTest {
    @Test
    fun extractAudio_readsPayloadAfterHeaderBlock() {
        val header = "Path:audio\r\nContent-Type:audio/mpeg\r\n\r\n".toByteArray(Charsets.UTF_8)
        val audio = byteArrayOf(0xFF.toByte(), 0xFB.toByte(), 0x90.toByte())
        val frame = ByteArray(2 + header.size + audio.size)
        frame[0] = ((header.size shr 8) and 0xFF).toByte()
        frame[1] = (header.size and 0xFF).toByte()
        header.copyInto(frame, destinationOffset = 2)
        audio.copyInto(frame, destinationOffset = 2 + header.size)

        val extracted = EdgeTtsClient().extractAudio(frame)
        assertNotNull(extracted)
        assertTrue(extracted!!.contentEquals(audio))
    }
}
