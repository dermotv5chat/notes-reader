package com.andriod.reader.ui.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.andriod.reader.domain.PracticeEvent
import com.andriod.reader.domain.PracticeLogEntry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PracticeSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun practiceSheetContent_showsBlockLabel() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    PracticeSheetContent(
                        sheetState = PracticeSheetState(
                            blockId = "id1",
                            blockLabel = "11 点睡觉",
                        ),
                        onSave = { _, _ -> },
                        onClear = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(PracticeSheetTestTags.SHEET_TITLE, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("11 点睡觉").assertIsDisplayed()
    }

    @Test
    fun practiceSheetContent_followedButtonInvokesSave() {
        var savedEvent: PracticeEvent? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    PracticeSheetContent(
                        sheetState = PracticeSheetState(
                            blockId = "id1",
                            blockLabel = "测试准则",
                        ),
                        onSave = { event, _ -> savedEvent = event },
                        onClear = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(PracticeSheetTestTags.FOLLOWED_BUTTON, useUnmergedTree = true)
            .performClick()

        assertEquals(PracticeEvent.FOLLOWED, savedEvent)
    }

    @Test
    fun practiceSheetContent_historyCollapsedByDefault_showsEmptyOnExpand() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    PracticeSheetContent(
                        sheetState = PracticeSheetState(
                            blockId = "id1",
                            blockLabel = "测试准则",
                        ),
                        onSave = { _, _ -> },
                        onClear = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(PracticeSheetTestTags.HISTORY_SECTION, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("历史记录（0 条）").assertIsDisplayed()
        composeRule.onNodeWithTag(PracticeSheetTestTags.HISTORY_EMPTY, useUnmergedTree = true)
            .assertDoesNotExist()

        composeRule.onNodeWithTag(PracticeSheetTestTags.HISTORY_SECTION, useUnmergedTree = true)
            .performClick()
        composeRule.onNodeWithTag(PracticeSheetTestTags.HISTORY_EMPTY, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun practiceSheetContent_showsHistoryEntriesWhenExpanded() {
        val recordedAt = Instant.parse("2026-06-20T14:30:00Z")
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    PracticeSheetContent(
                        sheetState = PracticeSheetState(
                            blockId = "id1",
                            blockLabel = "测试准则",
                            history = listOf(
                                PracticeLogEntry(
                                    event = PracticeEvent.VIOLATED,
                                    note = "熬夜",
                                    recordedAt = recordedAt,
                                ),
                            ),
                        ),
                        onSave = { _, _ -> },
                        onClear = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("历史记录（1 条）").assertIsDisplayed()
        composeRule.onNodeWithTag(PracticeSheetTestTags.HISTORY_ITEM, useUnmergedTree = true)
            .assertDoesNotExist()

        composeRule.onNodeWithTag(PracticeSheetTestTags.HISTORY_SECTION, useUnmergedTree = true)
            .performClick()
        composeRule.onNodeWithTag(PracticeSheetTestTags.HISTORY_ITEM, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("熬夜").assertIsDisplayed()
    }

    @Test
    fun practiceSheetContent_clearTodayRequiresConfirmation() {
        var cleared = false
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    PracticeSheetContent(
                        sheetState = PracticeSheetState(
                            blockId = "id1",
                            blockLabel = "测试准则",
                            hasTodayEntry = true,
                        ),
                        onSave = { _, _ -> },
                        onClear = { cleared = true },
                    )
                }
            }
        }

        composeRule.onNodeWithTag(PracticeSheetTestTags.CLEAR_TODAY_BUTTON, useUnmergedTree = true)
            .assertDoesNotExist()

        composeRule.onNodeWithTag(PracticeSheetTestTags.HISTORY_SECTION, useUnmergedTree = true)
            .performClick()
        composeRule.onNodeWithTag(PracticeSheetTestTags.CLEAR_TODAY_BUTTON, useUnmergedTree = true)
            .performClick()
        composeRule.onNodeWithTag(PracticeSheetTestTags.CLEAR_CONFIRM_DIALOG, useUnmergedTree = true)
            .assertIsDisplayed()

        composeRule.onNodeWithText("取消").performClick()
        assertEquals(false, cleared)

        composeRule.onNodeWithTag(PracticeSheetTestTags.CLEAR_TODAY_BUTTON, useUnmergedTree = true)
            .performClick()
        composeRule.onNodeWithTag(PracticeSheetTestTags.CLEAR_CONFIRM_BUTTON, useUnmergedTree = true)
            .performClick()
        assertEquals(true, cleared)
    }
}
