package com.ryno.classrecorder.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Handcrafted Mist Glass palette & hue-shifted NeutralTones, ported from dsh_palette.dart.
 */
object DshPalette {
    // -- Light: Morning Mist --
    internal val MistTop = Color(0xFFEBF1FE)
    internal val MistBottom = Color(0xFFF6F7FC)
    internal val Mist = Color(0xFFF0F4FD)
    internal val MistLowest = Color(0xFFFBFCFF)
    internal val MistLow = Color(0xFFF6F9FE)
    internal val MistContainer = Color(0xFFEEF1F9)
    internal val MistHigh = Color(0xFFE7EBF4)
    internal val MistHighest = Color(0xFFDFE4EE)

    internal val Ink = Color(0xFF1E232E)
    internal val InkMuted = Color(0xFF4F5562)
    internal val InkFaint = Color(0xFF646974)
    internal val InkGhost = Color(0xFFA1A5AC)
    internal val OutlineLight = Color(0xFF7E838D)
    internal val RuleLight = Color(0xFFDCE0E8)
    internal val ShadowInk = Color(0xFF0B152D)

    // -- Dark: Slate Night --
    internal val NightTop = Color(0xFF1D222C)
    internal val NightBottom = Color(0xFF1B1C23)
    internal val Night = Color(0xFF1C1F28)
    internal val NightLowest = Color(0xFF14171E)
    internal val NightLow = Color(0xFF232730)
    internal val NightContainer = Color(0xFF272C36)
    internal val NightHigh = Color(0xFF2D323C)
    internal val NightHighest = Color(0xFF343944)

    internal val Paper = Color(0xFFE6E9F0)
    internal val PaperMuted = Color(0xFFB6BBC5)
    internal val PaperFaint = Color(0xFF9A9EA9)
    internal val PaperGhost = Color(0xFF656971)
    internal val OutlineDark = Color(0xFF737781)
    internal val RuleDark = Color(0xFF343841)
    internal val ShadowNight = Color(0xFF03060D)

    // -- Shared Semantic Neutrals & Roles --
    internal val Slate = Color(0xFF545E6F)
    internal val SlateContainer = Color(0xFFE2E6F0)
    internal val SlateLift = Color(0xFFB3BED2)
    internal val OnSlateLift = Color(0xFF1A2230)
    private val SlateContainerDark = Color(0x17FFFFFF)

    private val Dusk = Color(0xFF6B578F)
    private val DuskContainer = Color(0xFFECE5FB)
    private val OnDuskContainer = Color(0xFF3B2C54)
    private val DuskLift = Color(0xFFBEAEDE)
    private val OnDuskLift = Color(0xFF251C36)
    private val DuskContainerDark = Color(0xFF3E3451)
    private val OnDuskContainerDark = Color(0xFFE4DDF3)

    private val Danger = Color(0xFFA54743)
    private val OnDanger = Color(0xFFFDF9F8)
    private val DangerContainer = Color(0xFFFFE2E0)
    private val OnDangerContainer = Color(0xFF642724)
    private val DangerLift = Color(0xFFE69089)
    private val OnDangerLift = Color(0xFF331513)
    private val DangerContainerDark = Color(0xFF542C29)
    private val OnDangerContainerDark = Color(0xFFF8D9D7)

    internal val Scrim = Color(0xFF0B111F)

    fun lightScheme(accent: AppAccent): ColorScheme {
        val n = NeutralTones.forAccent(accent)
        return lightColorScheme(
            primary = accent.seed,
            onPrimary = accent.onSeed,
            primaryContainer = accent.container,
            onPrimaryContainer = accent.onContainer,
            secondary = n.slate,
            onSecondary = Color.White,
            secondaryContainer = n.slateContainer,
            onSecondaryContainer = n.ink,
            tertiary = Dusk,
            onTertiary = Color.White,
            tertiaryContainer = DuskContainer,
            onTertiaryContainer = OnDuskContainer,
            error = Danger,
            onError = OnDanger,
            errorContainer = DangerContainer,
            onErrorContainer = OnDangerContainer,
            background = n.mist,
            onBackground = n.ink,
            surface = n.mist,
            onSurface = n.ink,
            surfaceVariant = n.mistContainer,
            onSurfaceVariant = n.inkMuted,
            surfaceDim = n.mistHighest,
            surfaceBright = n.mistLowest,
            surfaceContainerLowest = n.mistLowest,
            surfaceContainerLow = n.mistLow,
            surfaceContainer = n.mistContainer,
            surfaceContainerHigh = n.mistHigh,
            surfaceContainerHighest = n.mistHighest,
            surfaceTint = Color.Transparent,
            inverseSurface = n.inverseSurface,
            inverseOnSurface = n.onInverseSurface,
            inversePrimary = accent.seedDark,
            outline = n.outlineLight,
            outlineVariant = n.ruleLight,
            scrim = n.scrim
        )
    }

    fun darkScheme(accent: AppAccent): ColorScheme {
        val n = NeutralTones.forAccent(accent)
        return darkColorScheme(
            primary = accent.seedDark,
            onPrimary = accent.onSeedDark,
            primaryContainer = accent.containerDark,
            onPrimaryContainer = accent.onContainerDark,
            secondary = n.slateLift,
            onSecondary = n.onSlateLift,
            secondaryContainer = SlateContainerDark,
            onSecondaryContainer = n.paper,
            tertiary = DuskLift,
            onTertiary = OnDuskLift,
            tertiaryContainer = DuskContainerDark,
            onTertiaryContainer = OnDuskContainerDark,
            error = DangerLift,
            onError = OnDangerLift,
            errorContainer = DangerContainerDark,
            onErrorContainer = OnDangerContainerDark,
            background = n.night,
            onBackground = n.paper,
            surface = n.night,
            onSurface = n.paper,
            surfaceVariant = n.nightContainer,
            onSurfaceVariant = n.paperMuted,
            surfaceDim = n.nightLowest,
            surfaceBright = n.nightHighest,
            surfaceContainerLowest = n.nightLowest,
            surfaceContainerLow = n.nightLow,
            surfaceContainer = n.nightContainer,
            surfaceContainerHigh = n.nightHigh,
            surfaceContainerHighest = n.nightHighest,
            surfaceTint = Color.Transparent,
            inverseSurface = n.paper,
            inverseOnSurface = n.ink,
            inversePrimary = accent.seed,
            outline = n.outlineDark,
            outlineVariant = n.ruleDark,
            scrim = Color.Black
        )
    }
}

@Immutable
data class NeutralTones(
    val mistTop: Color,
    val mistBottom: Color,
    val mist: Color,
    val mistLowest: Color,
    val mistLow: Color,
    val mistContainer: Color,
    val mistHigh: Color,
    val mistHighest: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val inkGhost: Color,
    val outlineLight: Color,
    val ruleLight: Color,
    val shadowInk: Color,
    val slate: Color,
    val slateContainer: Color,
    val inverseSurface: Color,
    val onInverseSurface: Color,
    val scrim: Color,
    val codeWell: Color,
    val codeWellHeader: Color,
    val codeWellBorder: Color,
    val nightTop: Color,
    val nightBottom: Color,
    val night: Color,
    val nightLowest: Color,
    val nightLow: Color,
    val nightContainer: Color,
    val nightHigh: Color,
    val nightHighest: Color,
    val paper: Color,
    val paperMuted: Color,
    val paperFaint: Color,
    val paperGhost: Color,
    val outlineDark: Color,
    val ruleDark: Color,
    val shadowNight: Color,
    val slateLift: Color,
    val onSlateLift: Color,
    val smoke: Color,
    val sheetDark: Color,
    val codeWellDark: Color,
    val codeWellHeaderDark: Color,
    val codeWellBorderDark: Color
) {
    companion object {
        const val BASE_HUE = 266.0

        val Defaults = NeutralTones(
            mistTop = DshPalette.MistTop,
            mistBottom = DshPalette.MistBottom,
            mist = DshPalette.Mist,
            mistLowest = DshPalette.MistLowest,
            mistLow = DshPalette.MistLow,
            mistContainer = DshPalette.MistContainer,
            mistHigh = DshPalette.MistHigh,
            mistHighest = DshPalette.MistHighest,
            ink = DshPalette.Ink,
            inkMuted = DshPalette.InkMuted,
            inkFaint = DshPalette.InkFaint,
            inkGhost = DshPalette.InkGhost,
            outlineLight = DshPalette.OutlineLight,
            ruleLight = DshPalette.RuleLight,
            shadowInk = DshPalette.ShadowInk,
            slate = DshPalette.Slate,
            slateContainer = DshPalette.SlateContainer,
            inverseSurface = Color(0xFF252A35),
            onInverseSurface = Color(0xFFECEEF4),
            scrim = DshPalette.Scrim,
            codeWell = Color(0xFFEDF0F6),
            codeWellHeader = Color(0xFFE7EBF3),
            codeWellBorder = Color(0xFFDBDEE5),
            nightTop = DshPalette.NightTop,
            nightBottom = DshPalette.NightBottom,
            night = DshPalette.Night,
            nightLowest = DshPalette.NightLowest,
            nightLow = DshPalette.NightLow,
            nightContainer = DshPalette.NightContainer,
            nightHigh = DshPalette.NightHigh,
            nightHighest = DshPalette.NightHighest,
            paper = DshPalette.Paper,
            paperMuted = DshPalette.PaperMuted,
            paperFaint = DshPalette.PaperFaint,
            paperGhost = DshPalette.PaperGhost,
            outlineDark = DshPalette.OutlineDark,
            ruleDark = DshPalette.RuleDark,
            shadowNight = DshPalette.ShadowNight,
            slateLift = DshPalette.SlateLift,
            onSlateLift = DshPalette.OnSlateLift,
            smoke = Color(0xFF292E39),
            sheetDark = Color(0xFF232832),
            codeWellDark = Color(0xFF14171E),
            codeWellHeaderDark = Color(0xFF1A1E26),
            codeWellBorderDark = Color(0xFF2F333A)
        )

        private val cache = HashMap<Pair<Double, Double>, NeutralTones>()

        fun forAccent(accent: AppAccent): NeutralTones =
            of(tintHue = accent.tintHue, tintChroma = accent.tintChroma)

        fun of(tintHue: Double, tintChroma: Double): NeutralTones {
            if (tintHue == BASE_HUE && tintChroma == 1.0) return Defaults
            val key = tintHue to tintChroma
            synchronized(cache) {
                cache[key]?.let { return it }
                if (cache.size > 32) cache.clear()
                val shift = tintHue - BASE_HUE
                fun t(c: Color): Color = retint(c, hueShift = shift, chromaScale = tintChroma)
                val m = Defaults
                return NeutralTones(
                    mistTop = t(m.mistTop),
                    mistBottom = t(m.mistBottom),
                    mist = t(m.mist),
                    mistLowest = t(m.mistLowest),
                    mistLow = t(m.mistLow),
                    mistContainer = t(m.mistContainer),
                    mistHigh = t(m.mistHigh),
                    mistHighest = t(m.mistHighest),
                    ink = t(m.ink),
                    inkMuted = t(m.inkMuted),
                    inkFaint = t(m.inkFaint),
                    inkGhost = t(m.inkGhost),
                    outlineLight = t(m.outlineLight),
                    ruleLight = t(m.ruleLight),
                    shadowInk = t(m.shadowInk),
                    slate = t(m.slate),
                    slateContainer = t(m.slateContainer),
                    inverseSurface = t(m.inverseSurface),
                    onInverseSurface = t(m.onInverseSurface),
                    scrim = t(m.scrim),
                    codeWell = t(m.codeWell),
                    codeWellHeader = t(m.codeWellHeader),
                    codeWellBorder = t(m.codeWellBorder),
                    nightTop = t(m.nightTop),
                    nightBottom = t(m.nightBottom),
                    night = t(m.night),
                    nightLowest = t(m.nightLowest),
                    nightLow = t(m.nightLow),
                    nightContainer = t(m.nightContainer),
                    nightHigh = t(m.nightHigh),
                    nightHighest = t(m.nightHighest),
                    paper = t(m.paper),
                    paperMuted = t(m.paperMuted),
                    paperFaint = t(m.paperFaint),
                    paperGhost = t(m.paperGhost),
                    outlineDark = t(m.outlineDark),
                    ruleDark = t(m.ruleDark),
                    shadowNight = t(m.shadowNight),
                    slateLift = t(m.slateLift),
                    onSlateLift = t(m.onSlateLift),
                    smoke = t(m.smoke),
                    sheetDark = t(m.sheetDark),
                    codeWellDark = t(m.codeWellDark),
                    codeWellHeaderDark = t(m.codeWellHeaderDark),
                    codeWellBorderDark = t(m.codeWellBorderDark)
                ).also { cache[key] = it }
            }
        }
    }
}
