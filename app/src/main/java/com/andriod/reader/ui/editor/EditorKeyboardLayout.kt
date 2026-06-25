package com.andriod.reader.ui.editor

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object EditorKeyboardLayout {
    val ToolbarHeight: Dp = 48.dp

    fun shouldZeroNavPadding(isEditor: Boolean): Boolean = isEditor

    fun shouldHideMiniBarOnEditor(isEditor: Boolean): Boolean = isEditor

    fun resolveNavPadding(
        isEditor: Boolean,
        isPrinciplesGuide: Boolean,
        isSettingsSubRoute: Boolean,
        showMiniBar: Boolean,
        scaffoldPadding: PaddingValues,
    ): PaddingValues = when {
        shouldZeroNavPadding(isEditor) -> PaddingValues()
        isPrinciplesGuide || isSettingsSubRoute ->
            if (!showMiniBar) PaddingValues() else scaffoldPadding
        else -> scaffoldPadding
    }

    fun toolbarBottomPadding(imeBottom: Dp, bottomObstruction: Dp): Dp =
        (imeBottom - bottomObstruction).coerceAtLeast(0.dp)

    fun contentBottomPadding(
        imeBottom: Dp,
        toolbarVisible: Boolean,
        bottomObstruction: Dp,
    ): Dp {
        val effectiveIme = toolbarBottomPadding(imeBottom, bottomObstruction)
        return effectiveIme + if (toolbarVisible) ToolbarHeight else 0.dp
    }
}
