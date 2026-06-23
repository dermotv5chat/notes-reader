package com.andriod.reader.data.repository

import com.andriod.reader.data.local.BlockRegistry
import com.andriod.reader.data.local.CalloutLineParser
import com.andriod.reader.data.local.MarkdownBlockParser
import com.andriod.reader.data.local.PracticeLogStore
import com.andriod.reader.domain.NoteBlock
import com.andriod.reader.domain.PracticeDayEntry
import com.andriod.reader.domain.PracticeEvent
import com.andriod.reader.domain.PracticeLogEntry
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PracticeRepository @Inject constructor(
    private val practiceLogStore: PracticeLogStore,
    private val blockRegistry: BlockRegistry,
) {
    fun parseBlocks(content: String, fileName: String): List<NoteBlock> {
        val callouts = CalloutLineParser.extractCallouts(content)
        val calloutIds = blockRegistry.resolveCalloutIds(fileName, callouts)
        return MarkdownBlockParser.parse(content, fileName, calloutIds)
    }

    fun getTodayEntry(fileName: String, blockId: String, date: LocalDate = LocalDate.now()): PracticeDayEntry? =
        practiceLogStore.getTodayEntry(fileName, blockId, date)

    fun getTodayEntriesForNote(fileName: String, date: LocalDate = LocalDate.now()): Map<String, PracticeDayEntry> =
        practiceLogStore.getTodayEntriesForNote(fileName, date)

    fun getBlockHistory(fileName: String, blockId: String): List<PracticeLogEntry> =
        practiceLogStore.getHistoryForBlock(fileName, blockId)

    fun appendEntry(
        fileName: String,
        blockId: String,
        event: PracticeEvent,
        note: String = "",
        recordedAt: Instant = Instant.now(),
    ) {
        practiceLogStore.appendEntry(fileName, blockId, event, note, recordedAt)
    }

    fun clearTodayEntry(fileName: String, blockId: String, date: LocalDate = LocalDate.now()) {
        practiceLogStore.clearTodayEntry(fileName, blockId, date)
    }
}
