package com.ryno.classrecorder.ui.components

import android.graphics.BlurMaskFilter
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.ui.theme.AppShape
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.DshMotion
import com.ryno.classrecorder.ui.theme.GlassShadow
import com.ryno.classrecorder.ui.theme.GlassStyle
import com.ryno.classrecorder.ui.theme.GlassTier
import kotlin.math.max

/**
 * Live GPU [GraphicsLayer] recorded from the scrolling scene underneath floating
 * Chrome surfaces so [GlassSurface] can refract and blur real underlying pixels on Android 12+.
 */
val LocalBackdropLayer = compositionLocalOf<GraphicsLayer?> { null }

/**
 * Signals whether the enclosing Dialog/Sheet window has active OS-level cross-window blur.
 */
val LocalWindowBlurActive = compositionLocalOf { false }

/**
 * Four-tier DSH Go glass surface, ported from glass.dart:
 *  1. Outer-only clipped drop shadows (never muddies translucent interior)
 *  2. Real Android 12+ GraphicsLayer / Window backdrop blur for Chrome & Sheet tiers
 *     (automatically degrading to opaque/frosted fallbackFill when unsupported or reducedMotion)
 *  3. Inset 0.5dp hairline stroke + top-to-45% vertical rim highlight
 *  4. Press scale (0.97f) + LocalContentColor = colorScheme.onSurface
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    tier: GlassTier = GlassTier.Card,
    shape: Shape? = null,
    circle: Boolean = false,
    stadium: Boolean = false,
    styleOverride: GlassStyle? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val surfaces = ClassRecorderTheme.surfaces
    val reducedMotion = ClassRecorderTheme.reducedMotion
    val density = LocalDensity.current
    val backdropLayer = LocalBackdropLayer.current
    val windowBlurActive = LocalWindowBlurActive.current
    val s = styleOverride ?: surfaces.glass(tier)

    val resolvedShape: Shape = when {
        circle -> CircleShape
        stadium -> AppShape.pill
        shape != null -> shape
        else -> when (tier) {
            GlassTier.Chrome -> AppShape.radiusXl
            GlassTier.Sheet -> AppShape.sheetTop
            GlassTier.Card -> AppShape.radiusMd
            GlassTier.Pill -> AppShape.pill
        }
    }

    val canSampleLayerBlur = s.blurs &&
        surfaces.blurEnabled &&
        !reducedMotion &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        backdropLayer != null &&
        tier != GlassTier.Sheet

    val canUseWindowBlur = s.blurs &&
        surfaces.blurEnabled &&
        !reducedMotion &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        windowBlurActive &&
        tier == GlassTier.Sheet

    val fill = when {
        !s.blurs -> if (surfaces.blurEnabled) s.fill else s.fallbackFill
        canSampleLayerBlur || canUseWindowBlur -> s.fill
        else -> s.fallbackFill
    }

    var rootOffset by remember { mutableStateOf(Offset.Zero) }
    val blurRadiusPx = remember(s.blurSigma, density) {
        with(density) { s.blurSigma.dp.toPx() }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val interactive = enabled && (onClick != null || onLongClick != null)
    val scale by animateFloatAsState(
        targetValue = if (interactive && isPressed && !reducedMotion) DshMotion.PRESS_SCALE else 1f,
        animationSpec = DshMotion.pressSpring(reducedMotion),
        label = "glassPressScale"
    )

    val clickModifier = when {
        !interactive -> Modifier
        onLongClick != null -> Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = { onClick?.invoke() },
            onLongClick = onLongClick
        )
        onClick != null -> Modifier.clickable(
            interactionSource = interactionSource,
            indication = ripple(),
            onClick = onClick
        )
        else -> Modifier
    }

    Box(
        propagateMinConstraints = true,
        modifier = modifier
            .then(
                if (canSampleLayerBlur) {
                    Modifier.onGloballyPositioned { coords ->
                        rootOffset = coords.positionInRoot()
                    }
                } else Modifier
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawOuterGlassShadows(shape = resolvedShape, shadows = s.shadows)
            .clip(resolvedShape)
            .then(clickModifier)
            .drawGlassEdges(
                shape = resolvedShape,
                hairline = s.hairline,
                hairlineWidthDp = s.hairlineWidth.value,
                rim = s.rim
            )
    ) {
        if (canSampleLayerBlur && blurRadiusPx > 0.5f) {
            val layerToDraw = backdropLayer ?: return@Box
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        renderEffect = BlurEffect(
                            radiusX = blurRadiusPx,
                            radiusY = blurRadiusPx,
                            edgeTreatment = TileMode.Clamp
                        )
                    }
                    .drawBehind {
                        val rootW = max(size.width, layerToDraw.size.width.toFloat())
                        val rootH = max(size.height, layerToDraw.size.height.toFloat())
                        translate(left = -rootOffset.x, top = -rootOffset.y) {
                            drawBackdropLayer(
                                canvasSize = Size(rootW, rootH),
                                surfaces = surfaces,
                                glow = true
                            )
                            drawLayer(layerToDraw)
                        }
                    }
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(fill)
        )
        Box(modifier = Modifier.padding(contentPadding)) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface,
                // Nested GlassSurfaces inside Chrome/Sheet should not re-blur an already blurred surface
                LocalBackdropLayer provides if (canSampleLayerBlur) null else backdropLayer
            ) {
                content()
            }
        }
    }
}

private fun Modifier.drawGlassEdges(
    shape: Shape,
    hairline: Color,
    hairlineWidthDp: Float,
    rim: Color
): Modifier = this.drawWithContent {
    drawContent()
    if (size.isEmpty()) return@drawWithContent
    val strokePx = max(hairlineWidthDp.dp.toPx(), 1f)
    val inset = strokePx * 0.5f
    val innerSize = Size(
        width = (size.width - strokePx).coerceAtLeast(0f),
        height = (size.height - strokePx).coerceAtLeast(0f)
    )
    if (innerSize.isEmpty()) return@drawWithContent

    val outline = shape.createOutline(innerSize, layoutDirection, this)
    val path = outlineToPath(outline, offsetX = inset, offsetY = inset)

    if (hairline.alpha > 0f) {
        drawPath(
            path = path,
            color = hairline,
            style = Stroke(width = strokePx)
        )
    }
    if (rim.alpha > 0f) {
        drawPath(
            path = path,
            brush = Brush.verticalGradient(
                0f to rim,
                0.45f to rim.copy(alpha = 0f),
                startY = 0f,
                endY = size.height
            ),
            style = Stroke(width = max(strokePx, 1.1f))
        )
    }
}

private fun Modifier.drawOuterGlassShadows(
    shape: Shape,
    shadows: List<GlassShadow>
): Modifier {
    if (shadows.isEmpty()) return this
    return this.drawBehind {
        if (size.isEmpty()) return@drawBehind
        val shapeOutline = shape.createOutline(size, layoutDirection, this)
        val innerPath = outlineToPath(shapeOutline, 0f, 0f)
        var maxExtent = 0f
        for (sh in shadows) {
            maxExtent = max(maxExtent, sh.blurRadius.toPx() * 2.2f + sh.offsetY.toPx())
        }
        val outerBounds = Path().apply {
            addRect(
                Rect(
                    left = -maxExtent,
                    top = -maxExtent,
                    right = size.width + maxExtent,
                    bottom = size.height + maxExtent
                )
            )
        }
        val exteriorClip = Path.combine(PathOperation.Difference, outerBounds, innerPath)

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val saveCount = native.save()
            native.clipPath(exteriorClip.asAndroidPath())
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                style = android.graphics.Paint.Style.FILL
            }
            for (sh in shadows) {
                if (sh.color.alpha <= 0f) continue
                val blurPx = sh.blurRadius.toPx()
                paint.color = sh.color.toArgb()
                paint.maskFilter = if (blurPx > 0.5f) {
                    BlurMaskFilter(blurPx, BlurMaskFilter.Blur.NORMAL)
                } else null
                val shiftedOutline = shape.createOutline(size, layoutDirection, this)
                val shadowPath = outlineToPath(shiftedOutline, 0f, sh.offsetY.toPx())
                native.drawPath(shadowPath.asAndroidPath(), paint)
            }
            native.restoreToCount(saveCount)
        }
    }
}

private fun outlineToPath(outline: Outline, offsetX: Float, offsetY: Float): Path = Path().apply {
    when (outline) {
        is Outline.Rectangle -> addRect(outline.rect.translate(offsetX, offsetY))
        is Outline.Rounded -> addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                rect = outline.roundRect.let {
                    Rect(it.left + offsetX, it.top + offsetY, it.right + offsetX, it.bottom + offsetY)
                },
                topLeft = outline.roundRect.topLeftCornerRadius,
                topRight = outline.roundRect.topRightCornerRadius,
                bottomRight = outline.roundRect.bottomRightCornerRadius,
                bottomLeft = outline.roundRect.bottomLeftCornerRadius
            )
        )
        is Outline.Generic -> {
            addPath(outline.path, Offset(offsetX, offsetY))
        }
    }
}
