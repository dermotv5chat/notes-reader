package com.andriod.reader.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppThemeResolverTest {
    @Test
    fun resolveDarkTheme_lightMode() {
        assertFalse(AppThemeResolver.resolveDarkTheme(AppThemeMode.LIGHT, systemDark = true))
        assertFalse(AppThemeResolver.resolveDarkTheme(AppThemeMode.LIGHT, systemDark = false))
    }

    @Test
    fun resolveDarkTheme_darkMode() {
        assertTrue(AppThemeResolver.resolveDarkTheme(AppThemeMode.DARK, systemDark = false))
    }

    @Test
    fun resolveDarkTheme_systemMode() {
        assertTrue(AppThemeResolver.resolveDarkTheme(AppThemeMode.SYSTEM, systemDark = true))
        assertFalse(AppThemeResolver.resolveDarkTheme(AppThemeMode.SYSTEM, systemDark = false))
    }

    @Test
    fun fromStored_defaultsToSystem() {
        assertEquals(AppThemeMode.SYSTEM, AppThemeMode.fromStored(null))
        assertEquals(AppThemeMode.DARK, AppThemeMode.fromStored("DARK"))
    }
}
