package com.andriod.reader.service.edge

import com.andriod.reader.data.local.AppDiagnosticLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Edge TTS WebSocket client aligned with edge-tts (Sec-MS-GEC, binary audio framing).
 */
class EdgeTtsClient(
    private val diagnosticLog: AppDiagnosticLog? = null,
    private val okHttpClient: OkHttpClient = EdgeTtsSecMsGec.defaultHttpClient(),
) {
    suspend fun synthesizeToFile(
        text: String,
        voiceId: String,
        outputFile: File,
        speechRate: Float = 1.0f,
        speechPitch: Float = 1.0f,
    ): File = withContext(Dispatchers.IO) {
        EdgeTtsSecMsGec.ensureClockSynced(okHttpClient)
        val cleaned = removeIncompatibleCharacters(text)
        val ssml = EdgeTtsSsml.build(
            text = cleaned,
            voiceId = voiceId,
            rate = EdgeTtsSsml.rateOffset(speechRate),
            pitch = EdgeTtsSsml.pitchOffset(speechPitch),
        )
        val audioBytes = synthesizeWithRetry(ssml)
        outputFile.parentFile?.mkdirs()
        outputFile.writeBytes(audioBytes)
        outputFile
    }

    private suspend fun synthesizeWithRetry(ssml: String): ByteArray {
        var lastError: Exception? = null
        for (attempt in 0 until MAX_ATTEMPTS) {
            try {
                return withTimeout(SYNTHESIS_TIMEOUT_MS) {
                    synthesizeOnce(ssml)
                }
            } catch (e: Exception) {
                lastError = e
                val is403 = e.message?.contains("403") == true
                diagnosticLog?.w("EdgeTts", "synthesis attempt ${attempt + 1} failed: ${e.message}")
                if (is403 && attempt < MAX_ATTEMPTS - 1) {
                    EdgeTtsSecMsGec.resyncClockForRetry(okHttpClient)
                    continue
                }
                throw e
            }
        }
        throw lastError ?: IllegalStateException("Edge TTS 合成失败")
    }

    private suspend fun synthesizeOnce(ssml: String): ByteArray =
        suspendCancellableCoroutine { cont ->
            val connectionId = uuidNoDash()
            val requestId = uuidNoDash()
            val secMsGec = EdgeTtsSecMsGec.generate()
            val url = buildString {
                append("wss://speech.platform.bing.com/consumer/speech/synthesize/readaloud/edge/v1")
                append("?TrustedClientToken=${EdgeTtsSecMsGec.TRUSTED_CLIENT_TOKEN}")
                append("&ConnectionId=$connectionId")
                append("&Sec-MS-GEC=$secMsGec")
                append("&Sec-MS-GEC-Version=${EdgeTtsSecMsGec.SEC_MS_GEC_VERSION}")
            }
            val muid = EdgeTtsSecMsGec.randomMuid()
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", EdgeTtsSecMsGec.USER_AGENT)
                .header("Accept-Encoding", "gzip, deflate, br, zstd")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Origin", "chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold")
                .header("Pragma", "no-cache")
                .header("Cache-Control", "no-cache")
                .header("Cookie", "muid=$muid;")
                .build()

            val audioChunks = mutableListOf<ByteArray>()
            val finished = AtomicBoolean(false)
            val receivedAudio = AtomicBoolean(false)
            var audioChunkCount = 0

            fun completeSuccess() {
                if (!finished.compareAndSet(false, true)) return
                val combined = audioChunks.fold(ByteArray(0)) { acc, bytes -> acc + bytes }
                diagnosticLog?.i(
                    "EdgeTts",
                    "synthesis complete chunks=$audioChunkCount bytes=${combined.size}",
                )
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
                        diagnosticLog?.d("EdgeTts", "websocket open code=${response.code}")
                        val timestamp = edgeTimestamp()
                        val config = buildString {
                            append("X-Timestamp:$timestamp\r\n")
                            append("Content-Type:application/json; charset=utf-8\r\n")
                            append("Path:speech.config\r\n\r\n")
                            append(CONFIG_JSON)
                            append("\r\n")
                        }
                        val ssmlMessage = buildString {
                            append("X-RequestId:$requestId\r\n")
                            append("Content-Type:application/ssml+xml\r\n")
                            append("X-Timestamp:${timestamp}Z\r\n")
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
                        extractAudio(bytes.toByteArray())?.let { chunk ->
                            if (chunk.isNotEmpty()) {
                                receivedAudio.set(true)
                                audioChunkCount++
                                audioChunks += chunk
                            }
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        response?.header("Date")?.let { EdgeTtsSecMsGec.syncClockSkewFromDateHeader(it) }
                        val message = when (response?.code) {
                            403 -> "Edge TTS 鉴权失败(403)，请检查网络或稍后重试"
                            else -> t.message ?: "WebSocket 连接失败"
                        }
                        diagnosticLog?.e("EdgeTts", "websocket failure code=${response?.code} $message", t)
                        completeError(IllegalStateException(message, t))
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        when {
                            finished.get() -> Unit
                            receivedAudio.get() -> completeSuccess()
                            else -> completeError(IllegalStateException("Edge TTS 连接关闭：$reason"))
                        }
                    }
                },
            )

            cont.invokeOnCancellation {
                webSocket.cancel()
            }
        }

    /** Parse Edge binary audio frame (supports both header layout variants). */
    internal fun extractAudio(raw: ByteArray): ByteArray? {
        if (raw.size < 2) return null
        val headerLength = ((raw[0].toInt() and 0xFF) shl 8) or (raw[1].toInt() and 0xFF)
        if (headerLength <= 0 || headerLength > raw.size) return null

        // Variant A: 2-byte length + header text + audio (most ports).
        if (raw.size >= 2 + headerLength) {
            val headerA = String(raw, 2, headerLength, Charsets.UTF_8)
            if (headerA.contains("Path:audio")) {
                val start = 2 + headerLength
                return if (start < raw.size) raw.copyOfRange(start, raw.size) else ByteArray(0)
            }
        }

        // Variant B: edge-tts python layout (headerLength includes 2-byte prefix).
        val headerB = String(raw, 0, headerLength.coerceAtMost(raw.size), Charsets.UTF_8)
        if (headerB.contains("Path:audio")) {
            val start = headerLength + 2
            return if (start < raw.size) raw.copyOfRange(start, raw.size) else ByteArray(0)
        }
        return null
    }

    private fun edgeTimestamp(): String {
        val formatter = DateTimeFormatter.ofPattern(
            "EEE MMM dd yyyy HH:mm:ss 'GMT+0000 (Coordinated Universal Time)'",
            Locale.US,
        )
        return formatter.format(Instant.now().atZone(ZoneOffset.UTC))
    }

    private fun uuidNoDash(): String = UUID.randomUUID().toString().replace("-", "")

    private fun removeIncompatibleCharacters(text: String): String {
        return buildString(text.length) {
            for (ch in text) {
                val code = ch.code
                append(
                    when {
                        code in 0..8 || code in 11..12 || code in 14..31 -> ' '
                        else -> ch
                    },
                )
            }
        }
    }

    companion object {
        private const val MAX_ATTEMPTS = 3
        private const val SYNTHESIS_TIMEOUT_MS = 40_000L
        private const val CONFIG_JSON =
            """{"context":{"synthesis":{"audio":{"metadataoptions":{"sentenceBoundaryEnabled":"false","wordBoundaryEnabled":"false"},"outputFormat":"audio-24khz-48kbitrate-mono-mp3"}}}}"""
    }
}
