package com.ryno.classrecorder.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test

class OklchContrastTest {
    @Test
    fun allPresetsMeetWcagContrastInLightAndDark() {
        for (accent in AppAccent.presets) {
            val light = DshPalette.lightScheme(accent)
            val dark = DshPalette.darkScheme(accent)
            assertTrue("light body ${accent.id}", contrastRatio(light.onSurface, light.surface) >= 10.0)
            assertTrue("dark body ${accent.id}", contrastRatio(dark.onSurface, dark.surface) >= 9.5)
            assertTrue("light onPrimary ${accent.id}", contrastRatio(light.onPrimary, light.primary) >= 4.5)
            assertTrue("dark onPrimary ${accent.id}", contrastRatio(dark.onPrimary, dark.primary) >= 4.5)
            assertTrue("light primary vs surface ${accent.id}", contrastRatio(light.primary, light.surfaceContainerHighest) >= 3.0)
            assertTrue("dark primary vs surface ${accent.id}", contrastRatio(dark.primary, dark.surfaceContainerHighest) >= 3.0)
        }
    }

    @Test
    fun extremeWallpaperSeedsGuardCleanly() {
        val extremes = listOf(Color.White, Color.Black, Color(0xFFFFFF00), Color(0xFFFF00FF), Color(0xFF00FFAA))
        for (seed in extremes) {
            val derived = AppAccent.derive(seed)
            assertTrue(Oklch.fromColor(derived.seed).c <= AppAccent.MAX_CHROMA + 0.01)
            val light = DshPalette.lightScheme(derived)
            val dark = DshPalette.darkScheme(derived)
            assertTrue(contrastRatio(light.onPrimary, light.primary) >= 4.45)
            assertTrue(contrastRatio(dark.onPrimary, dark.primary) >= 4.45)
            assertTrue(contrastRatio(light.primary, light.surfaceContainerHighest) >= 2.95)
            assertTrue(contrastRatio(dark.primary, dark.surfaceContainerHighest) >= 2.95)
        }
    }
}
