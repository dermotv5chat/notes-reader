package com.andriod.reader.ui.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.andriod.reader.data.local.TtsPlaylistSnapshot
import com.andriod.reader.domain.TtsPlaylistItem
import com.andriod.reader.domain.TtsQueueRepeatMode
import com.andriod.reader.service.TtsPlaybackSession
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TtsQueueSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun queueSheetContent_showsItemsAndRepeatModes() {
        composeRule.setContent {
            MaterialTheme {
                TtsQueueSheetContent(
                    snapshot = TtsPlaylistSnapshot(
                        items = listOf(
                            TtsPlaylistItem("a.md", "笔记 A", Instant.EPOCH),
                        ),
                        repeatMode = TtsQueueRepeatMode.REPEAT_ONE,
                    ),
                    session = TtsPlaybackSession(fileName = "a.md", title = "笔记 A"),
                    onPlayItem = {},
                    onRemoveItem = {},
                    onClear = {},
                    onRepeatModeChange = {},
                )
            }
        }

        composeRule.onNodeWithTag(TtsQueueSheetTestTags.SHEET).assertIsDisplayed()
        composeRule.onNodeWithText("笔记 A").assertIsDisplayed()
        composeRule.onNodeWithTag(TtsQueueSheetTestTags.REPEAT_ONE).assertIsDisplayed()
    }
}
