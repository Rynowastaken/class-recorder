package com.ryno.classrecorder.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * DSH Go typography scale, ported from dsh_typography.dart.
 *
 * Hierarchy uses two structural weights (400 Normal / 600 SemiBold) plus 300 Light
 * for large display numerals/timers so CJK and Latin pair cleanly across OEM fonts.
 * Every numeric/clock style enables tabular figures ("tnum").
 */
private const val TABULAR_FIGURES = "tnum"

val SansFamily: FontFamily = FontFamily.Default
val MonoTypographyFamily: FontFamily = FontFamily.Monospace

private val EvenLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private val CleanPlatformText = PlatformTextStyle(includeFontPadding = false)

fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR_FIGURES)

@Immutable
data class MonoTypography(
    val mono: TextStyle,
    val monoSmall: TextStyle,
    val timer: TextStyle,
    val timerCompact: TextStyle,
    val seekLabel: TextStyle,
    val timerCaption: TextStyle
)

val DshMonoTypography = MonoTypography(
    mono = TextStyle(
        fontFamily = MonoTypographyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        fontFeatureSettings = TABULAR_FIGURES,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    monoSmall = TextStyle(
        fontFamily = MonoTypographyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = TABULAR_FIGURES,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    timer = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-1.0).sp,
        fontFeatureSettings = TABULAR_FIGURES,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    timerCompact = TextStyle(
        fontFamily = MonoTypographyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = TABULAR_FIGURES,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    seekLabel = TextStyle(
        fontFamily = MonoTypographyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontFeatureSettings = TABULAR_FIGURES,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    timerCaption = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    )
)

val DshTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.8).sp,
        fontFeatureSettings = TABULAR_FIGURES,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    displayMedium = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Light,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    displaySmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Light,
        fontSize = 28.sp,
        lineHeight = 35.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    headlineLarge = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    headlineMedium = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 29.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    headlineSmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 27.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    titleLarge = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 25.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    titleMedium = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    titleSmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    bodyLarge = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    bodyMedium = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    bodySmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    labelLarge = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    labelMedium = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    ),
    labelSmall = TextStyle(
        fontFamily = SansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.2.sp,
        platformStyle = CleanPlatformText,
        lineHeightStyle = EvenLineHeight
    )
)
