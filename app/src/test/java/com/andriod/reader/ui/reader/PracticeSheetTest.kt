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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
