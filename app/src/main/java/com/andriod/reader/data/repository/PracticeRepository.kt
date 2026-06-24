package com.andriod.reader.data.repository

import com.andriod.reader.data.local.BlockRegistry
import com.andriod.reader.data.local.CalloutCadenceResolver
import com.andriod.reader.data.local.CalloutLineParser
import com.andriod.reader.data.local.MarkdownBlockParser
import com.andriod.reader.data.local.PeriodBlockRef
import com.andriod.reader.data.local.PracticeLogStore
import com.andriod.reader.domain.NoteBlock
import com.andriod.reader.domain.PracticeColorPolicy
import com.andriod.reader.domain.PracticeDayEntry
import com.andriod.reader.domain.PracticeEvent
import com.andriod.reader.domain.PracticeLogEntry
import com.andriod.reader.domain.PracticeMode
import com.andriod.reader.domain.PracticeMaturityTier
import com.andriod.reader.domain.RepeatPeriod
import com.andriod.reader.domain.isStatusEvent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

data class BlockPracticeDisplayMeta(
    val lastStatusDate: LocalDate? = null,
    val maturityTier: PracticeMaturityTier = PracticeMaturityTier.NEUTRAL,
)

@Singleton
class PracticeRepository @Inject constructor(
    private val practiceLogStore: PracticeLogStore,
    private val blockRegistry: BlockRegistry,
) {
    private val zoneId: ZoneId = ZoneId.systemDefault()

    fun parseBlocks(content: String, fileName: String): List<NoteBlock> {
        val callouts = CalloutLineParser.extractCallouts(content)
        val calloutIds = blockRegistry.resolveCalloutIds(fileName, callouts)
        val registry = blockRegistry.readRegistry(fileName)
        val blocks = MarkdownBlockParser.parse(content, fileName, calloutIds)
        return blocks.map { block ->
            if (block is NoteBlock.Callout) {
                val index = registry.order.indexOf(block.id)
                val calloutKey = if (index >= 0) callouts.getOrNull(index) else null
                val entry = registry.entries[block.id]
                val info = resolvePracticeInfo(
                    variant = block.variant,
                    modifiers = calloutKey?.modifiers.orEmpty(),
                    entryMode = entry?.mode,
                    entryRepeatPeriod = entry?.repeatPeriod,
                    legacyCadence = entry?.cadence,
                )
                block.copy(mode = info.mode, repeatPeriod = info.repeatPeriod)
            } else {
                block
            }
        }
    }

    fun getPeriodStatusEntry(
        fileName: String,
        blockId: String,
        mode: PracticeMode,
        repeatPeriod: RepeatPeriod,
        date: LocalDate = LocalDate.now(),
    ): PracticeDayEntry? =
        practiceLogStore.getPeriodStatusEntry(fileName, blockId, mode, repeatPeriod, date)

    fun getPeriodEntriesForBlocks(
        fileName: String,
        blocks: List<NoteBlock>,
        date: LocalDate = LocalDate.now(),
    ): Map<String, PracticeDayEntry> {
        val callouts = blocks.filterIsInstance<NoteBlock.Callout>()
        return practiceLogStore.getPeriodStatusEntriesForBlocks(
            fileName = fileName,
            blocks = callouts.map { PeriodBlockRef(it.id, it.mode, it.repeatPeriod) },
            date = date,
        )
    }

    fun getBlockHistory(fileName: String, blockId: String): List<PracticeLogEntry> =
        practiceLogStore.getHistoryForBlock(fileName, blockId)

    fun getDisplayMetaForBlocks(fileName: String, blocks: List<NoteBlock>): Map<String, BlockPracticeDisplayMeta> =
        blocks.filterIsInstance<NoteBlock.Callout>().associate { block ->
            block.id to buildDisplayMeta(fileName, block.id, block.mode, block.repeatPeriod)
        }

    fun getMaturityTier(
        fileName: String,
        blockId: String,
        mode: PracticeMode,
        repeatPeriod: RepeatPeriod,
    ): PracticeMaturityTier =
        PracticeColorPolicy.compute(
            mode = mode,
            repeatPeriod = repeatPeriod,
            history = getBlockHistory(fileName, blockId),
            zoneId = zoneId,
        )

    fun appendEntry(
        fileName: String,
        blockId: String,
        event: PracticeEvent,
        note: String = "",
        recordedAt: Instant = Instant.now(),
    ) {
        practiceLogStore.appendEntry(fileName, blockId, event, note, recordedAt)
    }

    fun clearPeriodEntry(
        fileName: String,
        blockId: String,
        mode: PracticeMode,
        repeatPeriod: RepeatPeriod,
        date: LocalDate = LocalDate.now(),
    ) {
        practiceLogStore.clearPeriodEntry(fileName, blockId, mode, repeatPeriod, date)
    }

    fun hasAnyEntryInPeriod(
        fileName: String,
        blockId: String,
        mode: PracticeMode,
        repeatPeriod: RepeatPeriod,
        date: LocalDate = LocalDate.now(),
    ): Boolean = practiceLogStore.hasAnyEntryInPeriod(fileName, blockId, mode, repeatPeriod, date)

    private fun buildDisplayMeta(
        fileName: String,
        blockId: String,
        mode: PracticeMode,
        repeatPeriod: RepeatPeriod,
    ): BlockPracticeDisplayMeta {
        val history = getBlockHistory(fileName, blockId)
        val lastStatusDate = history
            .filter { it.event.isStatusEvent() }
            .maxByOrNull { it.recordedAt }
            ?.recordedAt
            ?.atZone(zoneId)
            ?.toLocalDate()
        return BlockPracticeDisplayMeta(
            lastStatusDate = lastStatusDate,
            maturityTier = PracticeColorPolicy.compute(mode, repeatPeriod, history, zoneId = zoneId),
        )
    }

    private fun resolvePracticeInfo(
        variant: String,
        modifiers: List<String>,
        entryMode: String?,
        entryRepeatPeriod: String?,
        legacyCadence: String?,
    ): com.andriod.reader.data.local.CalloutPracticeInfo {
        if (entryMode != null || legacyCadence != null) {
            return CalloutCadenceResolver.fromRegistry(entryMode, entryRepeatPeriod, legacyCadence)
        }
        return CalloutCadenceResolver.resolve(variant, modifiers)
    }
}
