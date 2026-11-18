package com.ryno.classrecorder.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.ui.theme.AppSpace
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.DshMotion
import com.ryno.classrecorder.ui.theme.tabular

/**
 * Secondary full-screen page frame ported from DSH Go's AppPage in page_frame.dart.
 *
 * Isolates pointer hit-testing and TalkBack traversal from any underlying route,
 * records scroll content into a GPU [rememberGraphicsLayer] so floating Chrome buttons
 * refract real pixels on Android 12+, and feathers both top and bottom system-bar zones.
 */
@Composable
fun AppPage(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    hero: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.() -> Unit
) {
    val density = LocalDensity.current
    val reduced = ClassRecorderTheme.reducedMotion
    val listState = rememberLazyListState()
    val backdropLayer = rememberGraphicsLayer()
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val barHeight = 56.dp
    val topReserved = statusTop + barHeight

    val defaultHeaderPx = with(density) { 72.dp.toPx() }
    var headerHeightPx by remember { mutableFloatStateOf(defaultHeaderPx) }

    val scrollOffsetPx by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.toFloat()
            } else {
                headerHeightPx + 1000f
            }
        }
    }

    val headerAlpha = (1f - scrollOffsetPx / (headerHeightPx * 0.6f).coerceAtLeast(1f))
        .coerceIn(0f, 1f)
    val barTitleRisePx = with(density) { 20.dp.toPx() }
    val barTitleProgress = ((scrollOffsetPx - headerHeightPx * 0.4f) / barTitleRisePx)
        .coerceIn(0f, 1f)
    val veilFadePx = with(density) { 24.dp.toPx() }
    val veilAlpha = (scrollOffsetPx / veilFadePx).coerceIn(0f, 1f)
    val titleSlideOffsetPx = with(density) { 6.dp.toPx() } * (1f - barTitleProgress)

    val canScrollDown by remember { derivedStateOf { listState.canScrollForward } }
    val bottomVeilAlpha by animateFloatAsState(
        targetValue = if (canScrollDown) 0.94f else 0f,
        animationSpec = DshMotion.quickTween(reduced),
        label = "appPageBottomVeil"
    )

    DshBackdrop(
        modifier = modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }
            // Absorb non-interactive background taps so overlays never leak touches underneath.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val sideInset = ((maxWidth - AppSpace.readingWidth) / 2f).coerceAtLeast(0.dp)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        backdropLayer.record {
                            this@drawWithContent.drawContent()
                        }
                        drawLayer(backdropLayer)
                    },
                contentPadding = PaddingValues(
                    start = sideInset,
                    end = sideInset,
                    top = topReserved,
                    bottom = navBottom + 44.dp
                )
            ) {
                item(key = "__app_page_header__") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onSizeChanged { sz ->
                                if (sz.height > 0) {
                                    headerHeightPx = sz.height.toFloat().coerceIn(
                                        with(density) { 40.dp.toPx() },
                                        with(density) { 420.dp.toPx() }
                                    )
                                }
                            }
                            .graphicsLayer {
                                alpha = headerAlpha
                            }
                    ) {
                        if (hero != null) {
                            hero()
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 24.dp, top = 6.dp, end = 24.dp, bottom = 20.dp)
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.semantics { heading() }
                                )
                                if (!subtitle.isNullOrBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                content()
            }

            BackdropVeil(
                fromTop = true,
                solidHeight = topReserved,
                feather = 12.dp,
                alpha = veilAlpha,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            BackdropVeil(
                fromTop = false,
                solidHeight = navBottom + 10.dp,
                feather = 20.dp,
                alpha = bottomVeilAlpha,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            CompositionLocalProvider(LocalBackdropLayer provides backdropLayer) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = statusTop)
                        .fillMaxWidth()
                        .height(barHeight)
                        .padding(horizontal = AppSpace.floatInset + sideInset),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .graphicsLayer {
                                alpha = barTitleProgress
                                translationY = titleSlideOffsetPx
                            }
                    )

                    Box(
                        modifier = Modifier.sizeIn(minWidth = 48.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            content = actions
                        )
                    }
                }
            }
        }
    }
}

/**
 * Detail-page header ported from DSH Go's PageHero in page_hero.dart.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PageHero(
    title: String,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    subtitle: String? = null,
    tags: List<String> = emptyList(),
    stats: List<Pair<String, String>> = emptyList()
) {
    val dsh = ClassRecorderTheme.dshColors
    val surfaces = ClassRecorderTheme.surfaces
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 4.dp, end = 24.dp, bottom = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.tabular(),
                        color = dsh.textTertiary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tags.forEach { rawTag ->
                    val label = if (rawTag.startsWith("#")) rawTag else "#$rawTag"
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (surfaces.darkTheme) scheme.onSurface.copy(alpha = 0.09f)
                                else scheme.onSurface.copy(alpha = 0.06f)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (stats.isNotEmpty()) {
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                stats.forEach { (value, label) ->
                    HeroStat(
                        value = value,
                        label = label,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private val NumberThenUnitRegex = Regex("""^([0-9][0-9.,:]*)(\s*)([^0-9\s.,:+\-].*)$""")

@Composable
fun HeroStat(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val dsh = ClassRecorderTheme.dshColors
    val numberStyle = MaterialTheme.typography.titleLarge.tabular()
    val unitStyle = MaterialTheme.typography.labelMedium.copy(
        color = dsh.textTertiary,
        fontWeight = FontWeight.Normal
    )
    val match = NumberThenUnitRegex.matchEntire(value.trim())

    Column(modifier = modifier) {
        if (match == null) {
            Text(
                text = value,
                style = numberStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            val numPart = match.groupValues[1]
            val unitPart = match.groupValues[2] + match.groupValues[3]
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            fontSize = numberStyle.fontSize,
                            fontWeight = numberStyle.fontWeight,
                            fontFeatureSettings = "tnum",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        append(numPart)
                    }
                    withStyle(
                        SpanStyle(
                            fontSize = unitStyle.fontSize,
                            fontWeight = FontWeight.Normal,
                            color = dsh.textTertiary
                        )
                    ) {
                        append(unitPart)
                    }
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = dsh.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Sentence-case group label above a card or day section, ported from DSH Go's PageSectionLabel.
 */
@Composable
fun PageSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    trailingText: String? = null
) {
    val dsh = ClassRecorderTheme.dshColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 16.dp, end = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = dsh.textTertiary,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = dsh.textTertiary
            )
        }
    }
}
