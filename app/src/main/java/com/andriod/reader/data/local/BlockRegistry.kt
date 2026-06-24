package com.andriod.reader.data.local

import javax.inject.Inject
import javax.inject.Singleton

data class CalloutKey(
    val lineIndex: Int,
    val variant: String,
    val modifiers: List<String> = emptyList(),
    val text: String,
    val rawLine: String,
)

@Singleton
class BlockRegistry @Inject constructor(
    private val store: BlockRegistryStore,
    private val practiceLogStore: PracticeLogStore,
) {
    fun resolveCalloutIds(fileName: String, callouts: List<CalloutKey>): List<String> {
        if (callouts.isEmpty()) {
            store.write(fileName, FileBlockRegistry())
            return emptyList()
        }

        val state = store.read(fileName)
        if (state.order.size == callouts.size &&
            state.order.indices.all { index ->
                state.entries[state.order[index]]?.variant == callouts[index].variant
            }
        ) {
            persistTextHintsIfChanged(fileName, state, callouts)
            return state.order
        }

        val remaining = state.order.toMutableList()
        val newOrder = mutableListOf<String>()
        val newEntries = state.entries.toMutableMap()

        for (callout in callouts) {
            val matchedId = pickBestMatch(callout, remaining, state.entries)
            val id = if (matchedId != null) {
                remaining.remove(matchedId)
                matchedId
            } else {
                newBlockId()
            }
            val practiceInfo = CalloutCadenceResolver.resolve(callout.variant, callout.modifiers)
            newEntries[id] = BlockRegistryEntry(
                variant = callout.variant,
                textHint = callout.text,
                mode = practiceInfo.mode.name,
                repeatPeriod = practiceInfo.repeatPeriod.name,
            )
            newOrder.add(id)
            migrateLegacyPracticeIds(fileName, callout, id)
        }

        store.write(fileName, FileBlockRegistry(order = newOrder, entries = newEntries))
        return newOrder
    }

    private fun persistTextHintsIfChanged(
        fileName: String,
        state: FileBlockRegistry,
        callouts: List<CalloutKey>,
    ) {
        val updatedEntries = state.entries.toMutableMap()
        var changed = false
        state.order.indices.forEach { index ->
            val id = state.order[index]
            val entry = updatedEntries[id] ?: return@forEach
            val callout = callouts[index]
            val practiceInfo = CalloutCadenceResolver.resolve(callout.variant, callout.modifiers)
            val newHint = callout.text
            val updated = entry.copy(
                textHint = newHint,
                mode = practiceInfo.mode.name,
                repeatPeriod = practiceInfo.repeatPeriod.name,
            )
            if (updated != entry) {
                updatedEntries[id] = updated
                changed = true
            }
        }
        if (changed) {
            store.write(fileName, state.copy(entries = updatedEntries))
        }
    }

    private fun pickBestMatch(
        callout: CalloutKey,
        candidates: List<String>,
        entries: Map<String, BlockRegistryEntry>,
    ): String? {
        return candidates
            .filter { entries[it]?.variant == callout.variant }
            .maxByOrNull { id -> textScore(entries[id]?.textHint.orEmpty(), callout.text) }
            ?.takeIf { id -> textScore(entries[id]?.textHint.orEmpty(), callout.text) >= MIN_MATCH_SCORE }
    }

    private fun textScore(previous: String, current: String): Int {
        if (previous.isBlank()) return 0
        if (previous == current) return 100
        if (current.contains(previous) || previous.contains(current)) return 80
        val maxLen = maxOf(previous.length, current.length).coerceAtLeast(1)
        return commonPrefixLength(previous, current) * 100 / maxLen
    }

    private fun commonPrefixLength(a: String, b: String): Int {
        val limit = minOf(a.length, b.length)
        for (i in 0 until limit) {
            if (a[i] != b[i]) return i
        }
        return limit
    }

    private fun migrateLegacyPracticeIds(fileName: String, callout: CalloutKey, newId: String) {
        val legacyIds = buildList {
            add(BlockIdResolver.lineIndexId(fileName, callout.lineIndex))
            add(BlockIdResolver.anchorId(fileName, "b${callout.lineIndex}"))
            BlockIdResolver.findAnchor(callout.rawLine)?.let { add(BlockIdResolver.anchorId(fileName, it)) }
        }
        for (legacy in legacyIds) {
            if (legacy != newId && practiceLogStore.hasAnyEntry(fileName, legacy)) {
                practiceLogStore.migrateBlockId(fileName, legacy, newId)
            }
        }
    }

    fun readRegistry(fileName: String): FileBlockRegistry = store.read(fileName)

    private fun newBlockId(): String = java.util.UUID.randomUUID().toString().replace("-", "").take(12)

    private companion object {
        const val MIN_MATCH_SCORE = 25
    }
}
