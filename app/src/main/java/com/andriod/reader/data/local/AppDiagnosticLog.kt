package com.andriod.reader.data.local

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class AppDiagnosticLog @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext
    private val logDir = File(appContext.filesDir, "logs").apply { mkdirs() }
    private val logFile = File(logDir, LOG_FILE_NAME)
    private val writeExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "AppDiagnosticLog").apply { isDaemon = true }
    }

    private val lock = Any()
    private val lines = ArrayDeque<String>()
    private var totalBytes = 0

    fun d(tag: String, message: String) = log(Level.DEBUG, tag, message)

    fun i(tag: String, message: String) = log(Level.INFO, tag, message)

    fun w(tag: String, message: String) = log(Level.WARN, tag, message)

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val body = if (throwable != null) {
            "$message | ${throwable.javaClass.simpleName}: ${throwable.message}"
        } else {
            message
        }
        log(Level.ERROR, tag, body)
    }

    fun logSessionHeader(header: String) {
        synchronized(lock) {
            lines.clear()
            totalBytes = 0
        }
        writeExecutor.execute {
            synchronized(logFile) {
                logFile.writeText("")
            }
        }
        i("Session", header)
    }

    fun stats(): LogStats {
        synchronized(lock) {
            return LogStats(lineCount = lines.size, sizeBytes = totalBytes.toLong())
        }
    }

    fun snapshotText(): String {
        synchronized(lock) {
            return lines.joinToString("\n")
        }
    }

    fun clear() {
        synchronized(lock) {
            lines.clear()
            totalBytes = 0
        }
        writeExecutor.execute {
            synchronized(logFile) {
                if (logFile.exists()) {
                    logFile.writeText("")
                }
            }
        }
    }

    internal fun logFileForExport(): File = logFile

    private fun log(level: Level, tag: String, message: String) {
        val redacted = LogRedactor.redact(message)
        val line = formatLine(level, tag, redacted)
        appendLine(line)
        writeExecutor.execute { appendLineToFile(line) }
        Log.println(level.androidPriority, LOGCAT_TAG, "[$tag] $redacted")
    }

    private fun appendLine(line: String) {
        val lineBytes = line.toByteArray(Charsets.UTF_8).size + 1
        synchronized(lock) {
            while (totalBytes + lineBytes > MAX_BUFFER_BYTES && lines.isNotEmpty()) {
                val removed = lines.removeFirst()
                totalBytes -= removed.toByteArray(Charsets.UTF_8).size + 1
            }
            lines.addLast(line)
            totalBytes += lineBytes
        }
    }

    private fun appendLineToFile(line: String) {
        synchronized(logFile) {
            if (logFile.length() > MAX_FILE_BYTES) {
                trimLogFile()
            }
            logFile.appendText("$line\n")
        }
    }

    private fun trimLogFile() {
        val content = logFile.readText()
        val trimmed = content.takeLast(MAX_FILE_BYTES / 2)
        val firstNewline = trimmed.indexOf('\n')
        logFile.writeText(if (firstNewline >= 0) trimmed.substring(firstNewline + 1) else trimmed)
    }

    private fun formatLine(level: Level, tag: String, message: String): String {
        val timestamp = TIMESTAMP_FORMAT.format(Instant.now().atZone(ZoneOffset.UTC))
        val thread = Thread.currentThread().name
        return "$timestamp ${level.label} [$tag] ($thread) $message"
    }

    data class LogStats(
        val lineCount: Int,
        val sizeBytes: Long,
    )

    private enum class Level(val label: String, val androidPriority: Int) {
        DEBUG("D", Log.DEBUG),
        INFO("I", Log.INFO),
        WARN("W", Log.WARN),
        ERROR("E", Log.ERROR),
    }

    companion object {
        const val LOGCAT_TAG = "ReaderDiag"
        private const val LOG_FILE_NAME = "diagnostic.log"
        private const val MAX_BUFFER_BYTES = 512 * 1024
        private const val MAX_FILE_BYTES = 512 * 1024
        private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    }
}
