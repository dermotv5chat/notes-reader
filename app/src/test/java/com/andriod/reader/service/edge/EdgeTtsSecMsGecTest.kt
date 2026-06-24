package com.andriod.reader.service.edge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeTtsSecMsGecTest {
    @Test
    fun generate_returnsUppercaseSha256Hex() {
        val token = EdgeTtsSecMsGec.generate()
        assertEquals(64, token.length)
        assertTrue(token.all { it in '0'..'9' || it in 'A'..'F' })
    }

    @Test
    fun generate_isStableWithinFiveMinuteWindow() {
        val first = EdgeTtsSecMsGec.generate()
        val second = EdgeTtsSecMsGec.generate()
        assertEquals(first, second)
    }
}
