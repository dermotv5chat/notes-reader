package com.andriod.reader.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class PracticeEventParseTest {
    @Test
    fun parseMuyu_fromMuyuString() {
        assertEquals(PracticeEvent.MUYU, parsePracticeEvent("MUYU"))
    }

    @Test
    fun parseMuyu_fromLegacyCommentString() {
        assertEquals(PracticeEvent.MUYU, parsePracticeEvent("COMMENT"))
    }
}
