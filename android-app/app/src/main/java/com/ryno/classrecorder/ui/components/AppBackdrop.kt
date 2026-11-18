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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
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
 * Full-bleed DSH Go page backdrop: vertical Mist/Slate gradient + two wide
 * low-alpha corner radial glows so translucent glass picks up soft ambient light.
 *
 * Shared at the root of the app so swiping tabs keeps the background stationary.
 */
@Composable
fun DshBackdrop(
    modifier: Modifier = Modifier,
    glow: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val s = ClassRecorderTheme.surfaces
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                drawBackdropLayer(
                    canvasSize = size,
                    surfaces = s,
                    glow = glow
                )
            },
        content = content
    )
}

/**
 * Feathered backdrop band placed behind floating top/bottom chrome once content
 * scrolls underneath, ported from DSH Go's BackdropVeil in backdrop.dart.
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
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                val bandH = size.height
                if (bandH <= 0f) return@drawWithContent
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = if (fromTop) {
                            listOf(s.backdropTop, s.backdropTop.copy(alpha = 0.96f))
                        } else {
                            listOf(s.backdropBottom.copy(alpha = 0.96f), s.backdropBottom)
                        }
                    )
                )
                val featherFrac = (feather.toPx() / bandH).coerceIn(0f, 1f)
                val maskStops = if (fromTop) {
                    arrayOf(
                        0f to Color.Black,
                        (1f - featherFrac).coerceAtLeast(0f) to Color.Black,
                        1f to Color.Transparent
                    )
                } else {
                    arrayOf(
                        0f to Color.Transparent,
                        featherFrac to Color.Black,
                        1f to Color.Black
                    )
                }
                drawRect(
                    brush = Brush.verticalGradient(*maskStops),
                    blendMode = BlendMode.DstIn
                )
            }
    )
}

internal fun DrawScope.drawBackdropLayer(
    canvasSize: Size,
    surfaces: DshSurfaces,
    glow: Boolean = true
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
    if (glow && longest > 0f) {
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
