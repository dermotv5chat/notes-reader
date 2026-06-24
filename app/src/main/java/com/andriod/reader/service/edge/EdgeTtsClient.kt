package com.andriod.reader.service.edge

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Minimal Edge TTS client (WebSocket, read-aloud API used by Microsoft Edge).
 * Protocol aligned with edge-tts: config + ssml sent on open, then binary audio chunks.
 */
class EdgeTtsClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build(),
) {
    suspend fun synthesizeToFile(
        text: String,
        voiceId: String,
        outputFile: File,
        speechRate: Float = 1.0f,
        speechPitch: Float = 1.0f,
    ): File = withContext(Dispatchers.IO) {
        val ssml = EdgeTtsSsml.build(
            text = text,
            voiceId = voiceId,
            rate = EdgeTtsSsml.rateOffset(speechRate),
            pitch = EdgeTtsSsml.pitchOffset(speechPitch),
        )
        val audioBytes = synthesize(ssml)
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(audioBytes)
        outputFile
    }

    private suspend fun synthesize(ssml: String): ByteArray =
        suspendCancellableCoroutine { cont ->
            val connectionId = UUID.randomUUID().toString().replace("-", "")
            val requestId = UUID.randomUUID().toString().replace("-", "")
            val url =
                "wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1" +
                    "?TrustedClientToken=$TRUSTED_CLIENT_TOKEN&ConnectionId=$connectionId"
            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                        "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0",
                )
                .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .build()

            val audioChunks = mutableListOf<ByteArray>()
            val finished = AtomicBoolean(false)

            fun completeSuccess() {
                if (!finished.compareAndSet(false, true)) return
                val combined = audioChunks.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
                if (combined.isEmpty()) {
                    cont.resumeWithException(IllegalStateException("Edge TTS 未返回音频"))
                } else {
                    cont.resume(combined)
                }
            }

            fun completeError(error: Throwable) {
                if (!finished.compareAndSet(false, true)) return
                cont.resumeWithException(error)
            }

            val webSocket = okHttpClient.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        val config = buildString {
                            append("X-Timestamp:${utcTimestamp()}\r\n")
                            append("Content-Type:application/json; charset=utf-8\r\n")
                            append("Path:speech.config\r\n\r\n")
                            append(CONFIG_JSON)
                        }
                        val ssmlMessage = buildString {
                            append("X-RequestId:$requestId\r\n")
                            append("X-Timestamp:${utcTimestamp()}\r\n")
                            append("Content-Type:application/ssml+xml\r\n")
                            append("Path:ssml\r\n\r\n")
                            append(ssml)
                        }
                        webSocket.send(config)
                        webSocket.send(ssmlMessage)
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        if (text.contains("Path:turn.end")) {
                            webSocket.close(1000, "done")
                            completeSuccess()
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                        val raw = bytes.toByteArray()
                        if (raw.size <= 2) return
                        val headerEnd = indexOfHeaderEnd(raw) ?: return
                        if (headerEnd >= raw.size) return
                        val header = String(raw, 0, headerEnd, Charsets.UTF_8)
                        if (header.contains("Path:audio") || header.contains("Content-Type:audio")) {
                            audioChunks += raw.copyOfRange(headerEnd, raw.size)
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        completeError(t)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        if (!finished.get() && audioChunks.isNotEmpty()) {
                            completeSuccess()
                        } else if (!finished.get()) {
                            completeError(IllegalStateException("Edge TTS 连接关闭：$reason"))
                        }
                    }
                },
            )

            cont.invokeOnCancellation {
                webSocket.cancel()
            }
        }

    private fun utcTimestamp(): String {
        val format = SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US)
        format.timeZone = TimeZone.getTimeZone("GMT")
        val formatted = format.format(Date())
        return "$formatted (Coordinated Universal Time)"
    }

    private fun indexOfHeaderEnd(data: ByteArray): Int? {
        val marker = "\r\n\r\n".toByteArray()
        for (i in 0..data.size - marker.size) {
            if (marker.indices.all { data[i + it] == marker[it] }) {
                return i + marker.size
            }
        }
        return null
    }

    companion object {
        private const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68892707A"
        private const val CONFIG_JSON =
            """{"context":{"synthesis":{"audio":{"metadataoptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},"outputFormat":"audio-24khz-48kbitrate-mono-mp3"}}}}"""
    }
}
