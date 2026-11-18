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

    @Test
    fun courseDotColorsMeetNonTextContrastInLightAndDark() {
        val sampleCourses = listOf("民法", "經濟學", "大學之道", "體育", "服務與學習", "資料結構", "微積分", "Uncategorized")
        for (accent in AppAccent.presets) {
            val lightScheme = DshPalette.lightScheme(accent)
            val darkScheme = DshPalette.darkScheme(accent)
            val lightDsh = DshColors.forAccent(false, accent)
            val darkDsh = DshColors.forAccent(true, accent)
            for (course in sampleCourses) {
                val lightDot = lightDsh.courseDot(course, onDarkSurface = false)
                val darkDot = darkDsh.courseDot(course, onDarkSurface = true)
                assertTrue(
                    "light dot $course on ${accent.id}",
                    contrastRatio(lightDot, lightScheme.surfaceContainerLow) >= 3.0
                )
                assertTrue(
                    "dark dot $course on ${accent.id}",
                    contrastRatio(darkDot, darkScheme.surfaceContainerHigh) >= 3.0
                )
            }
        }
    }

    @Test
    fun designMdFrontmatterMatchesKotlinThemeTokens() {
        val designMd = java.io.File("../../DESIGN.md").takeIf { it.exists() }
            ?: java.io.File("../DESIGN.md")
        assertTrue("DESIGN.md exists", designMd.exists())
        val text = designMd.readText()
        val expectedHexes = listOf(
            "mist-primary-light: \"#4B67B4\"",
            "mist-primary-dark: \"#97B1F0\"",
            "daiqing-seed: \"#236185\"",
            "dusk-seed: \"#726291\"",
            "turquoise-seed: \"#34766F\"",
            "amber-seed: \"#8D6636\"",
            "graphite-seed: \"#2C2E32\"",
            "morning-mist-top: \"#EBF1FE\"",
            "morning-mist-bottom: \"#F6F7FC\"",
            "morning-mist-ink: \"#1E232E\"",
            "slate-night-top: \"#1D222C\"",
            "slate-night-bottom: \"#1B1C23\"",
            "slate-night-paper: \"#E6E9F0\"",
            "slate-night-smoke: \"#292E39\""
        )
        for (expected in expectedHexes) {
            assertTrue("DESIGN.md missing $expected", text.contains(expected))
        }
    }
}
