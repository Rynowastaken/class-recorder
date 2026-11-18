package com.ryno.classrecorder.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearancePreferencesTest {
    @Test
    fun unknownOrBlankStringsFallbackSafely() {
        assertEquals(DshThemeMode.SYSTEM, AppearancePreferences.parseThemeMode(null))
        assertEquals(DshThemeMode.SYSTEM, AppearancePreferences.parseThemeMode("portal"))
        assertEquals(DshThemeMode.LIGHT, AppearancePreferences.parseThemeMode("LIGHT"))
        assertEquals(DshThemeMode.DARK, AppearancePreferences.parseThemeMode("dark"))

        assertEquals("mist", AppearancePreferences.parseAccentId(null))
        assertEquals("mist", AppearancePreferences.parseAccentId("neon_pink"))
        assertEquals("daiqing", AppearancePreferences.parseAccentId("daiqing"))
        assertEquals("wallpaper", AppearancePreferences.parseAccentId("wallpaper"))
    }
}
