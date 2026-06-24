package com.andriod.reader.data.local

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BlockRegistryTest {
    private lateinit var registry: BlockRegistry

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        context.filesDir.resolve(".meta/block-registry.json").delete()
        context.filesDir.resolve(".meta/practice-logs.json").delete()
        val gson = Gson()
        registry = BlockRegistry(BlockRegistryStore(context, gson), PracticeLogStore(context, gson))
    }

    @Test
    fun resolveCalloutIds_keepsIdsWhenOnlyTextChanges() {
        val fileName = "rules.md"
        val first = listOf(
            CalloutKey(0, "habit", text = "11点睡觉", rawLine = "> [!habit] 11点睡觉"),
        )
        val ids1 = registry.resolveCalloutIds(fileName, first)
        val second = listOf(
            CalloutKey(0, "habit", text = "11点睡觉，你没有晚睡的资本", rawLine = "> [!habit] 11点睡觉，你没有晚睡的资本"),
        )
        val ids2 = registry.resolveCalloutIds(fileName, second)
        assertEquals(ids1, ids2)
    }

    @Test
    fun resolveCalloutIds_assignsNewIdForNewCallout() {
        val fileName = "rules.md"
        registry.resolveCalloutIds(
            fileName,
            listOf(CalloutKey(0, "habit", text = "睡觉", rawLine = "> [!habit] 睡觉")),
        )
        val ids = registry.resolveCalloutIds(
            fileName,
            listOf(
                CalloutKey(0, "habit", text = "睡觉", rawLine = "> [!habit] 睡觉"),
                CalloutKey(2, "rule", text = "别诉苦", rawLine = "> [!rule] 别诉苦"),
            ),
        )
        assertEquals(2, ids.toSet().size)
        assertEquals(ids[0], registry.resolveCalloutIds(
            fileName,
            listOf(CalloutKey(0, "habit", text = "睡觉", rawLine = "> [!habit] 睡觉")),
        ).single())
    }
}
