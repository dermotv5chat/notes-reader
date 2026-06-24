package com.andriod.reader.service.edge

import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.roundToLong

/**
 * Generates Sec-MS-GEC token required by Microsoft Edge read-aloud WebSocket API.
 * @see <a href="https://github.com/rany2/edge-tts/blob/master/src/edge_tts/drm.py">edge-tts drm.py</a>
 */
object EdgeTtsSecMsGec {
    const val TRUSTED_CLIENT_TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4"
    private const val CHROMIUM_FULL_VERSION = "143.0.3650.75"
    private const val CHROMIUM_MAJOR_VERSION = "143"
    const val SEC_MS_GEC_VERSION = "1-$CHROMIUM_FULL_VERSION"
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/$CHROMIUM_MAJOR_VERSION.0.0.0 Safari/537.36 " +
            "Edg/$CHROMIUM_MAJOR_VERSION.0.0.0"

    private const val WIN_EPOCH = 11644473600.0
    private const val ROUND_SECONDS = 300.0
    private const val TICKS_MULTIPLIER = 10_000_000.0

    @Volatile
    var clockSkewSeconds: Double = 0.0

    @Volatile
    private var clockSynced: Boolean = false

    fun generate(): String {
        var ticks = currentUnixSeconds()
        ticks += WIN_EPOCH
        ticks -= ticks % ROUND_SECONDS
        ticks *= TICKS_MULTIPLIER
        val strToHash = "${ticks.roundToLong()}$TRUSTED_CLIENT_TOKEN"
        return sha256Hex(strToHash).uppercase()
    }

    fun syncClockSkewFromDateHeader(dateHeader: String?) {
        val serverSeconds = parseRfc2616Date(dateHeader) ?: return
        clockSkewSeconds += serverSeconds - currentUnixSecondsWithoutSkew()
        clockSynced = true
    }

    /** Proactively align device clock with Microsoft server (critical on Android). */
    fun ensureClockSynced(client: OkHttpClient) {
        if (clockSynced) return
        synchronized(this) {
            if (clockSynced) return
            fetchAndApplyServerClock(client)
        }
    }

    fun resyncClockForRetry(client: OkHttpClient) {
        synchronized(this) {
            clockSynced = false
            fetchAndApplyServerClock(client)
        }
    }

    private fun fetchAndApplyServerClock(client: OkHttpClient) {
        runCatching {
            val probe = client.newCall(
                Request.Builder()
                    .url(
                        "https://speech.platform.bing.com/consumer/speech/synthesize/readaloud/" +
                            "voices/list?trustedclienttoken=$TRUSTED_CLIENT_TOKEN" +
                            "&Sec-MS-GEC=${generate()}&Sec-MS-GEC-Version=$SEC_MS_GEC_VERSION",
                    )
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build(),
            ).execute()
            probe.use { response ->
                syncClockSkewFromDateHeader(response.header("Date"))
            }
        }
    }

    fun randomMuid(): String {
        val bytes = ByteArray(16)
        java.security.SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02X".format(it) }
    }

    private fun currentUnixSeconds(): Double =
        Instant.now().epochSecond.toDouble() + clockSkewSeconds

    private fun currentUnixSecondsWithoutSkew(): Double =
        Instant.now().epochSecond.toDouble()

    private fun parseRfc2616Date(date: String?): Double? {
        if (date.isNullOrBlank()) return null
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
        format.timeZone = TimeZone.getTimeZone("GMT")
        return runCatching { format.parse(date)?.time?.div(1000.0) }.getOrNull()
    }

    private fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray(Charsets.US_ASCII))
            .joinToString("") { "%02x".format(it) }
    }

    fun defaultHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
}
