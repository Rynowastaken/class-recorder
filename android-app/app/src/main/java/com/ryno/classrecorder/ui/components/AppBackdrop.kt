package com.ryno.classrecorder.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import kotlin.math.max

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
                    top = s.backdropTop,
                    bottom = s.backdropBottom,
                    glowPrimary = if (glow) s.glowPrimary else null,
                    glowSecondary = if (glow) s.glowSecondary else null
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

private fun DrawScope.drawBackdropLayer(
    top: Color,
    bottom: Color,
    glowPrimary: Color?,
    glowSecondary: Color?
) {
    drawRect(
        brush = Brush.verticalGradient(colors = listOf(top, bottom))
    )
    val longest = max(size.width, size.height)
    if (glowPrimary != null && longest > 0f) {
        val center = Offset(size.width * 0.92f, 0f)
        val radius = longest * 0.62f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowPrimary, glowPrimary.copy(alpha = 0f)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
    if (glowSecondary != null && longest > 0f) {
        val center = Offset(0f, size.height)
        val radius = longest * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowSecondary, glowSecondary.copy(alpha = 0f)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
    }
}
