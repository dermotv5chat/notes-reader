package com.andriod.reader.data.local

import android.content.Context
import com.andriod.reader.domain.PracticeDayEntry
import com.andriod.reader.domain.PracticeEvent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeLogStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    private val metaDir: File
        get() = File(context.filesDir, ".meta").also { it.mkdirs() }

    private val logFile: File
        get() = File(metaDir, "practice-logs.json")

    fun getTodayEntry(fileName: String, blockId: String, date: LocalDate = LocalDate.now()): PracticeDayEntry? {
        val dto = readRaw()[fileName]?.get(blockId)?.get(date.toString()) ?: return null
        return dto.toEntry()
    }

    fun getTodayEntriesForNote(
        fileName: String,
        date: LocalDate = LocalDate.now(),
    ): Map<String, PracticeDayEntry> {
        val dateKey = date.toString()
        return readRaw()[fileName].orEmpty()
            .mapNotNull { (blockId, days) ->
                days[dateKey]?.toEntry()?.let { blockId to it }
            }
            .toMap()
    }

    fun saveTodayEntry(
        fileName: String,
        blockId: String,
        event: PracticeEvent,
        note: String = "",
        date: LocalDate = LocalDate.now(),
    ) {
        val all = readRaw().toMutableMap()
        val noteLogs = all.getOrPut(fileName) { mutableMapOf() }.toMutableMap()
        val blockLogs = noteLogs.getOrPut(blockId) { mutableMapOf() }.toMutableMap()
        blockLogs[date.toString()] = PracticeDayEntryDto(
            event = event.name,
            note = note,
        )
        noteLogs[blockId] = blockLogs
        all[fileName] = noteLogs
        writeRaw(all)
    }

    fun clearTodayEntry(
        fileName: String,
        blockId: String,
        date: LocalDate = LocalDate.now(),
    ) {
        val all = readRaw().toMutableMap()
        val noteLogs = all[fileName]?.toMutableMap() ?: return
        val blockLogs = noteLogs[blockId]?.toMutableMap() ?: return
        blockLogs.remove(date.toString())
        if (blockLogs.isEmpty()) {
            noteLogs.remove(blockId)
        } else {
            noteLogs[blockId] = blockLogs
        }
        if (noteLogs.isEmpty()) {
            all.remove(fileName)
        } else {
            all[fileName] = noteLogs
        }
        writeRaw(all)
    }

    fun migrateBlockId(fileName: String, oldId: String, newId: String) {
        if (oldId == newId) return
        val all = readRaw().toMutableMap()
        val noteLogs = all[fileName]?.toMutableMap() ?: return
        val oldEntries = noteLogs.remove(oldId) ?: return
        val merged = noteLogs.getOrPut(newId) { mutableMapOf() }.toMutableMap()
        oldEntries.forEach { (date, dto) -> merged[date] = dto }
        noteLogs[newId] = merged
        all[fileName] = noteLogs
        writeRaw(all)
    }

    fun hasAnyEntry(fileName: String, blockId: String): Boolean =
        readRaw()[fileName]?.get(blockId)?.isNotEmpty() == true

    private fun readRaw(): Map<String, Map<String, Map<String, PracticeDayEntryDto>>> {
        val file = logFile
        if (!file.exists()) return emptyMap()
        val type = object : TypeToken<Map<String, Map<String, Map<String, PracticeDayEntryDto>>>>() {}.type
        return gson.fromJson(file.readText(), type) ?: emptyMap()
    }

    private fun writeRaw(data: Map<String, Map<String, Map<String, PracticeDayEntryDto>>>) {
        logFile.writeText(gson.toJson(data))
    }

    private data class PracticeDayEntryDto(
        val event: String,
        val note: String = "",
    ) {
        fun toEntry(): PracticeDayEntry? =
            runCatching { PracticeEvent.valueOf(event) }
                .getOrNull()
                ?.let { PracticeDayEntry(event = it, note = note) }
    }
}
