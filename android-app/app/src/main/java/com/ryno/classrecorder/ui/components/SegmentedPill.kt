package com.ryno.classrecorder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.DshMotion
import com.ryno.classrecorder.ui.theme.GlassTier
import kotlin.math.roundToInt

data class PillSegment<T>(
    val value: T,
    val label: String
)

/**
 * Capsule segmented control ported from DSH Go's segmented_pill.dart.
 *
 * Equal-width segments inside a stadium track with a sliding capsule thumb.
 * Supports continuous [fractionalIndex] tracking (e.g. HorizontalPager swipe)
 * as well as direct tap and finger drag.
 */
@Composable
fun <T> SegmentedPill(
    segments: List<PillSegment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    chromeGlass: Boolean = false,
    fractionalIndex: Float? = null,
    height: Dp = 44.dp
) {
    if (segments.isEmpty()) return
    val surfaces = ClassRecorderTheme.surfaces
    val reduced = ClassRecorderTheme.reducedMotion
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val selectedIndex = segments.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    var dragFraction by remember { mutableStateOf<Float?>(null) }

    val animatedIndex by animateFloatAsState(
        targetValue = fractionalIndex?.coerceIn(0f, (segments.size - 1).toFloat())
            ?: selectedIndex.toFloat(),
        animationSpec = DshMotion.thumbSpring(reduced),
        label = "segmentedPillThumb"
    )
    val effectiveIndex = (dragFraction ?: animatedIndex).coerceIn(0f, (segments.size - 1).toFloat())
    val activeSlot = effectiveIndex.roundToInt().coerceIn(0, segments.lastIndex)

    // Grow height automatically at large system font scales so labels never clip.
    val scaledMinHeight = with(density) {
        val textPx = 13.5.sp.toPx() * 1.35f
        maxOf(height, textPx.toDp() + 18.dp, 48.dp)
    }

    val inset = 3.dp
    val isDark = surfaces.darkTheme
    val trackFill = when {
        chromeGlass -> Color.Transparent
        isDark -> Color.White.copy(alpha = 0.07f)
        else -> scheme.onSurface.copy(alpha = 0.06f)
    }
    val thumbFill = if (isDark) Color.White.copy(alpha = 0.20f) else Color.White

    val body: @Composable () -> Unit = {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(scaledMinHeight)
                .then(
                    if (!chromeGlass) {
                        Modifier
                            .clip(CircleShape)
                            .background(trackFill)
                    } else Modifier
                )
                .pointerInput(segments) {
                    val count = segments.size
                    if (count < 2) return@pointerInput
                    var lastSlot = selectedIndex
                    detectHorizontalDragGestures(
                        onDragStart = { startOffset ->
                            val segW = size.width.toFloat() / count
                            val slot = (startOffset.x / segW).coerceIn(0f, (count - 1).toFloat())
                            dragFraction = slot
                            lastSlot = slot.roundToInt()
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val segW = (size.width.toFloat() / count).coerceAtLeast(1f)
                            val next = ((dragFraction ?: selectedIndex.toFloat()) + dragAmount / segW)
                                .coerceIn(0f, (count - 1).toFloat())
                            val slot = next.roundToInt()
                            if (slot != lastSlot) {
                                lastSlot = slot
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            dragFraction = next
                        },
                        onDragEnd = {
                            val target = (dragFraction ?: selectedIndex.toFloat())
                                .roundToInt()
                                .coerceIn(0, count - 1)
                            dragFraction = null
                            val picked = segments[target].value
                            if (picked != selected) {
                                onSelect(picked)
                            }
                        },
                        onDragCancel = { dragFraction = null }
                    )
                }
        ) {
            val count = segments.size
            val innerWidth = (maxWidth - inset * 2).coerceAtLeast(0.dp)
            val segmentWidth = innerWidth / count
            val stepPx = with(density) { segmentWidth.toPx() }
            val insetPx = with(density) { inset.toPx().roundToInt() }
            val textMeasurer = rememberTextMeasurer()
            val baseLabelStyle = MaterialTheme.typography.labelLarge
            val maxLabelPx = with(density) { (segmentWidth - 10.dp).toPx().coerceAtLeast(16f) }

            val fittedFontSp = remember(segments, maxLabelPx, density.fontScale, density.density) {
                var candidate = 13.5f
                while (candidate > 10.0f) {
                    val fitsAll = segments.all { seg ->
                        val res = textMeasurer.measure(
                            text = seg.label,
                            style = baseLabelStyle.copy(
                                fontSize = candidate.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                        res.size.width <= maxLabelPx
                    }
                    if (fitsAll) break
                    candidate -= 0.5f
                }
                candidate.sp
            }

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = insetPx + (effectiveIndex * stepPx).roundToInt(),
                            y = insetPx
                        )
                    }
                    .width(segmentWidth)
                    .height((scaledMinHeight - inset * 2).coerceAtLeast(0.dp))
                    .then(
                        if (!isDark) Modifier.shadow(2.dp, CircleShape, clip = false)
                        else Modifier
                    )
                    .clip(CircleShape)
                    .background(thumbFill)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inset)
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                segments.forEachIndexed { index, seg ->
                    val isCurrent = index == activeSlot
                    val labelColor by animateColorAsState(
                        targetValue = if (isCurrent) scheme.onSurface else scheme.onSurfaceVariant,
                        animationSpec = DshMotion.quickTween(reduced),
                        label = "segLabelColor$index"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .selectable(
                                selected = index == selectedIndex,
                                role = Role.Tab,
                                onClick = {
                                    if (seg.value != selected) {
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onSelect(seg.value)
                                    }
                                }
                            )
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = seg.label,
                            style = baseLabelStyle.copy(
                                fontSize = fittedFontSp,
                                fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = labelColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (chromeGlass) {
        GlassSurface(
            modifier = modifier,
            tier = GlassTier.Chrome,
            stadium = true
        ) {
            body()
        }
    } else {
        Box(modifier = modifier) { body() }
    }
}

/**
 * Quiet filter capsule ported from DSH Go's capsule_chip.dart.
 * Selected state uses ink fill (onSurface bg + surface text) rather than brand flood.
 */
@Composable
fun CapsuleChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
    courseKey: String? = null
) {
    val surfaces = ClassRecorderTheme.surfaces
    val dsh = ClassRecorderTheme.dshColors
    val reduced = ClassRecorderTheme.reducedMotion
    val scheme = MaterialTheme.colorScheme
    val isDark = surfaces.darkTheme

    val resolvedDot = when {
        courseKey != null -> dsh.courseDot(
            key = courseKey,
            onDarkSurface = if (selected) !isDark else isDark
        )
        else -> dotColor
    }

    val bg by animateColorAsState(
        targetValue = when {
            selected -> scheme.onSurface
            isDark -> Color.White.copy(alpha = 0.07f)
            else -> scheme.onSurface.copy(alpha = 0.055f)
        },
        animationSpec = DshMotion.quickTween(reduced),
        label = "capsuleChipBg"
    )
    val fg by animateColorAsState(
        targetValue = if (selected) scheme.surface else scheme.onSurfaceVariant,
        animationSpec = DshMotion.quickTween(reduced),
        label = "capsuleChipFg"
    )

    Box(
        modifier = modifier
            .sizeIn(minHeight = 48.dp)
            .clip(CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 34.dp)
                .clip(CircleShape)
                .background(bg)
                .padding(horizontal = 13.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (resolvedDot != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(resolvedDot)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Borderless stadium search field ported from DSH Go's search_capsule.dart.
 */
@Composable
fun SearchCapsule(
    query: String,
    onQueryChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier
) {
    val surfaces = ClassRecorderTheme.surfaces
    val dsh = ClassRecorderTheme.dshColors
    val scheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current
    val fill = if (surfaces.darkTheme) {
        Color.White.copy(alpha = 0.07f)
    } else {
        scheme.onSurface.copy(alpha = 0.055f)
    }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .background(fill)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = dsh.textTertiary
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = dsh.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = scheme.onSurface),
                cursorBrush = SolidColor(scheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Clear search",
                        onClick = { onQueryChange("") }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear search",
                    modifier = Modifier.size(18.dp),
                    tint = dsh.textTertiary
                )
            }
        } else {
            Spacer(Modifier.width(12.dp))
        }
    }
}
