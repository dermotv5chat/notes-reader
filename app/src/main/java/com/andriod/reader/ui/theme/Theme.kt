package com.andriod.reader.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val NoteEditorBackgroundLight = Color(0xFFFFF9E8)
val NoteEditorBackgroundDark = Color(0xFF1C1B18)

val LocalNoteEditorBackground = staticCompositionLocalOf { NoteEditorBackgroundLight }

private val LightColors = lightColorScheme(
    primary = Color(0xFF1565C0),
    secondary = Color(0xFF455A64),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFFB0BEC5),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
)

@Composable
fun ReaderTheme(
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = AppThemeResolver.resolveDarkTheme(
        mode = themeMode,
        systemDark = isSystemInDarkTheme(),
    )
    val editorBackground = if (darkTheme) NoteEditorBackgroundDark else NoteEditorBackgroundLight
    CompositionLocalProvider(LocalNoteEditorBackground provides editorBackground) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            content = content,
        )
    }
}
