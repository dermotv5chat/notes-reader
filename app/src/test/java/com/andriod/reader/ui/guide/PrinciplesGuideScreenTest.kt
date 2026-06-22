package com.andriod.reader.ui.guide

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PrinciplesGuideScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun principlesGuideScreen_showsTitleAndFirstSection() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    PrinciplesGuideScreen(onBack = {})
                }
            }
        }

        composeRule.onNodeWithText(PrinciplesGuideContent.SCREEN_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(PrinciplesGuideTestTags.FIRST_SECTION, useUnmergedTree = true)
            .assertIsDisplayed()
        composeRule.onNodeWithText("功能概览").assertIsDisplayed()
    }
}
