package com.andriod.reader.data.local

import android.content.Context
import com.andriod.reader.domain.PracticeDayEntry
import com.andriod.reader.domain.PracticeEvent
import com.andriod.reader.domain.PracticeLogEntry
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeLogStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    private val metaDir: File
        get() = File(context.filesDir, ".meta").also { it.mkdirs() }

    private val logFile: File
        get() = File(metaDir, "practice-logs.json")

    fun getTodayEntry(fileName: String, blockId: String, date: LocalDate = LocalDate.now()): PracticeDayEntry? =
        latestEntryOnDate(fileName, blockId, date)?.toDayEntry()

    fun getTodayEntriesForNote(
        fileName: String,
        date: LocalDate = LocalDate.now(),
    ): Map<String, PracticeDayEntry> =
        readRaw()[fileName].orEmpty()
            .mapNotNull { (blockId, entries) ->
                entries.latestStatusOnDate(date)?.toDayEntry()?.let { blockId to it }
            }
            .toMap()

    fun getHistoryForBlock(fileName: String, blockId: String): List<PracticeLogEntry> =
        readRaw()[fileName]?.get(blockId).orEmpty()
            .mapNotNull { it.toEntry() }
            .sortedByDescending { it.recordedAt }

    fun appendEntry(
        fileName: String,
        blockId: String,
        event: PracticeEvent,
        note: String = "",
        recordedAt: Instant = Instant.now(),
    ) {
        val all = readRaw().toMutableMap()
        val noteLogs = all.getOrPut(fileName) { mutableMapOf() }.toMutableMap()
        val blockLogs = noteLogs.getOrPut(blockId) { mutableListOf() }.toMutableList()
        blockLogs.add(
            PracticeLogEntryDto(
                event = event.name,
                note = note,
                recordedAt = recordedAt.toString(),
            ),
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
        val blockLogs = noteLogs[blockId]?.toMutableList() ?: return
        val remaining = blockLogs.filterNot { it.recordedOn(date, zoneId) }
        if (remaining.isEmpty()) {
            noteLogs.remove(blockId)
        } else {
            noteLogs[blockId] = remaining.toMutableList()
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
        val merged = (noteLogs.getOrPut(newId) { mutableListOf() } + oldEntries)
            .sortedBy { it.recordedAtInstant() }
            .toMutableList()
        noteLogs[newId] = merged
        all[fileName] = noteLogs
        writeRaw(all)
    }

    fun hasAnyEntryOnDate(
        fileName: String,
        blockId: String,
        date: LocalDate = LocalDate.now(),
    ): Boolean =
        readRaw()[fileName]?.get(blockId)?.any { it.recordedOn(date, zoneId) } == true

    fun hasAnyEntry(fileName: String, blockId: String): Boolean =
        readRaw()[fileName]?.get(blockId)?.isNotEmpty() == true

    private fun latestEntryOnDate(fileName: String, blockId: String, date: LocalDate): PracticeLogEntryDto? =
        readRaw()[fileName]?.get(blockId)?.latestStatusOnDate(date)

    private fun List<PracticeLogEntryDto>.latestStatusOnDate(date: LocalDate): PracticeLogEntryDto? =
        filter { it.recordedOn(date, zoneId) && it.isStatusEvent() }
            .maxByOrNull { it.recordedAtInstant() }

    private fun PracticeLogEntryDto.isStatusEvent(): Boolean =
        event == PracticeEvent.FOLLOWED.name || event == PracticeEvent.VIOLATED.name

    private fun readRaw(): Map<String, Map<String, MutableList<PracticeLogEntryDto>>> {
        val file = logFile
        if (!file.exists()) return emptyMap()
        val root = JsonParser.parseString(file.readText()).asJsonObject
        var migrated = false
        val result = mutableMapOf<String, MutableMap<String, MutableList<PracticeLogEntryDto>>>()
        root.entrySet().forEach { (fileName, blocksEl) ->
            val blocks = mutableMapOf<String, MutableList<PracticeLogEntryDto>>()
            blocksEl.asJsonObject.entrySet().forEach { (blockId, entriesEl) ->
                val (entries, wasLegacy) = parseBlockLogs(entriesEl)
                if (wasLegacy) migrated = true
                blocks[blockId] = entries.toMutableList()
            }
            result[fileName] = blocks
        }
        if (migrated) {
            writeRaw(result)
        }
        return result
    }

    private fun parseBlockLogs(element: JsonElement): Pair<List<PracticeLogEntryDto>, Boolean> {
        if (element.isJsonArray) {
            val type = object : TypeToken<List<PracticeLogEntryDto>>() {}.type
            return (gson.fromJson<List<PracticeLogEntryDto>>(element, type) ?: emptyList()) to false
        }
        if (!element.isJsonObject) return emptyList<PracticeLogEntryDto>() to false
        val legacy = element.asJsonObject.entrySet().mapNotNull { (dateKey, entryEl) ->
            runCatching {
                val dto = gson.fromJson(entryEl, LegacyDayEntryDto::class.java)
                val date = LocalDate.parse(dateKey)
                val instant = date.atTime(12, 0).atZone(zoneId).toInstant()
                PracticeLogEntryDto(
                    event = dto.event,
                    note = dto.note,
                    recordedAt = instant.toString(),
                )
            }.getOrNull()
        }
        return legacy to true
    }

    private fun writeRaw(data: Map<String, Map<String, MutableList<PracticeLogEntryDto>>>) {
        logFile.writeText(gson.toJson(data))
    }

    private data class LegacyDayEntryDto(
        val event: String,
        val note: String = "",
    )

    private data class PracticeLogEntryDto(
        val event: String,
        val note: String = "",
        val recordedAt: String,
    ) {
        fun recordedAtInstant(): Instant = Instant.parse(recordedAt)

        fun recordedOn(date: LocalDate, zoneId: ZoneId): Boolean =
            recordedAtInstant().atZone(zoneId).toLocalDate() == date

        fun toEntry(): PracticeLogEntry? =
            runCatching { PracticeEvent.valueOf(event) }
                .getOrNull()
                ?.let { PracticeLogEntry(event = it, note = note, recordedAt = recordedAtInstant()) }

        fun toDayEntry(): PracticeDayEntry? =
            toEntry()?.let { PracticeDayEntry(event = it.event, note = it.note) }
    }
}
