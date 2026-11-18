package com.ryno.classrecorder.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.ryno.classrecorder.ui.DshThemeMode

val LocalDshSurfaces = staticCompositionLocalOf { DshSurfaces.dark(AppAccent.Mist) }
val LocalDshColors = staticCompositionLocalOf { DshColors.Dark }
val LocalMonoTypography = staticCompositionLocalOf { DshMonoTypography }

/**
 * Resolves the active [AppAccent], deriving a guarded OKLCH palette from
 * Android 12+ `system_accent1_500` when [accentId] == `"wallpaper"`.
 */
@Composable
fun resolveAppAccent(accentId: String): AppAccent {
    val context = LocalContext.current
    if (accentId == AppAccent.WALLPAPER_ID && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val rawArgb = remember(context) {
            runCatching { ContextCompat.getColor(context, android.R.color.system_accent1_500) }
                .getOrNull()
        }
        if (rawArgb != null) {
            return remember(rawArgb) { AppAccent.derive(Color(rawArgb)) }
        }
    }
    return AppAccent.preset(accentId)
}

@Composable
fun ClassRecorderTheme(
    themeMode: DshThemeMode = DshThemeMode.SYSTEM,
    accentId: String = AppAccent.Mist.id,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        DshThemeMode.LIGHT -> false
        DshThemeMode.DARK -> true
        DshThemeMode.SYSTEM -> systemDark
    }
    val accent = resolveAppAccent(accentId)
    val reducedMotion = rememberReducedMotion()
    val blurEnabled = !reducedMotion && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = remember(isDark, accent) {
        if (isDark) DshPalette.darkScheme(accent) else DshPalette.lightScheme(accent)
    }
    val surfaces = remember(isDark, accent, blurEnabled) {
        if (isDark) DshSurfaces.dark(accent, blurEnabled = blurEnabled)
        else DshSurfaces.light(accent, blurEnabled = blurEnabled)
    }
    val dshColors = remember(isDark, accent) {
        DshColors.forAccent(isDark, accent)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalDshSurfaces provides surfaces,
        LocalDshColors provides dshColors,
        LocalMonoTypography provides DshMonoTypography,
        LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DshTypography,
            shapes = DshMaterialShapes,
            content = content
        )
    }
}

object ClassRecorderTheme {
    val surfaces: DshSurfaces
        @Composable @ReadOnlyComposable
        get() = LocalDshSurfaces.current

    val dshColors: DshColors
        @Composable @ReadOnlyComposable
        get() = LocalDshColors.current

    val mono: MonoTypography
        @Composable @ReadOnlyComposable
        get() = LocalMonoTypography.current

    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReducedMotion.current
}
