package com.andriod.reader.ui.editor

import androidx.compose.foundation.layout.PaddingValues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.unit.dp

class EditorKeyboardLayoutTest {
    @Test
    fun shouldZeroNavPadding_alwaysTrueForEditor() {
        assertTrue(EditorKeyboardLayout.shouldZeroNavPadding(isEditor = true))
        assertFalse(EditorKeyboardLayout.shouldZeroNavPadding(isEditor = false))
    }

    @Test
    fun shouldHideMiniBarOnEditor_whileOnEditorRoute() {
        assertTrue(EditorKeyboardLayout.shouldHideMiniBarOnEditor(isEditor = true))
        assertFalse(EditorKeyboardLayout.shouldHideMiniBarOnEditor(isEditor = false))
    }

    @Test
    fun toolbarBottomPadding_subtractsObstruction() {
        assertEquals(244.dp, EditorKeyboardLayout.toolbarBottomPadding(300.dp, 56.dp))
        assertEquals(0.dp, EditorKeyboardLayout.toolbarBottomPadding(40.dp, 56.dp))
    }

    @Test
    fun contentBottomPadding_includesToolbarHeightWhenVisible() {
        assertEquals(292.dp, EditorKeyboardLayout.contentBottomPadding(300.dp, toolbarVisible = true, bottomObstruction = 56.dp))
        assertEquals(244.dp, EditorKeyboardLayout.contentBottomPadding(300.dp, toolbarVisible = false, bottomObstruction = 56.dp))
    }

    @Test
    fun resolveNavPadding_editorAlwaysZero() {
        val scaffoldPadding = PaddingValues(bottom = 80.dp)
        assertEquals(
            PaddingValues(),
            EditorKeyboardLayout.resolveNavPadding(
                isEditor = true,
                isPrinciplesGuide = false,
                isSettingsSubRoute = false,
                showMiniBar = true,
                scaffoldPadding = scaffoldPadding,
            ),
        )
    }

    @Test
    fun resolveNavPadding_principlesGuideDependsOnMiniBar() {
        val scaffoldPadding = PaddingValues(bottom = 80.dp)
        assertEquals(
            PaddingValues(),
            EditorKeyboardLayout.resolveNavPadding(
                isEditor = false,
                isPrinciplesGuide = true,
                isSettingsSubRoute = false,
                showMiniBar = false,
                scaffoldPadding = scaffoldPadding,
            ),
        )
        assertEquals(
            scaffoldPadding,
            EditorKeyboardLayout.resolveNavPadding(
                isEditor = false,
                isPrinciplesGuide = true,
                isSettingsSubRoute = false,
                showMiniBar = true,
                scaffoldPadding = scaffoldPadding,
            ),
        )
    }
}
