package com.andriod.reader.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockIdResolverTest {
    @Test
    fun resolve_usesAnchorWhenPresent() {
        assertEquals(
            "note.md^sleep11",
            BlockIdResolver.resolve("note.md", 5, "> [!habit] 11 点睡觉 ^sleep11"),
        )
    }

    @Test
    fun stripBlockAnchor_removesSuffix() {
        assertEquals(
            "11 点睡觉",
            BlockIdResolver.stripBlockAnchor("11 点睡觉 ^sleep11"),
        )
    }

    @Test
    fun resolve_fingerprintWhenNoAnchor() {
        val id = BlockIdResolver.resolve("note.md", 0, "- [ ] test")
        assertTrue(id.startsWith("note.md#0#"))
    }
}
