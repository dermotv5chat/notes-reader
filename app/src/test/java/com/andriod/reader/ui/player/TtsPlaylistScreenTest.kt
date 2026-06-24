package com.andriod.reader.ui.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.andriod.reader.data.local.TtsPlaylistSnapshot
import com.andriod.reader.domain.TtsPlaylistItem
import com.andriod.reader.service.TtsPlaybackSession
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TtsPlaylistScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playlistScreenContent_emptyQueue_disablesPlayAll() {
        composeRule.setContent {
            MaterialTheme {
                TtsPlaylistScreenContent(
                    snapshot = TtsPlaylistSnapshot(),
                    session = TtsPlaybackSession(),
                    onPlayAll = {},
                    onPlayItem = {},
                    onRemoveItem = {},
                    onClear = {},
                    onRepeatModeChange = {},
                    onOpenNote = {},
                )
            }
        }

        composeRule.onNodeWithTag(TtsPlaylistScreenTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(TtsPlaylistScreenTestTags.PLAY_ALL_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithText("列表为空，在笔记列表左滑或阅读页加入").assertIsDisplayed()
    }

    @Test
    fun playlistScreenContent_withItems_enablesPlayAll() {
        composeRule.setContent {
            MaterialTheme {
                TtsPlaylistScreenContent(
                    snapshot = TtsPlaylistSnapshot(
                        items = listOf(
                            TtsPlaylistItem("a.md", "笔记 A", Instant.EPOCH),
                        ),
                    ),
                    session = TtsPlaybackSession(),
                    onPlayAll = {},
                    onPlayItem = {},
                    onRemoveItem = {},
                    onClear = {},
                    onRepeatModeChange = {},
                    onOpenNote = {},
                )
            }
        }

        composeRule.onNodeWithTag(TtsPlaylistScreenTestTags.PLAY_ALL_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithText("笔记 A").assertIsDisplayed()
    }
}
