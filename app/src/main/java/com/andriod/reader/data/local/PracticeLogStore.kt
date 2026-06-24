package com.andriod.reader.data.local

import android.content.Context
import com.andriod.reader.domain.PracticeDayEntry
import com.andriod.reader.domain.PracticeEvent
import com.andriod.reader.domain.PracticeLogEntry
import com.andriod.reader.domain.PracticeMode
import com.andriod.reader.domain.PracticePeriod
import com.andriod.reader.domain.RepeatPeriod
import com.andriod.reader.domain.parsePracticeEvent
import com.andriod.reader.domain.isStatusEvent
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

    fun getPeriodStatusEntry(
        fileName: String,
        blockId: String,
        mode: PracticeMode,
        repeatPeriod: RepeatPeriod,
        date: LocalDate = LocalDate.now(),
    ): PracticeDayEntry? = when (mode) {
        PracticeMode.WHEN -> latestStatusOnDate(fileName, blockId, date)?.toDayEntry()
        PracticeMode.REPEATLY -> latestStatusInPeriod(fileName, blockId, repeatPeriod, date)?.toDayEntry()
    }

    fun getPeriodStatusEntriesForBlocks(
        fileName: String,
        blocks: List<PeriodBlockRef>,
        date: LocalDate = LocalDate.now(),
    ): Map<String, PracticeDayEntry> =
        blocks.mapNotNull { block ->
            getPeriodStatusEntry(fileName, block.blockId, block.mode, block.repeatPeriod, date)
                ?.let { block.blockId to it }
        }.toMap()

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

    fun clearPeriodEntry(
        fileName: String,
        blockId: String,
        mode: PracticeMode,
        repeatPeriod: RepeatPeriod,
        date: LocalDate = LocalDate.now(),
    ) {
        val all = readRaw().toMutableMap()
        val noteLogs = all[fileName]?.toMutableMap() ?: return
        val blockLogs = noteLogs[blockId]?.toMutableList() ?: return
        val remaining = when (mode) {
            PracticeMode.WHEN -> blockLogs.filterNot { it.recordedOn(date, zoneId) }
            PracticeMode.REPEATLY -> blockLogs.filterNot {
                val entryDate = it.recordedAtInstant().atZone(zoneId).toLocalDate()
                PracticePeriod.isSamePeriod(entryDate, date, repeatPeriod)
            }
        }
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

    fun hasAnyEntryInPeriod(
        fileName: String,
        blockId: String,
        mode: PracticeMode,
        repeatPeriod: RepeatPeriod,
        date: LocalDate = LocalDate.now(),
    ): Boolean = when (mode) {
        PracticeMode.WHEN -> readRaw()[fileName]?.get(blockId)?.any { it.recordedOn(date, zoneId) } == true
        PracticeMode.REPEATLY -> readRaw()[fileName]?.get(blockId)?.any {
            val entryDate = it.recordedAtInstant().atZone(zoneId).toLocalDate()
            PracticePeriod.isSamePeriod(entryDate, date, repeatPeriod)
        } == true
    }

    fun hasAnyEntry(fileName: String, blockId: String): Boolean =
        readRaw()[fileName]?.get(blockId)?.isNotEmpty() == true

    private fun latestStatusOnDate(fileName: String, blockId: String, date: LocalDate): PracticeLogEntryDto? =
        readRaw()[fileName]?.get(blockId)?.latestStatusOnDate(date)

    private fun latestStatusInPeriod(
        fileName: String,
        blockId: String,
        repeatPeriod: RepeatPeriod,
        date: LocalDate,
    ): PracticeLogEntryDto? =
        readRaw()[fileName]?.get(blockId)
            ?.filter {
                val entryDate = it.recordedAtInstant().atZone(zoneId).toLocalDate()
                PracticePeriod.isSamePeriod(entryDate, date, repeatPeriod) && it.isStatusEvent()
            }
            ?.maxByOrNull { it.recordedAtInstant() }

    private fun List<PracticeLogEntryDto>.latestStatusOnDate(date: LocalDate): PracticeLogEntryDto? =
        filter { it.recordedOn(date, zoneId) && it.isStatusEvent() }
            .maxByOrNull { it.recordedAtInstant() }

    private fun PracticeLogEntryDto.isStatusEvent(): Boolean =
        parsePracticeEvent(event)?.isStatusEvent() == true

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
            parsePracticeEvent(event)?.let {
                PracticeLogEntry(event = it, note = note, recordedAt = recordedAtInstant())
            }

        fun toDayEntry(): PracticeDayEntry? =
            toEntry()?.let { PracticeDayEntry(event = it.event, note = it.note) }
    }
}

data class PeriodBlockRef(
    val blockId: String,
    val mode: PracticeMode,
    val repeatPeriod: RepeatPeriod,
)
