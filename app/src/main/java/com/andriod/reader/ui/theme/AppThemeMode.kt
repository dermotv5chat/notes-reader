package com.andriod.reader.ui.theme

enum class AppThemeMode {
    LIGHT,
    DARK,
    SYSTEM,
    ;

    companion object {
        fun fromStored(value: String?): AppThemeMode =
            entries.find { it.name == value } ?: SYSTEM
    }
}

object AppThemeResolver {
    fun resolveDarkTheme(mode: AppThemeMode, systemDark: Boolean): Boolean = when (mode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> systemDark
    }
}
