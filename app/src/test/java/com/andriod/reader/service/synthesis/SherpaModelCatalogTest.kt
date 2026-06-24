package com.andriod.reader.service.synthesis

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SherpaModelCatalogTest {
    @Test
    fun defaultPack_isMelo() {
        assertEquals(SherpaModelCatalog.MELO_ID, SherpaModelCatalog.defaultPack().id)
    }

    @Test
    fun catalog_containsMultiSpeakerPack() {
        val zhLl = SherpaModelCatalog.packById(SherpaModelCatalog.ZH_LL_ID)
        requireNotNull(zhLl)
        assertTrue(zhLl.speakerCount > 1)
    }

    @Test
    fun catalog_containsMaleVoicePack() {
        val wnj = SherpaModelCatalog.packById(SherpaModelCatalog.FANCHEN_WNJ_ID)
        requireNotNull(wnj)
        assertEquals("男声", wnj.genderLabel)
    }
}
