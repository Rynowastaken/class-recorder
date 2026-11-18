package com.ryno.classrecorder.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.core.CourseSlot
import com.ryno.classrecorder.service.RecorderState
import com.ryno.classrecorder.ui.MainUiState
import com.ryno.classrecorder.ui.components.AppSheet
import com.ryno.classrecorder.ui.components.BackdropVeil
import com.ryno.classrecorder.ui.components.CapsuleChoiceChip
import com.ryno.classrecorder.ui.components.DshConfirmSheet
import com.ryno.classrecorder.ui.components.DshPrimaryButton
import com.ryno.classrecorder.ui.components.DshSecondaryButton
import com.ryno.classrecorder.ui.components.GlassPill
import com.ryno.classrecorder.ui.components.GlassSurface
import com.ryno.classrecorder.ui.components.ListCardDivider
import com.ryno.classrecorder.ui.components.ListPageRow
import com.ryno.classrecorder.ui.components.LocalBackdropLayer
import com.ryno.classrecorder.ui.components.OverviewCard
import com.ryno.classrecorder.ui.components.OverviewFooterRow
import com.ryno.classrecorder.ui.components.OverviewGlyph
import com.ryno.classrecorder.ui.components.SettingsIcon
import com.ryno.classrecorder.ui.components.SheetGroup
import com.ryno.classrecorder.ui.components.StatusChip
import com.ryno.classrecorder.ui.components.StatusDot
import com.ryno.classrecorder.ui.components.rememberSafeSideInsets
import com.ryno.classrecorder.ui.theme.AppShape
import com.ryno.classrecorder.ui.theme.AppSpace
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.DshMotion
import com.ryno.classrecorder.ui.theme.GlassTier
import com.ryno.classrecorder.ui.theme.StatusTone

/**
 * Primary lecture capture workspace rebuilt in DSH Go's Mist Glass language:
 * - Course OverviewCard with inline StatusChip, atomic metadata FlowRow, and MCP footer
 * - Optional collapsible GlassPill for recent agent tool calls
 * - Quick course CapsuleChoiceChips with >= 3.5:1 contrast-guarded course dots
 * - Mist Glass waveform stage + 48sp Light tabular clock
 * - Floating Chrome-tier bottom Composer dock: tappable course summary on the left +
 *   circular brick-rose record trigger on the right (transitioning to Discard / Pause / Finish when live)
 */
@Composable
fun RecordScreen(
    ui: MainUiState,
    recorder: RecorderState,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onDiscardRecording: () -> Unit,
    onTogglePause: () -> Unit,
    onSelectCourseOverride: (CourseSlot?) -> Unit,
    onToggleMcp: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    topContentInset: Dp = 0.dp,
    onScrolledUnderChange: (Float) -> Unit = {}
) {
    val density = LocalDensity.current
    val dsh = ClassRecorderTheme.dshColors
    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val contentBackdropLayer = rememberGraphicsLayer()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var showCoursePicker by remember { mutableStateOf(false) }
    var showLocalMcpSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var expandAgentCalls by remember { mutableStateOf(false) }

    val amplitudes = remember { mutableStateListOf<Float>() }
    LaunchedEffect(recorder.isRecording) {
        if (!recorder.isRecording) {
            amplitudes.clear()
        }
    }
    LaunchedEffect(recorder.elapsedMs, recorder.amplitude, recorder.isRecording, recorder.isPaused) {
        if (recorder.isRecording && !recorder.isPaused) {
            amplitudes.add(recorder.amplitude.coerceIn(0.04f, 1f))
            while (amplitudes.size > 96) {
                amplitudes.removeAt(0)
            }
        }
    }

    val scrollFraction by remember {
        derivedStateOf {
            val px = if (listState.firstVisibleItemIndex > 0) {
                1000f
            } else {
                listState.firstVisibleItemScrollOffset.toFloat()
            }
            (px / with(density) { 24.dp.toPx() }).coerceIn(0f, 1f)
        }
    }
    LaunchedEffect(scrollFraction) {
        onScrolledUnderChange(scrollFraction)
    }

    val distinctCourses = remember(ui.schedule) {
        ui.schedule?.slots
            .orEmpty()
            .flatMap { it.days }
            .filterNotNull()
            .filter { it.name.isNotBlank() }
            .distinctBy { it.name.trim() }
    }

    val activeCourseName = when {
        recorder.isRecording -> recorder.activeRecording?.courseName?.ifBlank { "Uncategorized" } ?: "Recording"
        ui.selectedCourseOverride != null -> ui.selectedCourseOverride.name
        ui.currentClass != null -> ui.currentClass.course.name
        else -> "Uncategorized"
    }

    val (statusTone, statusLabel) = when {
        recorder.isRecording && recorder.isPaused -> StatusTone.Warn to "Paused"
        recorder.isRecording -> StatusTone.Error to "Live"
        ui.selectedCourseOverride != null -> StatusTone.Busy to "Manual"
        ui.currentClass != null -> {
            if (ui.currentClass.kind == "upcoming") StatusTone.Busy to "Up next"
            else StatusTone.Ok to "In class"
        }
        else -> StatusTone.Idle to "Ready"
    }

    val courseAtoms: List<String> = when {
        recorder.isRecording -> {
            val r = recorder.activeRecording
            listOfNotNull(
                r?.slot?.takeIf { it.isNotBlank() }?.let { "P$it" },
                r?.room?.takeIf { it.isNotBlank() },
                r?.teacher?.takeIf { it.isNotBlank() }
            )
        }
        ui.selectedCourseOverride != null -> {
            val c = ui.selectedCourseOverride
            listOfNotNull(
                c.room.takeIf { it.isNotBlank() },
                c.teacher.takeIf { it.isNotBlank() }
            )
        }
        ui.currentClass != null -> {
            val cur = ui.currentClass
            listOfNotNull(
                cur.slot.takeIf { it.isNotBlank() }?.let { "P$it" },
                UiFormatters.formatTimeRange(cur.time).takeIf { it.isNotBlank() },
                cur.course.room.takeIf { it.isNotBlank() },
                cur.course.teacher.takeIf { it.isNotBlank() }
            )
        }
        else -> emptyList()
    }

    val fallbackSubtitle = when {
        courseAtoms.isNotEmpty() -> null
        recorder.isRecording -> "Capturing background audio"
        ui.selectedCourseOverride != null -> "Manual class override"
        distinctCourses.isNotEmpty() -> "No class in session · Tap to pick a course"
        else -> "Sync your schedule or record an untagged session"
    }

    val activeRoom = when {
        ui.selectedCourseOverride != null -> ui.selectedCourseOverride.room
        ui.currentClass != null -> ui.currentClass.course.room
        else -> ""
    }
    val activeTeacher = when {
        ui.selectedCourseOverride != null -> ui.selectedCourseOverride.teacher
        ui.currentClass != null -> ui.currentClass.course.teacher
        else -> ""
    }
    val activeSlot = ui.currentClass?.slot.orEmpty()
    val targetPathPreview = remember(
        ui.filenameTemplate,
        ui.dateFormat,
        activeCourseName,
        activeRoom,
        activeTeacher,
        activeSlot
    ) {
        val filePreview = UiFormatters.previewFilename(
            template = ui.filenameTemplate,
            dateFormat = ui.dateFormat,
            courseName = activeCourseName,
            room = activeRoom.ifBlank { "Room" },
            teacher = activeTeacher.ifBlank { "Instructor" },
            slot = activeSlot.ifBlank { "1" }
        )
        val safeFolder = UiFormatters.sanitizePathSegment(activeCourseName)
        "$safeFolder/$filePreview"
    }

    val dockReservedHeight = navBottom + 96.dp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val (startInset, endInset) = rememberSafeSideInsets(maxWidth)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .drawWithContent {
                    contentBackdropLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(contentBackdropLayer)
                },
            contentPadding = PaddingValues(
                start = AppSpace.gutter + startInset,
                end = AppSpace.gutter + endInset,
                top = topContentInset + 8.dp,
                bottom = dockReservedHeight + 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasMicPermission && !recorder.isRecording) {
                item(key = "mic_permission_banner") {
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        tier = GlassTier.Card,
                        shape = AppShape.radiusMd,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SettingsIcon(
                                icon = Icons.Rounded.MicOff,
                                danger = true
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Microphone access needed",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = scheme.onSurface
                                )
                                Text(
                                    text = "Grant audio permission to capture classroom lectures.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = dsh.textTertiary
                                )
                            }
                            DshSecondaryButton(
                                label = "Allow microphone",
                                onClick = onRequestMicPermission
                            )
                        }
                    }
                }
            }

            item(key = "course_overview") {
                OverviewCard(
                    leading = {
                        OverviewGlyph(
                            icon = Icons.Rounded.School,
                            identityKey = activeCourseName
                        )
                    },
                    title = activeCourseName,
                    subtitle = fallbackSubtitle,
                    subtitleAtoms = courseAtoms,
                    titleBadge = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (ui.selectedCourseOverride != null && !recorder.isRecording) {
                                StatusChip(
                                    tone = StatusTone.Idle,
                                    label = "Auto",
                                    onClick = { onSelectCourseOverride(null) }
                                )
                            }
                            StatusChip(
                                tone = statusTone,
                                label = statusLabel
                            )
                        }
                    },
                    onClick = if (!recorder.isRecording) {
                        { showCoursePicker = true }
                    } else null,
                    footer = {
                        OverviewFooterRow(
                            tone = if (ui.mcpStatus.running) StatusTone.Ok else StatusTone.Idle,
                            text = if (ui.mcpStatus.running) {
                                "MCP · http://${ui.lanIp}:${ui.mcpStatus.port}/mcp"
                            } else {
                                "MCP · Server stopped"
                            }
                        )
                    },
                    onFooterClick = {
                        showLocalMcpSheet = true
                    }
                )
            }

            if (ui.mcpStatus.recentCalls.isNotEmpty()) {
                item(key = "mcp_recent_pill") {
                    val latest = ui.mcpStatus.recentCalls.last()
                    GlassPill(
                        onClick = { expandAgentCalls = !expandAgentCalls },
                        summary = {
                            StatusDot(tone = StatusTone.Busy, size = 7.dp)
                            Text(
                                text = "Agent · $latest",
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (expandAgentCalls) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = if (expandAgentCalls) "Collapse agent activity" else "Expand agent activity",
                                modifier = Modifier.size(18.dp),
                                tint = dsh.textTertiary
                            )
                        },
                        expandedContent = if (expandAgentCalls) {
                            {
                                ui.mcpStatus.recentCalls.takeLast(5).asReversed().forEach { call ->
                                    Text(
                                        text = call,
                                        style = ClassRecorderTheme.mono.monoSmall,
                                        color = scheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else null
                    )
                }
            }

            if (!recorder.isRecording && distinctCourses.isNotEmpty()) {
                item(key = "quick_courses") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item(key = "__auto__") {
                            CapsuleChoiceChip(
                                label = if (ui.currentClass != null) {
                                    "Auto (${ui.currentClass.course.name})"
                                } else {
                                    "Auto schedule"
                                },
                                selected = ui.selectedCourseOverride == null,
                                onClick = { onSelectCourseOverride(null) }
                            )
                        }
                        items(
                            items = distinctCourses,
                            key = { it.name }
                        ) { course ->
                            CapsuleChoiceChip(
                                label = course.name,
                                selected = ui.selectedCourseOverride?.name == course.name,
                                courseKey = course.name,
                                onClick = {
                                    if (ui.selectedCourseOverride?.name == course.name) {
                                        onSelectCourseOverride(null)
                                    } else {
                                        onSelectCourseOverride(course)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            item(key = "capture_stage") {
                CaptureStageCard(
                    recorder = recorder,
                    amplitudes = amplitudes,
                    hasMicPermission = hasMicPermission
                )
            }
        }

        BackdropVeil(
            fromTop = false,
            solidHeight = navBottom + 76.dp,
            feather = 18.dp,
            alpha = 0.92f,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        CompositionLocalProvider(LocalBackdropLayer provides contentBackdropLayer) {
            CaptureComposerDock(
                activeCourseName = activeCourseName,
                targetPathPreview = targetPathPreview,
                isRecording = recorder.isRecording,
                isPaused = recorder.isPaused,
                hasMicPermission = hasMicPermission,
                onOpenCoursePicker = { showCoursePicker = true },
                onRequestMicPermission = onRequestMicPermission,
                onStart = onStartRecording,
                onTogglePause = onTogglePause,
                onFinish = onStopRecording,
                onDiscard = { showDiscardDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = AppSpace.floatInset + startInset,
                        end = AppSpace.floatInset + endInset,
                        bottom = navBottom + AppSpace.floatInset
                    )
            )
        }
    }

    if (showCoursePicker) {
        CoursePickerSheet(
            courses = distinctCourses,
            selectedOverride = ui.selectedCourseOverride,
            currentClassName = ui.currentClass?.course?.name,
            onSelect = { picked ->
                onSelectCourseOverride(picked)
                showCoursePicker = false
            },
            onDismiss = { showCoursePicker = false }
        )
    }

    if (showLocalMcpSheet) {
        McpServerSheet(
            ui = ui,
            onDismiss = { showLocalMcpSheet = false },
            onToggleMcp = onToggleMcp,
            onCopied = onShowMessage
        )
    }

    if (showDiscardDialog) {
        DshConfirmSheet(
            title = "Discard recording?",
            message = "The current audio capture will be deleted and cannot be recovered.",
            confirmLabel = "Discard",
            cancelLabel = "Keep recording",
            danger = true,
            onConfirm = {
                showDiscardDialog = false
                onDiscardRecording()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }
}

@Composable
private fun CaptureStageCard(
    recorder: RecorderState,
    amplitudes: List<Float>,
    hasMicPermission: Boolean
) {
    val dsh = ClassRecorderTheme.dshColors
    val mono = ClassRecorderTheme.mono
    val scheme = MaterialTheme.colorScheme
    val reduced = ClassRecorderTheme.reducedMotion

    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        tier = GlassTier.Card,
        shape = AppShape.radiusLg,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = UiFormatters.formatDuration(if (recorder.isRecording) recorder.elapsedMs else 0L),
                style = mono.timer,
                color = when {
                    !recorder.isRecording -> dsh.textTertiary
                    recorder.isPaused -> dsh.warning
                    else -> scheme.onSurface
                }
            )
            Spacer(Modifier.height(6.dp))
            AnimatedContent(
                targetState = when {
                    recorder.isRecording && recorder.isPaused -> "Recording paused"
                    recorder.isRecording -> "Capturing AAC 128 kbps · .m4a"
                    !hasMicPermission -> "Microphone permission required before recording"
                    else -> "Continues cleanly in the background with screen off"
                },
                transitionSpec = {
                    fadeIn(DshMotion.quickTween(reduced)) togetherWith fadeOut(DshMotion.quickTween(reduced))
                },
                label = "stageCaption"
            ) { caption ->
                Text(
                    text = caption,
                    style = mono.timerCaption,
                    color = dsh.textTertiary,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(20.dp))

            LiveWaveformCanvas(
                amplitudes = amplitudes,
                active = recorder.isRecording,
                paused = recorder.isPaused,
                activeColor = if (recorder.isPaused) dsh.textTertiary else dsh.danger,
                idleColor = dsh.textTertiary,
                playheadColor = dsh.danger,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(128.dp)
            )
        }
    }
}

/**
 * DSH Go Composer-style bottom dock:
 * - Idle: tappable target summary on the left + circular 52dp brick-rose record trigger on the right.
 * - Live/Paused: circular Discard button + Pause/Resume capsule + brick-rose Finish capsule.
 */
@Composable
private fun CaptureComposerDock(
    activeCourseName: String,
    targetPathPreview: String,
    isRecording: Boolean,
    isPaused: Boolean,
    hasMicPermission: Boolean,
    onOpenCoursePicker: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onStart: () -> Unit,
    onTogglePause: () -> Unit,
    onFinish: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dsh = ClassRecorderTheme.dshColors
    val surfaces = ClassRecorderTheme.surfaces
    val scheme = MaterialTheme.colorScheme
    val reduced = ClassRecorderTheme.reducedMotion

    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        tier = GlassTier.Chrome,
        shape = AppShape.radiusXl,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        AnimatedContent(
            targetState = isRecording,
            transitionSpec = {
                fadeIn(DshMotion.standardTween(reduced)) togetherWith fadeOut(DshMotion.quickTween(reduced))
            },
            label = "composerDockState"
        ) { active ->
            if (!active) {
                val dotColor = dsh.courseDot(activeCourseName, surfaces.darkTheme)
                val recInteraction = remember { MutableInteractionSource() }
                val recPressed by recInteraction.collectIsPressedAsState()
                val recScale by animateFloatAsState(
                    targetValue = if (recPressed && !reduced) DshMotion.PRESS_SCALE else 1f,
                    animationSpec = DshMotion.pressSpring(reduced),
                    label = "recordTriggerScale"
                )
                val triggerLabel = if (hasMicPermission) "Start recording" else "Allow microphone"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(AppShape.radiusLg)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Choose course tag",
                                onClick = onOpenCoursePicker
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Record · $activeCourseName",
                                style = MaterialTheme.typography.titleSmall,
                                color = scheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (hasMicPermission) {
                                    targetPathPreview
                                } else {
                                    "Tap mic to allow audio permission"
                                },
                                style = ClassRecorderTheme.mono.monoSmall,
                                color = dsh.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .graphicsLayer {
                                scaleX = recScale
                                scaleY = recScale
                            }
                            .clip(CircleShape)
                            .background(dsh.danger)
                            .clickable(
                                interactionSource = recInteraction,
                                indication = ripple(),
                                role = Role.Button,
                                onClickLabel = triggerLabel,
                                onClick = if (hasMicPermission) onStart else onRequestMicPermission
                            )
                            .semantics {
                                contentDescription = triggerLabel
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Mic,
                            contentDescription = null,
                            tint = dsh.onDanger,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassSurface(
                        modifier = Modifier.size(52.dp),
                        tier = GlassTier.Pill,
                        circle = true,
                        onClick = onDiscard
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Discard recording",
                                tint = dsh.danger,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    if (isPaused) {
                        DshPrimaryButton(
                            label = "Resume",
                            icon = Icons.Rounded.PlayArrow,
                            onClick = onTogglePause,
                            minHeight = 52.dp,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        DshSecondaryButton(
                            label = "Pause",
                            icon = Icons.Rounded.Pause,
                            onClick = onTogglePause,
                            minHeight = 52.dp,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    DshPrimaryButton(
                        label = "Finish",
                        icon = Icons.Rounded.Stop,
                        danger = true,
                        onClick = onFinish,
                        minHeight = 52.dp,
                        modifier = Modifier.defaultMinSize(minWidth = 116.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CoursePickerSheet(
    courses: List<CourseSlot>,
    selectedOverride: CourseSlot?,
    currentClassName: String?,
    onSelect: (CourseSlot?) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    AppSheet(
        onDismiss = onDismiss,
        title = "Tag recording course"
    ) {
        SheetGroup {
            ListPageRow(
                title = "Automatic from schedule",
                subtitle = currentClassName?.let { "Currently matches $it" }
                    ?: "Uses current timetable slot or Uncategorized",
                leading = {
                    SettingsIcon(icon = Icons.Rounded.AutoAwesome, tintSlot = 0)
                },
                trailing = if (selectedOverride == null) {
                    {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = "Selected",
                            tint = scheme.primary
                        )
                    }
                } else null,
                showChevron = false,
                onClick = { onSelect(null) }
            )
        }

        if (courses.isNotEmpty()) {
            SheetGroup(title = "Courses") {
                courses.forEachIndexed { idx, course ->
                    if (idx > 0) ListCardDivider(startIndent = 60.dp)
                    val selected = selectedOverride?.name == course.name
                    val sub = listOfNotNull(
                        course.room.takeIf { it.isNotBlank() },
                        course.teacher.takeIf { it.isNotBlank() }
                    ).joinToString(" · ").ifBlank { null }
                    ListPageRow(
                        title = course.name,
                        subtitle = sub,
                        leading = {
                            SettingsIcon(
                                icon = Icons.Rounded.School,
                                identityKey = course.name
                            )
                        },
                        trailing = if (selected) {
                            {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = scheme.primary
                                )
                            }
                        } else null,
                        showChevron = false,
                        onClick = { onSelect(course) }
                    )
                }
            }
        }
    }
}
