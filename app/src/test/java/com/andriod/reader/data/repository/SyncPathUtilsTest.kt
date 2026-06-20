package com.andriod.reader.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncPathUtilsTest {
    @Test
    fun normalize_trimsSlashesAndBackslashes() {
        assertEquals("work/todo.md", SyncPathUtils.normalize("/work\\todo.md/"))
    }

    @Test
    fun fileBaseName_returnsLastSegment() {
        assertEquals("todo.md", SyncPathUtils.fileBaseName("work/todo.md"))
    }
}
