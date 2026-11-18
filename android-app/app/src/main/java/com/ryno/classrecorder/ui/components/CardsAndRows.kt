package com.ryno.classrecorder.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryno.classrecorder.ui.theme.AppShape
import com.ryno.classrecorder.ui.theme.AppSpace
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.DshMotion
import com.ryno.classrecorder.ui.theme.GlassTier
import com.ryno.classrecorder.ui.theme.StatusTone
import com.ryno.classrecorder.ui.theme.tabular

/**
 * Hub overview card ported from DSH Go's overview_card.dart:
 * 52dp leading glyph slot, 16dp inset, 14dp gap, optional hairline + 44dp/48dp footer strip.
 */
@Composable
fun OverviewCard(
    leading: @Composable () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    footer: (@Composable RowScope.() -> Unit)? = null,
    onFooterClick: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val surfaces = ClassRecorderTheme.surfaces
    val dividerColor = if (surfaces.darkTheme) {
        Color.White.copy(alpha = 0.08f)
    } else {
        scheme.onSurface.copy(alpha = 0.08f)
    }

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        tier = GlassTier.Card,
        shape = AppShape.radiusLg
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (onClick != null) {
                            Modifier.clickable(role = Role.Button, onClick = onClick)
                        } else Modifier
                    )
                    .padding(start = 16.dp, top = 16.dp, end = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    leading()
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium.tabular(),
                            color = scheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    trailing()
                } else if (onClick != null) {
                    Spacer(Modifier.width(4.dp))
                    OverviewChevron()
                }
            }

            if (footer != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = dividerColor
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 48.dp)
                        .then(
                            if (onFooterClick != null) {
                                Modifier.clickable(role = Role.Button, onClick = onFooterClick)
                            } else Modifier
                        )
                        .padding(start = 0.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        content = footer
                    )
                    if (onFooterClick != null) {
                        Spacer(Modifier.width(4.dp))
                        OverviewChevron()
                    }
                }
            }
        }
    }
}

/**
 * Standard [OverviewCard] footer alignment: centers a [StatusDot] under the 52dp
 * avatar column and aligns the status text directly beneath the card title.
 */
@Composable
fun RowScope.OverviewFooterRow(
    tone: StatusTone,
    text: String,
    leading: (@Composable () -> Unit)? = null
) {
    val reduced = ClassRecorderTheme.reducedMotion
    Box(
        modifier = Modifier
            .width(16.dp + 52.dp)
            .padding(start = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (leading != null) {
            leading()
        } else {
            StatusDot(tone = tone, size = 8.dp)
        }
    }
    Spacer(Modifier.width(14.dp))
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            fadeIn(DshMotion.quickTween(reduced)) togetherWith fadeOut(DshMotion.quickTween(reduced))
        },
        modifier = Modifier.weight(1f),
        label = "overviewFooterText"
    ) { current ->
        Text(
            text = current,
            style = MaterialTheme.typography.bodySmall.tabular(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 52dp circular identity-tinted badge for an [OverviewCard] or [PageHero].
 */
@Composable
fun OverviewGlyph(
    icon: ImageVector,
    identityKey: String = "",
    modifier: Modifier = Modifier,
    tintSlot: Int? = null,
    size: Dp = 52.dp
) {
    val dsh = ClassRecorderTheme.dshColors
    val (bg, fg) = if (tintSlot != null) {
        dsh.identityAt(tintSlot)
    } else {
        dsh.identityPair(identityKey)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(size * 0.46f),
            tint = fg
        )
    }
}

/**
 * 32dp circular identity-tinted disc for settings and list rows.
 */
@Composable
fun SettingsIcon(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tintSlot: Int = 0,
    identityKey: String? = null,
    danger: Boolean = false,
    size: Dp = 32.dp
) {
    val dsh = ClassRecorderTheme.dshColors
    val (bg, fg) = when {
        danger -> dsh.dangerContainer to dsh.onDangerContainer
        identityKey != null -> dsh.identityPair(identityKey)
        else -> dsh.identityAt(tintSlot)
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = fg
        )
    }
}

@Composable
fun OverviewChevron(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Rounded.ChevronRight,
        contentDescription = null,
        modifier = modifier
            .size(20.dp)
            .offset(x = 3.dp),
        tint = ClassRecorderTheme.dshColors.textTertiary
    )
}

/**
 * Card-tier glass group for settings sections and day-grouped rows, ported from
 * DSH Go's ListPageCard and SettingsSection.
 *
 * Section titles use quiet sentence-case `labelLarge` in `textTertiary`.
 */
@Composable
fun ListPageCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    trailingTitle: String? = null,
    footer: String? = null,
    contentPadding: PaddingValues = PaddingValues(vertical = 4.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val dsh = ClassRecorderTheme.dshColors
    Column(modifier = modifier.fillMaxWidth()) {
        if (!title.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = dsh.textTertiary,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() }
                )
                if (!trailingTitle.isNullOrBlank()) {
                    Text(
                        text = trailingTitle,
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = dsh.textTertiary
                    )
                }
            }
        }
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            tier = GlassTier.Card,
            shape = AppShape.radiusLg,
            contentPadding = contentPadding
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
        if (!footer.isNullOrBlank()) {
            Text(
                text = footer,
                style = MaterialTheme.typography.bodySmall,
                color = dsh.textTertiary,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
            )
        }
    }
}

/**
 * Subtle 0.5dp inset divider for separating multi-line rows inside a [ListPageCard].
 */
@Composable
fun ListCardDivider(startIndent: Dp = 16.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = startIndent, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    )
}

/**
 * Shared row anatomy ported from DSH Go's ListPageRow / SettingsTile:
 * 52dp min height single-line, 64dp min height two-line, >= 48dp touch target.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListPageRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    leadingWidth: Dp = 32.dp,
    badge: (@Composable () -> Unit)? = null,
    value: String? = null,
    meta: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    showChevron: Boolean = onClick != null && trailing == null,
    danger: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val dsh = ClassRecorderTheme.dshColors
    val hasSubtitle = !subtitle.isNullOrBlank()
    val minHeight = if (hasSubtitle) 64.dp else 52.dp
    val interactionSource = remember { MutableInteractionSource() }

    val clickMod = when {
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .then(clickMod)
            .padding(
                start = 16.dp,
                top = 10.dp,
                end = if (showChevron || trailing != null) 12.dp else 16.dp,
                bottom = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null || icon != null) {
            Box(
                modifier = Modifier.width(leadingWidth),
                contentAlignment = Alignment.Center
            ) {
                if (leading != null) {
                    leading()
                } else if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (danger) dsh.danger else scheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = if (danger) dsh.danger else scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    badge()
                }
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = dsh.textTertiary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!value.isNullOrBlank()) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.tabular(),
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End
            )
        }
        if (!meta.isNullOrBlank()) {
            Spacer(Modifier.width(12.dp))
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = dsh.textTertiary,
                maxLines = 1
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
        if (showChevron) {
            Spacer(Modifier.width(4.dp))
            OverviewChevron()
        }
    }
}

/**
 * Full-row toggle tile ported from DSH Go's SettingsSwitchTile.
 */
@Composable
fun SettingsSwitchTile(
    title: String,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    tintSlot: Int = 0
) {
    val haptics = LocalHapticFeedback.current
    val scheme = MaterialTheme.colorScheme
    val toggle: (() -> Unit)? = if (onCheckedChange != null) {
        {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onCheckedChange(!checked)
        }
    } else null

    ListPageRow(
        title = title,
        subtitle = subtitle,
        modifier = modifier,
        leading = leading ?: icon?.let {
            { SettingsIcon(icon = it, tintSlot = tintSlot) }
        },
        onClick = toggle,
        showChevron = false,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = if (onCheckedChange != null) {
                    { next ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onCheckedChange(next)
                    }
                } else null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = scheme.onPrimary,
                    checkedTrackColor = scheme.primary,
                    uncheckedThumbColor = scheme.onSurfaceVariant,
                    uncheckedTrackColor = scheme.surfaceContainerHighest,
                    uncheckedBorderColor = scheme.outlineVariant
                )
            )
        }
    )
}

/**
 * Recessed monospace well for MCP URLs, ADB commands, and generated filenames,
 * using DSH Go's codeWellBg / codeWellBorder tokens.
 */
@Composable
fun CodeWell(
    text: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    onCopy: (() -> Unit)? = null
) {
    val dsh = ClassRecorderTheme.dshColors
    val mono = ClassRecorderTheme.mono

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppShape.radiusSm)
            .background(dsh.codeWellBg)
            .border(0.75.dp, dsh.codeWellBorder, AppShape.radiusSm)
    ) {
        if (!label.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(dsh.codeWellHeader)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = dsh.textTertiary,
                    modifier = Modifier.weight(1f)
                )
            }
            HorizontalDivider(thickness = 0.5.dp, color = dsh.codeWellBorder)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .then(
                    if (onCopy != null) {
                        Modifier.clickable(
                            role = Role.Button,
                            onClickLabel = "Copy",
                            onClick = onCopy
                        )
                    } else Modifier
                )
                .padding(start = 12.dp, top = 8.dp, end = if (onCopy != null) 4.dp else 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = mono.monoSmall,
                color = dsh.codeWellFg,
                modifier = Modifier.weight(1f)
            )
            if (onCopy != null) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(16.dp),
                        tint = dsh.textTertiary
                    )
                }
            }
        }
    }
}

/**
 * Recessed borderless text input field matching DSH Go's quiet capsule/well inputs.
 */
@Composable
fun DshTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 6,
    mono: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = if (singleLine) ImeAction.Done else ImeAction.Default,
    onImeAction: (() -> Unit)? = null
) {
    val surfaces = ClassRecorderTheme.surfaces
    val dsh = ClassRecorderTheme.dshColors
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current

    val wellFill = if (surfaces.darkTheme) {
        Color.White.copy(alpha = 0.065f)
    } else {
        scheme.onSurface.copy(alpha = 0.05f)
    }
    val textStyle = if (mono) {
        ClassRecorderTheme.mono.mono.copy(color = scheme.onSurface)
    } else {
        MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (!label.isNullOrBlank()) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = dsh.textTertiary,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clip(AppShape.radiusMd)
                .background(wellFill)
                .border(
                    width = 0.5.dp,
                    color = scheme.outlineVariant.copy(alpha = 0.6f),
                    shape = AppShape.radiusMd
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty() && placeholder.isNotEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(color = dsh.textTertiary),
                        maxLines = if (singleLine) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    minLines = minLines,
                    maxLines = maxLines,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(scheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = imeAction),
                    keyboardActions = KeyboardActions(
                        onAny = {
                            onImeAction?.invoke() ?: focusManager.clearFocus()
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

/**
 * Centered empty / not-yet state ported from DSH Go's empty_view.dart:
 * circular Card-glass badge, sentence-case title, concise subtitle, and at most one action.
 */
@Composable
fun EmptyView(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    compact: Boolean = false
) {
    val dsh = ClassRecorderTheme.dshColors
    val scheme = MaterialTheme.colorScheme
    val badgeSize = if (compact) 48.dp else 64.dp
    val iconSize = if (compact) 22.dp else 28.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (compact) 24.dp else 32.dp, vertical = if (compact) 20.dp else 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlassSurface(
            modifier = Modifier.size(badgeSize),
            tier = GlassTier.Card,
            circle = true
        ) {
            Box(
                modifier = Modifier.size(badgeSize),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(iconSize),
                    tint = scheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(if (compact) 14.dp else 18.dp))
        Text(
            text = title,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            color = scheme.onSurface,
            textAlign = TextAlign.Center
        )
        if (!message.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = message,
                style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                color = dsh.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.sizeIn(maxWidth = 300.dp)
            )
        }
        if (!actionLabel.isNullOrBlank() && onAction != null) {
            Spacer(Modifier.height(if (compact) 14.dp else 20.dp))
            DshSecondaryButton(
                label = actionLabel,
                onClick = onAction
            )
        }
    }
}

/**
 * Primary stadium action button with DSH Go press-scale spring and >= 48dp hit height.
 */
@Composable
fun DshPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    minHeight: Dp = 48.dp
) {
    val scheme = MaterialTheme.colorScheme
    val dsh = ClassRecorderTheme.dshColors
    val reduced = ClassRecorderTheme.reducedMotion
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && pressed && !reduced) DshMotion.PRESS_SCALE else 1f,
        animationSpec = DshMotion.pressSpring(reduced),
        label = "primaryBtnScale"
    )
    val bg = when {
        !enabled -> scheme.surfaceContainerHighest
        danger -> dsh.danger
        else -> scheme.primary
    }
    val fg = when {
        !enabled -> dsh.textDisabled
        danger -> dsh.onDanger
        else -> scheme.onPrimary
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minHeight = minHeight)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = ripple(),
                        role = Role.Button,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = fg
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Quiet Pill-tier stadium secondary button with >= 48dp touch height.
 */
@Composable
fun DshSecondaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    minHeight: Dp = 48.dp
) {
    val scheme = MaterialTheme.colorScheme
    val dsh = ClassRecorderTheme.dshColors
    val fg = when {
        !enabled -> dsh.textDisabled
        danger -> dsh.danger
        else -> scheme.onSurface
    }
    GlassSurface(
        modifier = modifier.defaultMinSize(minHeight = minHeight),
        tier = GlassTier.Pill,
        stadium = true,
        enabled = enabled,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = fg
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
