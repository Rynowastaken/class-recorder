package com.ryno.classrecorder.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DragScope
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
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
 */
@Composable
fun <T> SegmentedPill(
    segments: List<PillSegment<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    chromeGlass: Boolean = false,
    pagerState: PagerState? = null
) {
    if (segments.isEmpty()) return
    val surfaces = ClassRecorderTheme.surfaces
    val reduced = ClassRecorderTheme.reducedMotion
    val scheme = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    val selectedIndex = segments.indexOfFirst { it.value == selected }.coerceAtLeast(0)
    val lastIndex = segments.lastIndex.toFloat()
    var dragIndex by remember { mutableStateOf<Float?>(null) }
    val animatedIndex = if (pagerState == null) {
        val animation = remember { Animatable(selectedIndex.toFloat()) }
        val dragging = dragIndex != null
        LaunchedEffect(selectedIndex, dragging, reduced) {
            if (!dragging) animation.animateTo(selectedIndex.toFloat(), DshMotion.thumbSpring(reduced))
        }
        animation
    } else null
    val activeSlot = dragIndex?.roundToInt() ?: selectedIndex
    val reverseDrag = LocalLayoutDirection.current == LayoutDirection.Rtl

    fun select(value: T) {
        if (value != selected || pagerState?.isScrollInProgress == true) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelect(value)
        }
    }

    val scaledMinHeight = with(density) {
        val textPx = 13.5.sp.toPx() * 1.35f
        maxOf(48.dp, textPx.toDp() + 18.dp)
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
        ) {
            val count = segments.size
            val innerWidth = (maxWidth - inset * 2).coerceAtLeast(0.dp)
            val segmentWidth = innerWidth / count
            val stepPx = with(density) { segmentWidth.toPx() }.coerceAtLeast(1f)
            val insetPx = with(density) { inset.toPx().roundToInt() }
            val dragState = if (pagerState != null) {
                remember(pagerState, stepPx, haptics) {
                    object : DraggableState {
                        // A segment's travel on the bar maps to one full pager page.
                        private fun scrollDelta(delta: Float): Float = with(pagerState.layoutInfo) {
                            delta * (pageSize + pageSpacing) / stepPx
                        }

                        override suspend fun drag(
                            dragPriority: MutatePriority,
                            block: suspend DragScope.() -> Unit
                        ) {
                            pagerState.scroll(dragPriority) {
                                val dragScope = object : DragScope {
                                    override fun dragBy(pixels: Float) {
                                        val previousPage = pagerState.currentPage
                                        scrollBy(scrollDelta(pixels))
                                        if (pagerState.currentPage != previousPage) {
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                    }
                                }
                                dragScope.block()
                            }
                        }

                        override fun dispatchRawDelta(delta: Float) {
                            pagerState.dispatchRawDelta(scrollDelta(delta))
                        }
                    }
                }
            } else {
                rememberDraggableState { delta ->
                    val previousIndex = dragIndex ?: selectedIndex.toFloat()
                    val nextIndex = (previousIndex + delta / stepPx).coerceIn(0f, lastIndex)
                    if (previousIndex.roundToInt() != nextIndex.roundToInt()) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    dragIndex = nextIndex
                }
            }
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
                        val index = if (pagerState != null) {
                            pagerState.currentPage + pagerState.currentPageOffsetFraction
                        } else {
                            dragIndex ?: animatedIndex?.value ?: selectedIndex.toFloat()
                        }
                        IntOffset(
                            x = insetPx + (index.coerceIn(0f, lastIndex) * stepPx).roundToInt(),
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
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Horizontal,
                        enabled = count > 1,
                        reverseDirection = reverseDrag,
                        onDragStarted = {
                            if (animatedIndex != null) {
                                dragIndex = animatedIndex.value.coerceIn(0f, lastIndex)
                            }
                        },
                        onDragStopped = {
                            if (pagerState != null) {
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage,
                                    animationSpec = DshMotion.quickTween(reduced)
                                )
                            } else if (animatedIndex != null) {
                                dragIndex?.let { index ->
                                    animatedIndex.snapTo(index)
                                    select(segments[index.roundToInt()].value)
                                    dragIndex = null
                                }
                            }
                        }
                    )
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
                                onClick = { select(seg.value) }
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
 */
@Composable
fun CapsuleChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    courseKey: String? = null
) {
    val surfaces = ClassRecorderTheme.surfaces
    val dsh = ClassRecorderTheme.dshColors
    val reduced = ClassRecorderTheme.reducedMotion
    val scheme = MaterialTheme.colorScheme
    val isDark = surfaces.darkTheme

    val resolvedDot = courseKey?.let {
        dsh.courseDot(key = it, onDarkSurface = if (selected) !isDark else isDark)
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
            imageVector = DshIcons.Search,
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
                    imageVector = DshIcons.Close,
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
