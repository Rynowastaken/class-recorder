package com.ryno.classrecorder.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.ui.theme.AppShape
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.DshMotion
import com.ryno.classrecorder.ui.theme.GlassTier
import com.ryno.classrecorder.ui.theme.StatusTone
import com.ryno.classrecorder.ui.theme.tabular

/**
 * 44dp circular chrome-glass button inside a >= 48dp Android touch target,
 * ported from DSH Go's GlassIconButton in page_frame.dart.
 */
@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tier: GlassTier = GlassTier.Chrome,
    visualSize: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    enabled: Boolean = true,
    badgeTone: StatusTone? = null
) {
    val scheme = MaterialTheme.colorScheme
    val dshColors = ClassRecorderTheme.dshColors
    val reducedMotion = ClassRecorderTheme.reducedMotion
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && isPressed && !reducedMotion) DshMotion.PRESS_SCALE else 1f,
        animationSpec = DshMotion.pressSpring(reducedMotion),
        label = "glassIconBtnScale"
    )

    // Outer 48dp touch target intentionally avoids .clip(CircleShape) so Chrome's
    // 24dp outer drop shadow is never hard-truncated into a 2dp dark halo.
    Box(
        modifier = modifier
            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = false, radius = visualSize / 2),
                        role = Role.Button,
                        onClickLabel = contentDescription,
                        onClick = onClick
                    )
                } else Modifier
            )
            .semantics {
                this.contentDescription = contentDescription
                this.role = Role.Button
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(visualSize)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        ) {
            GlassSurface(
                modifier = Modifier.size(visualSize),
                tier = tier,
                circle = true
            ) {
                Box(
                    modifier = Modifier.size(visualSize),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                        tint = if (enabled) scheme.onSurface else dshColors.textDisabled
                    )
                }
            }
            if (badgeTone != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 3.dp, end = 3.dp)
                ) {
                    StatusDot(tone = badgeTone, size = 7.dp)
                }
            }
        }
    }
}

/**
 * Collapsible pill capsule ported from DSH Go's glass_pill.dart:
 * stadium capsule at rest, smoothly morphs to 14dp rounded card when expanded.
 */
@Composable
fun GlassPill(
    summary: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = true,
    onClick: (() -> Unit)? = null,
    expandedContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    val reduced = ClassRecorderTheme.reducedMotion
    val expanded = expandedContent != null
    val cornerRadius by animateDpAsState(
        targetValue = if (expanded) AppShape.md else 24.dp,
        animationSpec = DshMotion.standardTween(reduced),
        label = "glassPillRadius"
    )
    GlassSurface(
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
        tier = GlassTier.Pill,
        shape = RoundedCornerShape(cornerRadius)
    ) {
        Column(
            modifier = if (fillWidth) Modifier.fillMaxWidth() else Modifier
        ) {
            Row(
                modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                    .defaultMinSize(minHeight = 48.dp)
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(role = Role.Button, onClick = onClick)
                        } else Modifier
                    )
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = summary
            )
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(DshMotion.collapseTween(reduced)) + fadeIn(DshMotion.quickTween(reduced)),
                exit = shrinkVertically(DshMotion.collapseTween(reduced)) + fadeOut(DshMotion.quickTween(reduced))
            ) {
                if (expandedContent != null) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            content = expandedContent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun statusDotColor(tone: StatusTone): Color {
    val dsh = ClassRecorderTheme.dshColors
    val scheme = MaterialTheme.colorScheme
    return when (tone) {
        StatusTone.Idle -> dsh.textTertiary
        StatusTone.Ok -> dsh.success
        StatusTone.Warn -> dsh.warning
        StatusTone.Error -> dsh.danger
        StatusTone.Busy -> scheme.primary
    }
}

/**
 * Animated status indicator dot ported from DSH Go's status_chip.dart.
 * Pulses gently on Busy or Error unless system Reduce Motion is active.
 */
@Composable
fun StatusDot(
    tone: StatusTone,
    size: Dp = 7.dp,
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    val reduced = ClassRecorderTheme.reducedMotion
    val color by animateColorAsState(
        targetValue = statusDotColor(tone),
        animationSpec = DshMotion.standardTween(reduced),
        label = "statusDotColor"
    )
    val shouldPulse = animated && !reduced && (tone == StatusTone.Busy || tone == StatusTone.Error)
    val pulseAlpha = if (shouldPulse) {
        val transition = rememberInfiniteTransition(label = "statusDotPulse")
        val a by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.38f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = DshMotion.StandardEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "statusDotAlpha"
        )
        a
    } else 1f

    Box(
        modifier = modifier
            .size(size)
            .alpha(pulseAlpha)
            .clip(CircleShape)
            .background(color)
    )
}

/**
 * Compact status pill `( ● Label )` ported from DSH Go's StatusChip.
 */
@Composable
fun StatusChip(
    tone: StatusTone,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val surfaces = ClassRecorderTheme.surfaces
    val dsh = ClassRecorderTheme.dshColors
    val scheme = MaterialTheme.colorScheme
    val reduced = ClassRecorderTheme.reducedMotion
    val isDark = surfaces.darkTheme
    val dot = statusDotColor(tone)

    val (targetBg, targetFg) = when (tone) {
        StatusTone.Idle -> (
            if (isDark) Color.White.copy(alpha = 0.08f) else scheme.onSurface.copy(alpha = 0.06f)
            ) to dsh.textTertiary
        StatusTone.Ok -> dot.copy(alpha = if (isDark) 0.20f else 0.12f) to
            (if (isDark) dsh.onSuccessContainer else dsh.success)
        StatusTone.Warn -> dot.copy(alpha = if (isDark) 0.20f else 0.12f) to
            (if (isDark) dsh.onWarningContainer else dsh.warning)
        StatusTone.Error -> dot.copy(alpha = if (isDark) 0.22f else 0.14f) to
            (if (isDark) dsh.onDangerContainer else dsh.danger)
        StatusTone.Busy -> dot.copy(alpha = if (isDark) 0.20f else 0.12f) to
            (if (isDark) scheme.onPrimaryContainer else scheme.primary)
    }

    val bg by animateColorAsState(targetBg, DshMotion.standardTween(reduced), label = "statusChipBg")
    val fg by animateColorAsState(targetFg, DshMotion.standardTween(reduced), label = "statusChipFg")

    val boxMod = if (onClick != null) {
        modifier
            .sizeIn(minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
    } else modifier

    Box(
        modifier = boxMod,
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 26.dp)
                .clip(CircleShape)
                .background(bg)
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusDot(tone = tone, size = 6.dp)
            Spacer(Modifier.width(6.dp))
            AnimatedContent(
                targetState = label,
                transitionSpec = {
                    fadeIn(DshMotion.quickTween(reduced)) togetherWith fadeOut(DshMotion.quickTween(reduced))
                },
                label = "statusChipText"
            ) { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium.tabular(),
                    color = fg,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
