package com.ryno.classrecorder.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Semantic fixed colors and 8-slot deterministic FNV-1a course/category identity pairs,
 * ported from DSH Go's semantic_colors.dart.
 */
enum class StatusTone { Idle, Ok, Warn, Error, Busy }

@Immutable
data class DshColors(
    val textTertiary: Color,
    val textDisabled: Color,
    val success: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
    val codeWellBg: Color,
    val codeWellHeader: Color,
    val codeWellFg: Color,
    val codeWellBorder: Color,
    val identity: List<Color>,
    val onIdentity: List<Color>
) {
    /** Returns `(containerColor, contentColor)` for a course or group name. */
    fun identityPair(key: String): Pair<Color, Color> {
        val idx = identityIndex(key, identity.size)
        return identity[idx] to onIdentity[idx]
    }

    /** Returns `(containerColor, contentColor)` for a fixed slot index. */
    fun identityAt(slot: Int): Pair<Color, Color> {
        val idx = ((slot % identity.size) + identity.size) % identity.size
        return identity[idx] to onIdentity[idx]
    }

    /**
     * Contrast-guarded (>= 3.5:1) solid dot/pip color for a course or category key,
     * tuned in OKLCH for [onDarkSurface] so small 4–9dp indicators never wash out.
     */
    fun courseDot(key: String, onDarkSurface: Boolean): Color {
        val hue = DOT_HUES[identityIndex(key, DOT_HUES.size)]
        return if (onDarkSurface) {
            Oklch(0.79, 0.105, hue).toColor()
        } else {
            Oklch(0.45, 0.115, hue).toColor()
        }
    }

    companion object {
        private val DOT_HUES = doubleArrayOf(
            245.0, // Sky blue
            285.0, // Lavender
            332.0, // Lotus rose
            24.0,  // Terracotta coral
            66.0,  // Ochre wheat
            140.0, // Moss green
            172.0, // Celadon mint
            205.0  // Lake cyan
        )

        /**
         * Stable unsigned 32-bit FNV-1a hash mapped to `0 until slots`.
         * Normalizes blank strings to `"Uncategorized"` so untagged courses always match.
         */
        fun identityIndex(key: String, slots: Int = 8): Int {
            if (slots <= 1) return 0
            val normalized = key.trim().ifEmpty { "Uncategorized" }
            var hash = 0x811c9dc5u
            for (ch in normalized) {
                hash = hash xor ch.code.toUInt()
                hash *= 0x01000193u
            }
            return (hash % slots.toUInt()).toInt()
        }

        val Light = DshColors(
            textTertiary = Color(0xFF646974),
            textDisabled = Color(0xFFA1A5AC),
            success = Color(0xFF337250),
            onSuccessContainer = Color(0xFF153F29),
            warning = Color(0xFF936123),
            onWarningContainer = Color(0xFF55350D),
            danger = Color(0xFFA54743),
            onDanger = Color(0xFFFDF9F8),
            dangerContainer = Color(0xFFFFE2E0),
            onDangerContainer = Color(0xFF642724),
            codeWellBg = Color(0xFFEDF0F6),
            codeWellHeader = Color(0xFFE7EBF3),
            codeWellFg = Color(0xFF1E232E),
            codeWellBorder = Color(0xFFDBDEE5),
            identity = listOf(
                Color(0xFFDAECFF),
                Color(0xFFE9E6FF),
                Color(0xFFF9E1F3),
                Color(0xFFFFE2E0),
                Color(0xFFF8E6D1),
                Color(0xFFE0EFD8),
                Color(0xFFD3F1E9),
                Color(0xFFD1F0F8)
            ),
            onIdentity = listOf(
                Color(0xFF173F65),
                Color(0xFF3E3463),
                Color(0xFF562B4E),
                Color(0xFF602A2A),
                Color(0xFF563500),
                Color(0xFF2A4618),
                Color(0xFF00483D),
                Color(0xFF004552)
            )
        )

        val Dark = DshColors(
            textTertiary = Color(0xFF9A9EA9),
            textDisabled = Color(0xFF656971),
            success = Color(0xFF8BCCA6),
            onSuccessContainer = Color(0xFFC9E7D4),
            warning = Color(0xFFE6BD7C),
            onWarningContainer = Color(0xFFF4E2C3),
            danger = Color(0xFFE69089),
            onDanger = Color(0xFF331513),
            dangerContainer = Color(0xFF542C29),
            onDangerContainer = Color(0xFFF8D9D7),
            codeWellBg = Color(0xFF14171E),
            codeWellHeader = Color(0xFF1A1E26),
            codeWellFg = Color(0xFFE6E9F0),
            codeWellBorder = Color(0xFF2F333A),
            identity = listOf(
                Color(0xFF243A50),
                Color(0xFF38334F),
                Color(0xFF472E42),
                Color(0xFF4E2E2D),
                Color(0xFF47341A),
                Color(0xFF2D3E24),
                Color(0xFF174038),
                Color(0xFF153E47)
            ),
            onIdentity = listOf(
                Color(0xFFC5DEF8),
                Color(0xFFDBD6F7),
                Color(0xFFEFD0E7),
                Color(0xFFF7D0CD),
                Color(0xFFEED7BB),
                Color(0xFFCEE2C4),
                Color(0xFFBCE5DB),
                Color(0xFFBAE3ED)
            )
        )

        fun forAccent(darkTheme: Boolean, accent: AppAccent): DshColors {
            val base = if (darkTheme) Dark else Light
            val n = NeutralTones.forAccent(accent)
            if (n === NeutralTones.Defaults) return base
            return if (darkTheme) {
                base.copy(
                    textTertiary = n.paperFaint,
                    textDisabled = n.paperGhost,
                    codeWellBg = n.codeWellDark,
                    codeWellHeader = n.codeWellHeaderDark,
                    codeWellFg = n.paper,
                    codeWellBorder = n.codeWellBorderDark
                )
            } else {
                base.copy(
                    textTertiary = n.inkFaint,
                    textDisabled = n.inkGhost,
                    codeWellBg = n.codeWell,
                    codeWellHeader = n.codeWellHeader,
                    codeWellFg = n.ink,
                    codeWellBorder = n.codeWellBorder
                )
            }
        }
    }
}
