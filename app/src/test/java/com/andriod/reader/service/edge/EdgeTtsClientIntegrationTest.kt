package com.andriod.reader.service.edge

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/**
 * Live network smoke test for Edge TTS. Fails when API is reachable but synthesis breaks.
 */
class EdgeTtsClientIntegrationTest {
    @Test
    fun synthesizeToFile_returnsNonEmptyMp3() {
        assumeTrue("speech.platform.bing.com unreachable", hasInternet())
        val client = EdgeTtsClient()
        val out = File.createTempFile("edge-tts-it-", ".mp3")
        try {
            runBlocking {
                client.synthesizeToFile(
                    text = "你好，在线高质量语音测试。",
                    voiceId = "zh-CN-XiaoxiaoNeural",
                    outputFile = out,
                )
            }
            assertTrue("expected mp3 bytes, got ${out.length()}", out.length() > 1000)
        } finally {
            out.delete()
        }
    }

    private fun hasInternet(): Boolean {
        return runCatching {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress("speech.platform.bing.com", 443), 5000)
            }
            true
        }.getOrDefault(false)
    }
}
