package com.ryno.classrecorder.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.min

/**
 * DSH Go accent palette presets + OKLCH contrast-guarded runtime derivation.
 *
 * Ported from DSH Go's accent.dart.
 */
@Immutable
data class AppAccent(
    val id: String,
    val label: String,
    val seed: Color,
    val container: Color,
    val onContainer: Color,
    val bubble: Color,
    val glow: Color,
    val seedDark: Color,
    val onSeedDark: Color,
    val containerDark: Color,
    val onContainerDark: Color,
    val bubbleDark: Color,
    val glowDark: Color,
    val tintHue: Double,
    val tintChroma: Double,
    val haze: Color,
    val hazeDark: Color
) {
    val onSeed: Color get() = Color(0xFFFFFFFF)

    fun seedFor(darkTheme: Boolean): Color = if (darkTheme) seedDark else seed

    companion object {
        const val WALLPAPER_ID = "wallpaper"
        const val MAX_CHROMA = 0.13
        const val DARK_LIFT = 0.234
        const val MIN_ON_SURFACE = 3.0
        const val MIN_TEXT = 4.5

        val Mist = AppAccent(
            id = "mist",
            label = "Mist",
            seed = Color(0xFF4B67B4),
            container = Color(0xFFDEE8FF),
            onContainer = Color(0xFF213262),
            bubble = Color(0xFFD9E4FD),
            glow = Color(0xFFABC3FC),
            seedDark = Color(0xFF97B1F0),
            onSeedDark = Color(0xFF121C34),
            containerDark = Color(0xFF2E3C5F),
            onContainerDark = Color(0xFFD8E1F6),
            bubbleDark = Color(0xFF293144),
            glowDark = Color(0xFF51679D),
            tintHue = NeutralTones.BASE_HUE,
            tintChroma = 1.0,
            haze = Color(0xFFD5CBEB),
            hazeDark = Color(0xFF514765)
        )

        val Daiqing = AppAccent(
            id = "daiqing",
            label = "Daiqing",
            seed = Color(0xFF236185),
            container = Color(0xFFDBEAF5),
            onContainer = Color(0xFF123950),
            bubble = Color(0xFFD6E7F3),
            glow = Color(0xFFA2CAE5),
            seedDark = Color(0xFF8CB9D7),
            onSeedDark = Color(0xFF0D1F2B),
            containerDark = Color(0xFF274152),
            onContainerDark = Color(0xFFD5E3ED),
            bubbleDark = Color(0xFF26333C),
            glowDark = Color(0xFF466E88),
            tintHue = 238.0,
            tintChroma = 0.9,
            haze = Color(0xFFC7D0EC),
            hazeDark = Color(0xFF444C66)
        )

        val Dusk = AppAccent(
            id = "dusk",
            label = "Dusk",
            seed = Color(0xFF726291),
            container = Color(0xFFEAE5F4),
            onContainer = Color(0xFF392F4C),
            bubble = Color(0xFFE6E1F1),
            glow = Color(0xFFC8BDE1),
            seedDark = Color(0xFFB8ABD2),
            onSeedDark = Color(0xFF1F1A29),
            containerDark = Color(0xFF40394F),
            onContainerDark = Color(0xFFE2DEEC),
            bubbleDark = Color(0xFF332F3B),
            glowDark = Color(0xFF6D6284),
            tintHue = 295.0,
            tintChroma = 0.85,
            haze = Color(0xFFE6C7D7),
            hazeDark = Color(0xFF5F4353)
        )

        val Turquoise = AppAccent(
            id = "turquoise",
            label = "Turquoise",
            seed = Color(0xFF34766F),
            container = Color(0xFFDBECE9),
            onContainer = Color(0xFF123D39),
            bubble = Color(0xFFD6E8E6),
            glow = Color(0xFFA3CEC8),
            seedDark = Color(0xFF8DBEB7),
            onSeedDark = Color(0xFF0D211F),
            containerDark = Color(0xFF274340),
            onContainerDark = Color(0xFFD6E5E2),
            bubbleDark = Color(0xFF263533),
            glowDark = Color(0xFF47726D),
            tintHue = 190.0,
            tintChroma = 0.7,
            haze = Color(0xFFB9D7E3),
            hazeDark = Color(0xFF34525E)
        )

        val Amber = AppAccent(
            id = "amber",
            label = "Amber",
            seed = Color(0xFF8D6636),
            container = Color(0xFFF2E6D9),
            onContainer = Color(0xFF483011),
            bubble = Color(0xFFEEE2D3),
            glow = Color(0xFFDCBE9D),
            seedDark = Color(0xFFCCAC87),
            onSeedDark = Color(0xFF271A0C),
            containerDark = Color(0xFF4C3A25),
            onContainerDark = Color(0xFFEADFD3),
            bubbleDark = Color(0xFF392F24),
            glowDark = Color(0xFF7F6343),
            tintHue = 75.0,
            tintChroma = 0.75,
            haze = Color(0xFFEAC8C1),
            hazeDark = Color(0xFF64443D)
        )

        val Graphite = AppAccent(
            id = "graphite",
            label = "Graphite",
            seed = Color(0xFF2C2E32),
            container = Color(0xFFE3E6EC),
            onContainer = Color(0xFF2C2E32),
            bubble = Color(0xFFE0E3E8),
            glow = Color(0xFFBBBEC3),
            seedDark = Color(0xFFE5E8ED),
            onSeedDark = Color(0xFF191B1E),
            containerDark = Color(0xFF3B3D41),
            onContainerDark = Color(0xFFDEE1E7),
            bubbleDark = Color(0xFF2E3034),
            glowDark = Color(0xFF616368),
            tintHue = 262.0,
            tintChroma = 0.25,
            haze = Color(0xFFCED1D6),
            hazeDark = Color(0xFF4A4D52)
        )

        val presets: List<AppAccent> = listOf(Mist, Daiqing, Dusk, Turquoise, Amber, Graphite)

        fun preset(id: String?): AppAccent = presets.firstOrNull { it.id == id } ?: Mist

        private val derivedCache = HashMap<Triple<Int, String, String>, AppAccent>()

        fun derive(
            pick: Color,
            id: String = WALLPAPER_ID,
            label: String = "Wallpaper"
        ): AppAccent {
            val key = Triple(pick.toArgb(), id, label)
            synchronized(derivedCache) {
                derivedCache[key]?.let { return it }
                if (derivedCache.size > 64) derivedCache.clear()
                return deriveUncached(pick, id, label).also { derivedCache[key] = it }
            }
        }

        private fun deriveUncached(pick: Color, id: String, label: String): AppAccent {
            val o = Oklch.fromColor(pick)
            val c = min(o.c, MAX_CHROMA)
            val h = if (o.h.isFinite()) o.h else NeutralTones.BASE_HUE
            val k = min(1.0, c / 0.1255)
            val tintChroma = 0.8 * k
            val neutrals = NeutralTones.of(tintHue = h, tintChroma = tintChroma)
            fun tone(l: Double, ratio: Double): Color = Oklch(l, c * ratio, h).toColor()

            val onSeedDark = tone(0.231, 0.39)
            val lightL = o.l.coerceIn(0.30, 0.60)
            val seed = guard(
                start = Oklch(lightL, c, h),
                darkTheme = false,
                surface = neutrals.mistHighest,
                onAccent = Color.White
            )
            val seedDark = guard(
                start = Oklch(min(0.93, lightL + DARK_LIFT), c * 0.76, h),
                darkTheme = true,
                surface = neutrals.nightHighest,
                onAccent = onSeedDark
            )
            val hazeHue = (h + 33.0) % 360.0
            return AppAccent(
                id = id,
                label = label,
                seed = seed,
                container = tone(0.93, 0.26),
                onContainer = tone(0.33, 0.70),
                bubble = tone(0.918, 0.29),
                glow = tone(0.819, 0.68),
                seedDark = seedDark,
                onSeedDark = onSeedDark,
                containerDark = tone(0.361, 0.51),
                onContainerDark = tone(0.909, 0.24),
                bubbleDark = tone(0.314, 0.29),
                glowDark = tone(0.52, 0.72),
                tintHue = h,
                tintChroma = tintChroma,
                haze = Oklch(0.86, 0.045 * k, hazeHue).toColor(),
                hazeDark = Oklch(0.42, 0.05 * k, hazeHue).toColor()
            )
        }

        fun guard(
            start: Oklch,
            darkTheme: Boolean,
            surface: Color,
            onAccent: Color
        ): Color {
            fun ok(candidate: Color): Boolean =
                contrastRatio(candidate, surface) >= MIN_ON_SURFACE &&
                    contrastRatio(onAccent, candidate) >= MIN_TEXT

            val first = start.toColor()
            if (ok(first)) return first
            var pass = if (darkTheme) 1.0 else 0.0
            var fail = start.l
            repeat(24) {
                val mid = (pass + fail) * 0.5
                if (ok(start.copy(l = mid).toColor())) {
                    pass = mid
                } else {
                    fail = mid
                }
            }
            return start.copy(l = pass).toColor()
        }
    }
}
