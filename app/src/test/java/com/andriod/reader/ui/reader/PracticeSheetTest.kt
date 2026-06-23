package com.andriod.reader.ui.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
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
    fun practiceSheetContent_followedButtonQuickTapSavesWithoutNote() {
        var savedEvent: PracticeEvent? = null
        var savedNote: String? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    PracticeSheetContent(
                        sheetState = PracticeSheetState(
                            blockId = "id1",
                            blockLabel = "测试准则",
                        ),
                        onSave = { event, note ->
                            savedEvent = event
                            savedNote = note
                        },
                        onClear = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(PracticeSheetTestTags.FOLLOWED_BUTTON, useUnmergedTree = true)
            .performClick()

        assertEquals(PracticeEvent.FOLLOWED, savedEvent)
        assertEquals("", savedNote)
    }

    @Test
    fun practiceSheetContent_noteFieldHiddenUntilNoteDialog() {
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

        composeRule.onNodeWithTag(PracticeSheetTestTags.NOTE_FIELD, useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithText("轻点快记 · 长按可加备注 · 评论写想法").assertIsDisplayed()
    }

    @Test
    fun practiceSheetContent_commentButtonOpensNoteDialog() {
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

        composeRule.onNodeWithTag(PracticeSheetTestTags.COMMENT_BUTTON, useUnmergedTree = true)
            .performClick()
        composeRule.onNodeWithText("写评论").assertIsDisplayed()
    }

    @Test
    fun practiceNoteDialog_commentRequiresNonEmptyNote() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    PracticeNoteDialog(
                        event = PracticeEvent.COMMENT,
                        onConfirm = {},
                        onDismiss = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(PracticeSheetTestTags.NOTE_CONFIRM_BUTTON, useUnmergedTree = true)
            .assertIsNotEnabled()
        composeRule.onNodeWithTag(PracticeSheetTestTags.NOTE_FIELD, useUnmergedTree = true)
            .performTextInput("有感而发")
        composeRule.onNodeWithTag(PracticeSheetTestTags.NOTE_CONFIRM_BUTTON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun practiceNoteDialog_commentConfirmSavesNote() {
        var savedNote: String? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    PracticeNoteDialog(
                        event = PracticeEvent.COMMENT,
                        onConfirm = { savedNote = it },
                        onDismiss = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(PracticeSheetTestTags.NOTE_FIELD, useUnmergedTree = true)
            .performTextInput("今天想到的事")
        composeRule.onNodeWithTag(PracticeSheetTestTags.NOTE_CONFIRM_BUTTON, useUnmergedTree = true)
            .performClick()

        assertEquals("今天想到的事", savedNote)
    }

    @Test
    fun practiceSheetContent_longPressOpensNoteDialog() {
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

        composeRule.onNodeWithTag(PracticeSheetTestTags.FOLLOWED_BUTTON, useUnmergedTree = true)
            .performTouchInput {
                down(center)
                advanceEventTime(500)
                up()
            }
        composeRule.onNodeWithTag(PracticeSheetTestTags.NOTE_DIALOG, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun practiceNoteDialog_confirmSavesWithNote() {
        var savedNote: String? = null
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    PracticeNoteDialog(
                        event = PracticeEvent.VIOLATED,
                        onConfirm = { savedNote = it },
                        onDismiss = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(PracticeSheetTestTags.NOTE_DIALOG, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithTag(PracticeSheetTestTags.NOTE_FIELD, useUnmergedTree = true)
            .performTextInput("熬夜")
        composeRule.onNodeWithTag(PracticeSheetTestTags.NOTE_CONFIRM_BUTTON, useUnmergedTree = true)
            .performClick()

        assertEquals("熬夜", savedNote)
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
