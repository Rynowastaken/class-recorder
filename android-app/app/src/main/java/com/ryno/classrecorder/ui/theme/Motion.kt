package com.ryno.classrecorder.ui.theme

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * DSH Go motion tokens & live system Reduce Motion observer, ported from motion.dart.
 */
object DshMotion {
    const val QUICK_MS = 160
    const val STANDARD_MS = 240
    const val COLLAPSE_MS = 280
    const val EMPHASIZED_MS = 340

    const val PRESS_SCALE = 0.97f

    val StandardEasing: Easing = FastOutSlowInEasing
    val GlideEasing: Easing = CubicBezierEasing(0.32f, 0.72f, 0.0f, 1.0f)
    val CollapseEasing: Easing = CubicBezierEasing(0.65f, 0.0f, 0.35f, 1.0f)

    fun <T> pressSpring(reduced: Boolean = false): FiniteAnimationSpec<T> =
        if (reduced) tween(0)
        else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    fun <T> thumbSpring(reduced: Boolean = false): FiniteAnimationSpec<T> =
        if (reduced) tween(0)
        else spring(dampingRatio = 0.84f, stiffness = 520f)

    fun <T> quickTween(reduced: Boolean = false): TweenSpec<T> =
        tween(durationMillis = if (reduced) 0 else QUICK_MS, easing = StandardEasing)

    fun <T> standardTween(reduced: Boolean = false): TweenSpec<T> =
        tween(durationMillis = if (reduced) 0 else STANDARD_MS, easing = StandardEasing)

    fun <T> collapseTween(reduced: Boolean = false): TweenSpec<T> =
        tween(durationMillis = if (reduced) 0 else COLLAPSE_MS, easing = CollapseEasing)

    fun <T> glideTween(reduced: Boolean = false): TweenSpec<T> =
        tween(durationMillis = if (reduced) 0 else EMPHASIZED_MS, easing = GlideEasing)
}

val LocalReducedMotion = staticCompositionLocalOf { false }

private fun readAnimatorReduced(resolver: android.content.ContentResolver): Boolean =
    runCatching {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }.getOrDefault(false)

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    val resolver = context.contentResolver
    var reduced by remember(resolver) { mutableStateOf(readAnimatorReduced(resolver)) }

    DisposableEffect(resolver) {
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = readAnimatorReduced(resolver)
            }
        }
        runCatching { resolver.registerContentObserver(uri, false, observer) }
        onDispose {
            runCatching { resolver.unregisterContentObserver(observer) }
        }
    }
    return reduced
}
