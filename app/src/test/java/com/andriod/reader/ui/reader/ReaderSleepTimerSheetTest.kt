package com.andriod.reader.ui.reader

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.percentOffset
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ReaderSleepTimerSheetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun lastPresetSubtitle_isIndependentOfSliderValue() {
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    ReaderSleepTimerSheetContent(
                        uiState = ReaderUiState(
                            sleepTimerVisible = true,
                            sleepTimerSliderMinutes = 10f,
                            lastSleepTimerPresetSubtitle = "45 分钟",
                        ),
                        onSliderFinished = {},
                        onAfterNoteEnd = {},
                        onApplyLastPreset = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(SleepTimerSheetTestTags.SLIDER_VALUE_LABEL, useUnmergedTree = true)
            .assertTextEquals("10 分钟")
        composeRule.onNodeWithTag(SleepTimerSheetTestTags.LAST_PRESET_SUBTITLE, useUnmergedTree = true)
            .assertTextEquals("45 分钟")
    }

    @Test
    fun lastPresetSubtitle_staysFixedWhenSliderCommits() {
        var uiState by mutableStateOf(
            ReaderUiState(
                sleepTimerVisible = true,
                sleepTimerSliderMinutes = 0f,
                lastSleepTimerPresetSubtitle = "45 分钟",
            ),
        )
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    ReaderSleepTimerSheetContent(
                        uiState = uiState,
                        onSliderFinished = { minutes ->
                            uiState = uiState.copy(
                                sleepTimerSliderMinutes = minutes,
                                lastSleepTimerPresetSubtitle = "${minutes.toInt()} 分钟",
                            )
                        },
                        onAfterNoteEnd = {},
                        onApplyLastPreset = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(SleepTimerSheetTestTags.SLIDER, useUnmergedTree = true)
            .performTouchInput { click(percentOffset(0.5f, 0.5f)) }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SleepTimerSheetTestTags.LAST_PRESET_SUBTITLE, useUnmergedTree = true)
            .assertTextEquals("45 分钟")
    }

    @Test
    fun sliderTap_invokesFinishedOnceWithSelectedMinutes() {
        val finishedMinutes = mutableListOf<Float>()
        composeRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                MaterialTheme {
                    ReaderSleepTimerSheetContent(
                        uiState = ReaderUiState(
                            sleepTimerVisible = true,
                            sleepTimerSliderMinutes = 0f,
                            lastSleepTimerPresetSubtitle = "30 分钟",
                        ),
                        onSliderFinished = { finishedMinutes.add(it) },
                        onAfterNoteEnd = {},
                        onApplyLastPreset = {},
                    )
                }
            }
        }

        composeRule.onNodeWithTag(SleepTimerSheetTestTags.SLIDER, useUnmergedTree = true)
            .performTouchInput { click(percentOffset(0.5f, 0.5f)) }

        composeRule.waitForIdle()
        assertEquals(1, finishedMinutes.size)
        assert(finishedMinutes.single() > 0f)
    }
}
