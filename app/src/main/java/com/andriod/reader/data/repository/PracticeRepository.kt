package com.andriod.reader.data.repository

import com.andriod.reader.data.local.BlockRegistry
import com.andriod.reader.data.local.CalloutLineParser
import com.andriod.reader.data.local.MarkdownBlockParser
import com.andriod.reader.data.local.PracticeLogStore
import com.andriod.reader.domain.NoteBlock
import com.andriod.reader.domain.PracticeDayEntry
import com.andriod.reader.domain.PracticeEvent
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

    fun saveTodayEntry(
        fileName: String,
        blockId: String,
        event: PracticeEvent,
        note: String = "",
        date: LocalDate = LocalDate.now(),
    ) {
        practiceLogStore.saveTodayEntry(fileName, blockId, event, note, date)
    }

    fun clearTodayEntry(fileName: String, blockId: String, date: LocalDate = LocalDate.now()) {
        practiceLogStore.clearTodayEntry(fileName, blockId, date)
    }
}
