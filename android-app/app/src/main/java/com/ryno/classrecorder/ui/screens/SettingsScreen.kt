package com.ryno.classrecorder.ui.screens

import android.os.Build
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.ui.AppearancePreferences
import com.ryno.classrecorder.ui.DshThemeMode
import com.ryno.classrecorder.ui.MainUiState
import com.ryno.classrecorder.ui.components.AppPage
import com.ryno.classrecorder.ui.components.CapsuleChoiceChip
import com.ryno.classrecorder.ui.components.CodeWell
import com.ryno.classrecorder.ui.components.DshPrimaryButton
import com.ryno.classrecorder.ui.components.DshSecondaryButton
import com.ryno.classrecorder.ui.components.DshTextField
import com.ryno.classrecorder.ui.components.ListCardDivider
import com.ryno.classrecorder.ui.components.ListPageCard
import com.ryno.classrecorder.ui.components.ListPageRow
import com.ryno.classrecorder.ui.components.OverviewCard
import com.ryno.classrecorder.ui.components.OverviewFooterRow
import com.ryno.classrecorder.ui.components.OverviewGlyph
import com.ryno.classrecorder.ui.components.SettingsIcon
import com.ryno.classrecorder.ui.components.SettingsSwitchTile
import com.ryno.classrecorder.ui.theme.AppAccent
import com.ryno.classrecorder.ui.theme.AppShape
import com.ryno.classrecorder.ui.theme.AppSpace
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.DshMotion
import com.ryno.classrecorder.ui.theme.DshPalette
import com.ryno.classrecorder.ui.theme.DshSurfaces
import com.ryno.classrecorder.ui.theme.StatusTone
import com.ryno.classrecorder.ui.theme.resolveAppAccent

private const val DEFAULT_TEMPLATE = "{date}_{time}_{class}_{room}"
private val DATE_FORMATS = listOf(
    "yyyy-MM-dd",
    "yyyyMMdd",
    "MM-dd",
    "yyyy.MM.dd",
    "dd-MM-yyyy"
)
private val PLACEHOLDER_TOKENS = listOf(
    "{class}",
    "{date}",
    "{lesson number}",
    "{room}",
    "{teacher}",
    "{time}",
    "{slot}"
)

/**
 * Full-screen DSH Go Settings hub built on [AppPage] + [OverviewCard] + [ListPageCard]:
 * 1. On-device MCP server OverviewCard + expandable CodeWell copy rows
 * 2. Appearance picker: Light / Dark / System miniature Mist Glass previews,
 *    6 OKLCH guarded accent swatches, and Android 12+ Follow wallpaper switch
 * 3. Class schedule source URL sync
 * 4. File naming template editor with live .m4a CodeWell preview
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    ui: MainUiState,
    onDismiss: () -> Unit,
    onSelectThemeMode: (DshThemeMode) -> Unit,
    onSelectAccentId: (String) -> Unit,
    onSaveFilenameTemplate: (String, String) -> Unit,
    onSyncSchedule: (String) -> Unit,
    onToggleMcp: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme
    val surfaces = ClassRecorderTheme.surfaces
    val reduced = ClassRecorderTheme.reducedMotion

    var backProgress by remember { mutableFloatStateOf(0f) }
    PredictiveBackHandler(enabled = true) { progressFlow ->
        try {
            progressFlow.collect { event ->
                backProgress = event.progress.coerceIn(0f, 1f)
            }
            onDismiss()
        } catch (_: CancellationException) {
            backProgress = 0f
        }
    }

    var showMcpEndpoints by rememberSaveable { mutableStateOf(false) }
    var scheduleUrl by rememberSaveable(ui.scheduleSourceInput) { mutableStateOf(ui.scheduleSourceInput) }
    var templateInput by rememberSaveable(ui.filenameTemplate) { mutableStateOf(ui.filenameTemplate) }
    var dateFormatInput by rememberSaveable(ui.dateFormat) { mutableStateOf(ui.dateFormat) }
    var lastPresetId by rememberSaveable {
        mutableStateOf(
            if (ui.accentId != AppAccent.WALLPAPER_ID) ui.accentId
            else AppearancePreferences.loadLastPresetId(context)
        )
    }

    val activeAccent = resolveAppAccent(ui.accentId)
    val lanUrl = "http://${ui.lanIp}:${ui.mcpStatus.port}/mcp"
    val adbCmd = "adb forward tcp:${ui.mcpStatus.port} tcp:${ui.mcpStatus.port}"
    val sampleCourse = ui.schedule?.slots
        ?.flatMap { it.days }
        ?.firstOrNull { it != null && it.name.isNotBlank() }

    val livePreviewName = remember(templateInput, dateFormatInput, sampleCourse) {
        UiFormatters.previewFilename(
            template = templateInput,
            dateFormat = dateFormatInput,
            courseName = sampleCourse?.name ?: "大學之道",
            room = sampleCourse?.room?.ifBlank { "3309" } ?: "3309",
            teacher = sampleCourse?.teacher?.ifBlank { "何昕家" } ?: "何昕家"
        )
    }

    val slidePx = with(density) { 40.dp.toPx() } * backProgress

    AppPage(
        title = "Settings",
        onBack = onDismiss,
        modifier = Modifier.graphicsLayer {
            val s = 1f - 0.05f * backProgress
            scaleX = s
            scaleY = s
            translationX = slidePx
            alpha = 1f - 0.18f * backProgress
        }
    ) {
        item(key = "settings_body") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpace.gutter),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. MCP Server Hub Card
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OverviewCard(
                        leading = {
                            OverviewGlyph(
                                icon = Icons.Rounded.Hub,
                                tintSlot = 0
                            )
                        },
                        title = "On-device MCP server",
                        subtitle = if (ui.mcpStatus.running) {
                            "Listening on port ${ui.mcpStatus.port}"
                        } else {
                            "Stopped · Toggle to expose agent tools"
                        },
                        trailing = {
                            Switch(
                                checked = ui.mcpStatus.running,
                                onCheckedChange = { onToggleMcp() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = scheme.onPrimary,
                                    checkedTrackColor = scheme.primary,
                                    uncheckedThumbColor = scheme.onSurfaceVariant,
                                    uncheckedTrackColor = scheme.surfaceContainerHighest,
                                    uncheckedBorderColor = scheme.outlineVariant
                                )
                            )
                        },
                        footer = {
                            OverviewFooterRow(
                                tone = if (ui.mcpStatus.running) StatusTone.Ok else StatusTone.Idle,
                                text = if (ui.mcpStatus.running) {
                                    "$lanUrl · Tap for endpoints"
                                } else {
                                    "Tap to view LAN & ADB connection commands"
                                }
                            )
                        },
                        onFooterClick = { showMcpEndpoints = !showMcpEndpoints }
                    )

                    AnimatedVisibility(
                        visible = showMcpEndpoints,
                        enter = expandVertically(DshMotion.collapseTween(reduced)) + fadeIn(DshMotion.quickTween(reduced)),
                        exit = shrinkVertically(DshMotion.collapseTween(reduced)) + fadeOut(DshMotion.quickTween(reduced))
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CodeWell(
                                label = "LAN endpoint",
                                text = lanUrl,
                                onCopy = {
                                    UiFormatters.copyToClipboard(context, "MCP LAN URL", lanUrl)
                                    onShowMessage("Copied LAN endpoint")
                                }
                            )
                            CodeWell(
                                label = "ADB tunnel",
                                text = adbCmd,
                                onCopy = {
                                    UiFormatters.copyToClipboard(context, "ADB forward command", adbCmd)
                                    onShowMessage("Copied ADB command")
                                }
                            )
                        }
                    }
                }

                // 2. Appearance Section (Theme Mode + 6 OKLCH Presets + Follow Wallpaper)
                ListPageCard(
                    title = "Appearance",
                    footer = "Colors are contrast-guarded in OKLCH perceptual space across Morning Mist and Slate Night."
                ) {
                    ThemeModePickerRow(
                        selectedMode = ui.themeMode,
                        accent = activeAccent,
                        onSelectMode = onSelectThemeMode
                    )

                    ListCardDivider()

                    PalettePickerRow(
                        selectedAccentId = ui.accentId,
                        darkTheme = surfaces.darkTheme,
                        onSelectPreset = { id ->
                            lastPresetId = id
                            onSelectAccentId(id)
                        }
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        ListCardDivider()
                        val wallpaperAccent = resolveAppAccent(AppAccent.WALLPAPER_ID)
                        val wallpaperDot = wallpaperAccent.seedFor(surfaces.darkTheme)
                        val followingWallpaper = ui.accentId == AppAccent.WALLPAPER_ID
                        SettingsSwitchTile(
                            title = "Follow wallpaper",
                            subtitle = "Derive guarded Mist Glass palette from Android system color",
                            checked = followingWallpaper,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (ui.accentId != AppAccent.WALLPAPER_ID) {
                                        lastPresetId = ui.accentId
                                    }
                                    onSelectAccentId(AppAccent.WALLPAPER_ID)
                                } else {
                                    onSelectAccentId(lastPresetId)
                                }
                            },
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(wallpaperDot.copy(alpha = 0.22f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(wallpaperDot)
                                            .border(0.5.dp, scheme.onSurface.copy(alpha = 0.12f), CircleShape)
                                    )
                                }
                            }
                        )
                    }
                }

                // 3. Class Schedule Section
                ListPageCard(
                    title = "Class schedule",
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    ListPageRow(
                        title = ui.schedule?.schoolName?.ifBlank { "No timetable synced" } ?: "No timetable synced",
                        subtitle = ui.schedule?.let { "${it.slots.size} periods configured" }
                            ?: "Import uni-campus share link or NUTC timetable",
                        leading = {
                            SettingsIcon(
                                icon = Icons.Rounded.CalendarMonth,
                                tintSlot = 5
                            )
                        },
                        showChevron = false
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DshTextField(
                            value = scheduleUrl,
                            onValueChange = { scheduleUrl = it },
                            label = "Timetable URL or share link",
                            placeholder = "https://uni-campus-app.pages.dev/s/...",
                            mono = true,
                            imeAction = ImeAction.Go,
                            onImeAction = {
                                if (scheduleUrl.isNotBlank()) onSyncSchedule(scheduleUrl.trim())
                            }
                        )
                        DshPrimaryButton(
                            label = if (ui.isSyncing) "Syncing…" else "Sync schedule",
                            enabled = scheduleUrl.isNotBlank() && !ui.isSyncing,
                            onClick = { onSyncSchedule(scheduleUrl.trim()) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 4. File Naming Section
                ListPageCard(
                    title = "File naming",
                    footer = "Applied automatically when new .m4a lecture recordings are created.",
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        DshTextField(
                            value = templateInput,
                            onValueChange = { templateInput = it },
                            label = "Filename template",
                            placeholder = DEFAULT_TEMPLATE,
                            mono = true
                        )

                        CodeWell(
                            label = "Live preview",
                            text = livePreviewName
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Date format",
                                style = MaterialTheme.typography.labelMedium,
                                color = ClassRecorderTheme.dshColors.textTertiary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                DATE_FORMATS.forEach { fmt ->
                                    CapsuleChoiceChip(
                                        label = fmt,
                                        selected = dateFormatInput == fmt,
                                        onClick = { dateFormatInput = fmt }
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Insert token",
                                style = MaterialTheme.typography.labelMedium,
                                color = ClassRecorderTheme.dshColors.textTertiary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                PLACEHOLDER_TOKENS.forEach { token ->
                                    CapsuleChoiceChip(
                                        label = token,
                                        selected = false,
                                        onClick = {
                                            templateInput = if (templateInput.isBlank()) {
                                                token
                                            } else {
                                                "${templateInput}_$token"
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DshSecondaryButton(
                                label = "Reset",
                                icon = Icons.Rounded.RestartAlt,
                                onClick = {
                                    templateInput = DEFAULT_TEMPLATE
                                    dateFormatInput = "yyyy-MM-dd"
                                    onSaveFilenameTemplate(DEFAULT_TEMPLATE, "yyyy-MM-dd")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            DshPrimaryButton(
                                label = "Save format",
                                icon = Icons.Rounded.Save,
                                onClick = {
                                    onSaveFilenameTemplate(
                                        templateInput.trim().ifBlank { DEFAULT_TEMPLATE },
                                        dateFormatInput.trim().ifBlank { "yyyy-MM-dd" }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Three side-by-side 0.8-aspect miniature Mist Glass phone previews (Light, Dark, System)
 * ported faithfully from DSH Go's ThemeModePicker in appearance_page.dart.
 */
@Composable
private fun ThemeModePickerRow(
    selectedMode: DshThemeMode,
    accent: AppAccent,
    onSelectMode: (DshThemeMode) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val lightSurfaces = remember(accent) { DshSurfaces.light(accent) }
    val darkSurfaces = remember(accent) { DshSurfaces.dark(accent) }
    val lightScheme = remember(accent) { DshPalette.lightScheme(accent) }
    val darkScheme = remember(accent) { DshPalette.darkScheme(accent) }

    val modes = listOf(
        DshThemeMode.LIGHT to "Light",
        DshThemeMode.DARK to "Dark",
        DshThemeMode.SYSTEM to "System"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        modes.forEach { (mode, label) ->
            val isSelected = selectedMode == mode
            ModePreviewTile(
                label = label,
                selected = isSelected,
                lightPair = if (mode == DshThemeMode.DARK) null else (lightSurfaces to lightScheme),
                darkPair = if (mode == DshThemeMode.LIGHT) null else (darkSurfaces to darkScheme),
                onClick = {
                    if (!isSelected) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectMode(mode)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ModePreviewTile(
    label: String,
    selected: Boolean,
    lightPair: Pair<DshSurfaces, ColorScheme>?,
    darkPair: Pair<DshSurfaces, ColorScheme>?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val reduced = ClassRecorderTheme.reducedMotion
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (pressed && !reduced) DshMotion.PRESS_SCALE else 1f,
        animationSpec = DshMotion.pressSpring(reduced),
        label = "modeTileScale"
    )
    val ringAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = DshMotion.quickTween(reduced),
        label = "modeRingAlpha"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
        animationSpec = DshMotion.quickTween(reduced),
        label = "modeLabelColor"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AppShape.radiusLg)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics { this.selected = selected }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp)
                    .clip(AppShape.radiusMd)
                    .border(0.5.dp, scheme.outlineVariant, AppShape.radiusMd)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (lightPair != null && darkPair != null) {
                        drawMiniMistPhone(lightPair.first, lightPair.second, isDark = false)
                        clipRect(left = size.width * 0.5f, top = 0f, right = size.width, bottom = size.height) {
                            drawMiniMistPhone(darkPair.first, darkPair.second, isDark = true)
                        }
                    } else if (lightPair != null) {
                        drawMiniMistPhone(lightPair.first, lightPair.second, isDark = false)
                    } else if (darkPair != null) {
                        drawMiniMistPhone(darkPair.first, darkPair.second, isDark = true)
                    }
                }
            }

            if (ringAlpha > 0.01f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokePx = 2.dp.toPx()
                    val inset = strokePx * 0.5f
                    val r = (AppShape.md + 5.dp).toPx() - inset
                    drawRoundRect(
                        color = scheme.primary.copy(alpha = ringAlpha),
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - strokePx, size.height - strokePx),
                        cornerRadius = CornerRadius(r, r),
                        style = Stroke(width = strokePx)
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private fun DrawScope.drawMiniMistPhone(
    s: DshSurfaces,
    scheme: ColorScheme,
    isDark: Boolean
) {
    val w = size.width
    val h = size.height
    drawRect(brush = Brush.verticalGradient(listOf(s.backdropTop, s.backdropBottom)))

    val r1 = h * 0.7f
    val c1 = Offset(w * 0.92f, 0f)
    drawCircle(
        brush = Brush.radialGradient(listOf(s.glowPrimary, s.glowPrimary.copy(alpha = 0f)), center = c1, radius = r1),
        radius = r1,
        center = c1
    )
    val r2 = h * 0.62f
    val c2 = Offset(0f, h)
    drawCircle(
        brush = Brush.radialGradient(listOf(s.glowSecondary, s.glowSecondary.copy(alpha = 0f)), center = c2, radius = r2),
        radius = r2,
        center = c2
    )

    val unit = w / 100f
    val chromeFill = s.chrome.fallbackFill
    val chromeEdge = s.chrome.hairline
    val ink = scheme.onSurface.copy(alpha = if (isDark) 0.26f else 0.20f)

    // Floating top SegmentedPill silhouette
    val headerW = 54f * unit
    val headerH = 9.5f * unit
    val headerTopLeft = Offset((w - headerW) * 0.5f, 7.5f * unit)
    drawRoundRect(
        color = chromeFill,
        topLeft = headerTopLeft,
        size = Size(headerW, headerH),
        cornerRadius = CornerRadius(headerH * 0.5f, headerH * 0.5f)
    )
    drawRoundRect(
        color = chromeEdge,
        topLeft = headerTopLeft,
        size = Size(headerW, headerH),
        cornerRadius = CornerRadius(headerH * 0.5f, headerH * 0.5f),
        style = Stroke(width = 0.6.dp.toPx())
    )

    // Overview card & waveform lines
    val bubbleRect = Rect(w - 12f * unit - 48f * unit, 28f * unit, w - 12f * unit, 43f * unit)
    drawRoundRect(
        color = s.userBubble,
        topLeft = bubbleRect.topLeft,
        size = bubbleRect.size,
        cornerRadius = CornerRadius(6f * unit, 6f * unit)
    )
    fun drawLinePill(x: Float, y: Float, width: Float, height: Float) {
        drawRoundRect(
            color = ink,
            topLeft = Offset(x, y),
            size = Size(width, height),
            cornerRadius = CornerRadius(height * 0.5f, height * 0.5f)
        )
    }
    drawLinePill(w - 12f * unit - 40f * unit, 33.5f * unit, 32f * unit, 4f * unit)
    drawLinePill(12f * unit, 52f * unit, 66f * unit, 4.5f * unit)
    drawLinePill(12f * unit, 61f * unit, 76f * unit, 4.5f * unit)
    drawLinePill(12f * unit, 70f * unit, 48f * unit, 4.5f * unit)

    // Floating bottom Composer dock with primary record/send circle
    val composerH = 15f * unit
    val composerTop = h - 8f * unit - composerH
    val composerSize = Size(w - 16f * unit, composerH)
    drawRoundRect(
        color = chromeFill,
        topLeft = Offset(8f * unit, composerTop),
        size = composerSize,
        cornerRadius = CornerRadius(composerH * 0.5f, composerH * 0.5f)
    )
    drawRoundRect(
        color = chromeEdge,
        topLeft = Offset(8f * unit, composerTop),
        size = composerSize,
        cornerRadius = CornerRadius(composerH * 0.5f, composerH * 0.5f),
        style = Stroke(width = 0.6.dp.toPx())
    )
    drawCircle(
        color = scheme.primary,
        radius = composerH * 0.5f - 2.5f * unit,
        center = Offset(w - 8f * unit - composerH * 0.5f, composerTop + composerH * 0.5f)
    )
}

/**
 * Six guarded OKLCH color swatches (`Mist`, `Daiqing`, `Dusk`, `Turquoise`, `Amber`, `Graphite`),
 * ported from DSH Go's PalettePicker in appearance_page.dart.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PalettePickerRow(
    selectedAccentId: String,
    darkTheme: Boolean,
    onSelectPreset: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppAccent.presets.forEach { preset ->
            val selected = selectedAccentId == preset.id
            val swatchColor = remember(preset, darkTheme) { preset.seedFor(darkTheme) }
            AccentSwatchItem(
                label = preset.label,
                color = swatchColor,
                selected = selected,
                onClick = {
                    if (!selected) {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectPreset(preset.id)
                    }
                }
            )
        }
    }
}

@Composable
private fun AccentSwatchItem(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val reduced = ClassRecorderTheme.reducedMotion
    val ringAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = DshMotion.quickTween(reduced),
        label = "swatchRing_$label"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) scheme.primary else scheme.onSurfaceVariant,
        animationSpec = DshMotion.quickTween(reduced),
        label = "swatchLabel_$label"
    )

    Column(
        modifier = Modifier
            .sizeIn(minWidth = 50.dp, minHeight = 56.dp)
            .clip(AppShape.radiusMd)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(modifier = Modifier.size(40.dp)) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            val discRadius = 15.dp.toPx()
            if (ringAlpha > 0.01f) {
                drawCircle(
                    color = color.copy(alpha = ringAlpha),
                    radius = discRadius + 3.5.dp.toPx(),
                    center = center,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            drawCircle(
                color = color,
                radius = discRadius,
                center = center
            )
            drawCircle(
                color = scheme.onSurface.copy(alpha = 0.10f),
                radius = discRadius - 0.25.dp.toPx(),
                center = center,
                style = Stroke(width = 0.5.dp.toPx())
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = labelColor,
            maxLines = 1
        )
    }
}
