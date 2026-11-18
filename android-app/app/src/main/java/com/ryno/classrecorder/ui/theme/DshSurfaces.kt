package com.ryno.classrecorder.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Four-tier glass material system ported from DSH Go's dsh_surfaces.dart.
 *
 * Chrome and Sheet sit over moving content; Card and Pill rest over the static
 * gradient backdrop with blurSigma = 0f so scrolling lists never thrash GPU
 * offscreen render targets.
 */
enum class GlassTier { Chrome, Sheet, Card, Pill }

@Immutable
data class GlassShadow(
    val color: Color,
    val blurRadius: Dp,
    val offsetY: Dp
)

@Immutable
data class GlassStyle(
    val fill: Color,
    val fallbackFill: Color,
    val hairline: Color,
    val rim: Color,
    val blurSigma: Float = 0f,
    val hairlineWidth: Dp = 0.5.dp,
    val shadows: List<GlassShadow> = emptyList()
) {
    val blurs: Boolean get() = blurSigma > 0f
}

@Immutable
data class DshSurfaces(
    val backdropTop: Color,
    val backdropBottom: Color,
    val glowPrimary: Color,
    val glowSecondary: Color,
    val userBubble: Color,
    val onUserBubble: Color,
    val scrim: Color,
    val chrome: GlassStyle,
    val sheet: GlassStyle,
    val card: GlassStyle,
    val pill: GlassStyle,
    val accent: AppAccent = AppAccent.Mist,
    val darkTheme: Boolean = true,
    val blurEnabled: Boolean = true
) {
    fun glass(tier: GlassTier): GlassStyle = when (tier) {
        GlassTier.Chrome -> chrome
        GlassTier.Sheet -> sheet
        GlassTier.Card -> card
        GlassTier.Pill -> pill
    }

    companion object {
        fun light(accent: AppAccent, blurEnabled: Boolean = true): DshSurfaces {
            val n = NeutralTones.forAccent(accent)
            val ink = n.shadowInk
            val white = Color.White
            return DshSurfaces(
                backdropTop = n.mistTop,
                backdropBottom = n.mistBottom,
                glowPrimary = accent.glow.copy(alpha = 0.42f),
                glowSecondary = accent.haze.copy(alpha = 0.38f),
                userBubble = accent.bubble,
                onUserBubble = n.ink,
                scrim = n.scrim.copy(alpha = 0.32f),
                accent = accent,
                darkTheme = false,
                blurEnabled = blurEnabled,
                chrome = GlassStyle(
                    fill = white.copy(alpha = 0.78f),
                    fallbackFill = n.mistLowest.copy(alpha = 0.96f),
                    hairline = ink.copy(alpha = 0.08f),
                    rim = white.copy(alpha = 0.85f),
                    blurSigma = 18f,
                    shadows = listOf(
                        GlassShadow(color = ink.copy(alpha = 0.08f), blurRadius = 24.dp, offsetY = 8.dp),
                        GlassShadow(color = ink.copy(alpha = 0.05f), blurRadius = 3.dp, offsetY = 1.dp)
                    )
                ),
                sheet = GlassStyle(
                    fill = n.mistLow.copy(alpha = 0.94f),
                    fallbackFill = n.mistLow,
                    hairline = ink.copy(alpha = 0.07f),
                    rim = white.copy(alpha = 0.90f),
                    blurSigma = 24f,
                    shadows = listOf(
                        GlassShadow(color = ink.copy(alpha = 0.10f), blurRadius = 32.dp, offsetY = 8.dp)
                    )
                ),
                card = GlassStyle(
                    fill = white.copy(alpha = 0.64f),
                    fallbackFill = white.copy(alpha = 0.74f),
                    hairline = ink.copy(alpha = 0.06f),
                    rim = white.copy(alpha = 0.72f)
                ),
                pill = GlassStyle(
                    fill = white.copy(alpha = 0.56f),
                    fallbackFill = white.copy(alpha = 0.68f),
                    hairline = ink.copy(alpha = 0.07f),
                    rim = white.copy(alpha = 0.75f)
                )
            )
        }

        fun dark(accent: AppAccent, blurEnabled: Boolean = true): DshSurfaces {
            val n = NeutralTones.forAccent(accent)
            val white = Color.White
            val black = Color.Black
            val smoke = n.smoke
            return DshSurfaces(
                backdropTop = n.nightTop,
                backdropBottom = n.nightBottom,
                glowPrimary = accent.glowDark.copy(alpha = 0.28f),
                glowSecondary = accent.hazeDark.copy(alpha = 0.24f),
                userBubble = accent.bubbleDark,
                onUserBubble = n.paper,
                scrim = black.copy(alpha = 0.48f),
                accent = accent,
                darkTheme = true,
                blurEnabled = blurEnabled,
                chrome = GlassStyle(
                    fill = smoke.copy(alpha = 0.80f),
                    fallbackFill = smoke.copy(alpha = 0.97f),
                    hairline = white.copy(alpha = 0.09f),
                    rim = white.copy(alpha = 0.12f),
                    blurSigma = 18f,
                    shadows = listOf(
                        GlassShadow(color = black.copy(alpha = 0.35f), blurRadius = 24.dp, offsetY = 8.dp),
                        GlassShadow(color = black.copy(alpha = 0.25f), blurRadius = 3.dp, offsetY = 1.dp)
                    )
                ),
                sheet = GlassStyle(
                    fill = n.sheetDark.copy(alpha = 0.94f),
                    fallbackFill = n.sheetDark,
                    hairline = white.copy(alpha = 0.08f),
                    rim = white.copy(alpha = 0.10f),
                    blurSigma = 24f,
                    shadows = listOf(
                        GlassShadow(color = black.copy(alpha = 0.40f), blurRadius = 32.dp, offsetY = 8.dp)
                    )
                ),
                card = GlassStyle(
                    fill = white.copy(alpha = 0.055f),
                    fallbackFill = white.copy(alpha = 0.065f),
                    hairline = white.copy(alpha = 0.07f),
                    rim = white.copy(alpha = 0.06f)
                ),
                pill = GlassStyle(
                    fill = white.copy(alpha = 0.065f),
                    fallbackFill = white.copy(alpha = 0.075f),
                    hairline = white.copy(alpha = 0.08f),
                    rim = white.copy(alpha = 0.08f)
                )
            )
        }
    }
}
