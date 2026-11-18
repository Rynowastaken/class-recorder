package com.ryno.classrecorder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.ui.theme.AppSpace
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.DshSurfaces
import kotlin.math.max

/**
 * Computes `(startInset, endInset)` combining centered `AppSpace.readingWidth` (`720.dp`)
 * margins with landscape `displayCutout` and horizontal `navigationBars` safe insets.
 */
@Composable
fun rememberSafeSideInsets(maxWidth: Dp): Pair<Dp, Dp> {
    val layoutDirection = LocalLayoutDirection.current
    val safeHorizontal = WindowInsets.displayCutout
        .union(WindowInsets.navigationBars)
        .asPaddingValues()
    val centeredInset = ((maxWidth - AppSpace.readingWidth) / 2f).coerceAtLeast(0.dp)
    val start = maxOf(centeredInset, safeHorizontal.calculateStartPadding(layoutDirection))
    val end = maxOf(centeredInset, safeHorizontal.calculateEndPadding(layoutDirection))
    return start to end
}

/**
 * Reports normalized `0f..1f` scroll-under progress for a root tab's [LazyListState]
 * so the top [BackdropVeil] fades in smoothly once list content passes beneath the Chrome bar.
 */
@Composable
fun BindScrollUnderVeil(
    listState: LazyListState,
    onScrolledUnderChange: (Float) -> Unit
) {
    val density = LocalDensity.current
    val scrollFraction by remember(density) {
        val fadePx = with(density) { 28.dp.toPx() }
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / fadePx).coerceIn(0f, 1f)
        }
    }
    LaunchedEffect(scrollFraction) {
        onScrolledUnderChange(scrollFraction)
    }
}

/**
 * Full-bleed DSH Go page backdrop: vertical Mist/Slate gradient + two wide
 * low-alpha corner radial glows cached per canvas size.
 */
@Composable
fun DshBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val s = ClassRecorderTheme.surfaces
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val bgBrush = Brush.verticalGradient(
                    colors = listOf(s.backdropTop, s.backdropBottom),
                    startY = 0f,
                    endY = size.height
                )
                val longest = max(size.width, size.height)
                val center1 = Offset(size.width * 0.92f, 0f)
                val radius1 = (longest * 0.62f).coerceAtLeast(1f)
                val glow1 = Brush.radialGradient(
                    colors = listOf(s.glowPrimary, s.glowPrimary.copy(alpha = 0f)),
                    center = center1,
                    radius = radius1
                )
                val center2 = Offset(0f, size.height)
                val radius2 = (longest * 0.55f).coerceAtLeast(1f)
                val glow2 = Brush.radialGradient(
                    colors = listOf(s.glowSecondary, s.glowSecondary.copy(alpha = 0f)),
                    center = center2,
                    radius = radius2
                )
                onDrawBehind {
                    drawRect(brush = bgBrush)
                    if (longest > 0f) {
                        drawCircle(brush = glow1, radius = radius1, center = center1)
                        drawCircle(brush = glow2, radius = radius2, center = center2)
                    }
                }
            },
        content = content
    )
}

/**
 * Feathered backdrop band placed behind floating top/bottom chrome once content
 * scrolls underneath. Uses a single cached 3-stop gradient with zero Offscreen layer passes.
 */
@Composable
fun BackdropVeil(
    fromTop: Boolean,
    solidHeight: Dp,
    feather: Dp = 14.dp,
    alpha: Float = 1f,
    modifier: Modifier = Modifier
) {
    if (alpha <= 0.001f) return
    val s = ClassRecorderTheme.surfaces
    val totalHeight = solidHeight + feather
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight)
            .graphicsLayer {
                this.alpha = alpha.coerceIn(0f, 1f)
            }
            .drawWithCache {
                val bandH = size.height
                val featherFrac = if (bandH > 0f) (feather.toPx() / bandH).coerceIn(0f, 1f) else 0f
                val stops = if (fromTop) {
                    arrayOf(
                        0f to s.backdropTop,
                        (1f - featherFrac).coerceAtLeast(0f) to s.backdropTop.copy(alpha = 0.96f),
                        1f to Color.Transparent
                    )
                } else {
                    arrayOf(
                        0f to Color.Transparent,
                        featherFrac to s.backdropBottom.copy(alpha = 0.96f),
                        1f to s.backdropBottom
                    )
                }
                val brush = Brush.verticalGradient(*stops)
                onDrawBehind {
                    if (bandH > 0f) {
                        drawRect(brush = brush)
                    }
                }
            }
    )
}

internal fun DrawScope.drawBackdropLayer(
    canvasSize: Size,
    surfaces: DshSurfaces
) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(surfaces.backdropTop, surfaces.backdropBottom),
            startY = 0f,
            endY = canvasSize.height
        ),
        size = canvasSize
    )
    val longest = max(canvasSize.width, canvasSize.height)
    if (longest > 0f) {
        val center1 = Offset(canvasSize.width * 0.92f, 0f)
        val radius1 = longest * 0.62f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(surfaces.glowPrimary, surfaces.glowPrimary.copy(alpha = 0f)),
                center = center1,
                radius = radius1
            ),
            radius = radius1,
            center = center1
        )
        val center2 = Offset(0f, canvasSize.height)
        val radius2 = longest * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(surfaces.glowSecondary, surfaces.glowSecondary.copy(alpha = 0f)),
                center = center2,
                radius = radius2
            ),
            radius = radius2,
            center = center2
        )
    }
}
