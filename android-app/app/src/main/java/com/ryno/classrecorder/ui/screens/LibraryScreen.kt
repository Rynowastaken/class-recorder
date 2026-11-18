package com.ryno.classrecorder.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ryno.classrecorder.core.RecordingRecord
import com.ryno.classrecorder.ui.MainUiState
import com.ryno.classrecorder.ui.components.AppSheet
import com.ryno.classrecorder.ui.components.BackdropVeil
import com.ryno.classrecorder.ui.components.CapsuleChoiceChip
import com.ryno.classrecorder.ui.components.DshPrimaryButton
import com.ryno.classrecorder.ui.components.DshSecondaryButton
import com.ryno.classrecorder.ui.components.EmptyView
import com.ryno.classrecorder.ui.components.GlassIconButton
import com.ryno.classrecorder.ui.components.GlassSurface
import com.ryno.classrecorder.ui.components.ListCardDivider
import com.ryno.classrecorder.ui.components.ListPageCard
import com.ryno.classrecorder.ui.components.ListPageRow
import com.ryno.classrecorder.ui.components.SearchCapsule
import com.ryno.classrecorder.ui.components.SettingsIcon
import com.ryno.classrecorder.ui.components.SheetGroup
import com.ryno.classrecorder.ui.theme.AppShape
import com.ryno.classrecorder.ui.theme.AppSpace
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.GlassTier
import com.ryno.classrecorder.ui.theme.StatusTone
import com.ryno.classrecorder.ui.theme.tabular
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Recordings library screen rebuilt in DSH Go's Mist Glass language:
 * - Borderless SearchCapsule + compact circle GlassIconButtons (Select, .zip, MCP)
 * - Quiet ink-selected CapsuleChoiceChip course filters
 * - Day-grouped ListPageCards with inline preview playback & long-press multi-select
 * - Floating Chrome-tier SelectionDock and AppSheet mass export
 */
@Composable
fun LibraryScreen(
    ui: MainUiState,
    showMcpSheetInitial: Boolean = false,
    onDismissMcpSheet: () -> Unit = {},
    onToggleMcp: () -> Unit,
    onUpdateNotes: (String, String, List<String>) -> Unit,
    onDeleteRecording: (String) -> Unit,
    onSaveFilenameTemplate: (String, String) -> Unit = { _, _ -> },
    onShowMessage: (String) -> Unit = {},
    onOpenPlayback: (RecordingRecord) -> Unit = {},
    topContentInset: Dp = 0.dp,
    onScrolledUnderChange: (Float) -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val dsh = ClassRecorderTheme.dshColors
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val listState = rememberLazyListState()

    var query by remember { mutableStateOf("") }
    var selectedCourse by remember { mutableStateOf<String?>(null) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    var showMcpSheet by remember(showMcpSheetInitial) { mutableStateOf(showMcpSheetInitial) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var pendingExportCourse by remember { mutableStateOf<String?>(null) }
    var exportSelectedOnly by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }

    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun stopInlinePlayer() {
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        playingId = null
    }

    DisposableEffect(Unit) {
        onDispose { stopInlinePlayer() }
    }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedIds = emptySet()
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

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val targets = when {
            exportSelectedOnly -> ui.recordings.filter { selectedIds.contains(it.id) }
            pendingExportCourse != null -> ui.recordings.filter {
                it.courseName.ifBlank { "Uncategorized" } == pendingExportCourse
            }
            else -> ui.recordings
        }
        isExporting = true
        scope.launch {
            val msg = exportRecordingsToZip(context, uri, targets)
            isExporting = false
            showExportSheet = false
            if (exportSelectedOnly) {
                selectionMode = false
                selectedIds = emptySet()
                exportSelectedOnly = false
            }
            onShowMessage(msg)
        }
    }

    val courseCounts = remember(ui.recordings) {
        ui.recordings
            .groupingBy { it.courseName.ifBlank { "Uncategorized" } }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
    }

    val filtered = remember(ui.recordings, query, selectedCourse) {
        val q = query.trim().lowercase()
        ui.recordings
            .filter { rec ->
                val cName = rec.courseName.ifBlank { "Uncategorized" }
                val courseOk = selectedCourse == null || cName == selectedCourse
                val queryOk = q.isEmpty() ||
                    cName.lowercase().contains(q) ||
                    rec.notes.lowercase().contains(q) ||
                    rec.room.lowercase().contains(q) ||
                    rec.teacher.lowercase().contains(q) ||
                    rec.tags.any { it.lowercase().contains(q) }
                courseOk && queryOk
            }
            .sortedByDescending { it.startedAt }
    }

    val groupedByDay = remember(filtered) {
        filtered.groupBy { UiFormatters.toLocalDate(it.startedAt) }
            .entries
            .sortedByDescending { it.key }
    }

    fun toggleInlinePlayback(rec: RecordingRecord) {
        if (playingId == rec.id) {
            stopInlinePlayer()
            return
        }
        stopInlinePlayer()
        val f = File(rec.filePath)
        if (!f.exists()) {
            onShowMessage("Audio file not found on disk")
            return
        }
        runCatching {
            val mp = MediaPlayer().apply {
                setDataSource(f.absolutePath)
                prepare()
                setOnCompletionListener {
                    playingId = null
                    runCatching { release() }
                    mediaPlayer = null
                }
                start()
            }
            mediaPlayer = mp
            playingId = rec.id
        }.onFailure {
            stopInlinePlayer()
            onShowMessage("Unable to play recording")
        }
    }

    val bottomDockHeight = if (selectionMode && selectedIds.isNotEmpty()) navBottom + 96.dp else navBottom + 28.dp

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val sideInset = ((maxWidth - AppSpace.readingWidth) / 2f).coerceAtLeast(0.dp)

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = AppSpace.gutter + sideInset,
                end = AppSpace.gutter + sideInset,
                top = topContentInset + 8.dp,
                bottom = bottomDockHeight
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "library_toolbar") {
                if (selectionMode) {
                    SelectionHeaderRow(
                        selectedCount = selectedIds.size,
                        allSelected = filtered.isNotEmpty() && selectedIds.size >= filtered.size,
                        onCancel = {
                            selectionMode = false
                            selectedIds = emptySet()
                        },
                        onToggleAll = {
                            selectedIds = if (selectedIds.size >= filtered.size) {
                                emptySet()
                            } else {
                                filtered.map { it.id }.toSet()
                            }
                        }
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SearchCapsule(
                            query = query,
                            onQueryChange = { query = it },
                            hint = "Search courses, notes, tags",
                            modifier = Modifier.weight(1f)
                        )
                        if (ui.recordings.isNotEmpty()) {
                            GlassIconButton(
                                icon = Icons.Rounded.Checklist,
                                contentDescription = "Select recordings",
                                onClick = {
                                    selectionMode = true
                                    selectedIds = emptySet()
                                },
                                tier = GlassTier.Pill,
                                visualSize = 42.dp
                            )
                            GlassIconButton(
                                icon = Icons.Rounded.FolderZip,
                                contentDescription = "Export recordings as ZIP",
                                onClick = { showExportSheet = true },
                                tier = GlassTier.Pill,
                                visualSize = 42.dp
                            )
                        }
                        GlassIconButton(
                            icon = Icons.Rounded.Hub,
                            contentDescription = "MCP server status",
                            onClick = { showMcpSheet = true },
                            tier = GlassTier.Pill,
                            visualSize = 42.dp,
                            badgeTone = if (ui.mcpStatus.running) StatusTone.Ok else null
                        )
                    }
                }
            }

            if (courseCounts.size > 1) {
                item(key = "course_filters") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item(key = "__all__") {
                            CapsuleChoiceChip(
                                label = "All (${ui.recordings.size})",
                                selected = selectedCourse == null,
                                onClick = { selectedCourse = null }
                            )
                        }
                        items(
                            items = courseCounts,
                            key = { it.key }
                        ) { (courseName, count) ->
                            val (dotBg, _) = dsh.identityPair(courseName)
                            CapsuleChoiceChip(
                                label = "$courseName ($count)",
                                selected = selectedCourse == courseName,
                                dotColor = dotBg,
                                onClick = {
                                    selectedCourse = if (selectedCourse == courseName) null else courseName
                                }
                            )
                        }
                    }
                }
            }

            when {
                ui.recordings.isEmpty() -> {
                    item(key = "empty_library") {
                        Spacer(Modifier.height(16.dp))
                        EmptyView(
                            icon = Icons.Rounded.GraphicEq,
                            title = "No recordings yet",
                            message = "Captured lectures appear here grouped by day and tagged automatically from your schedule."
                        )
                    }
                }
                filtered.isEmpty() -> {
                    item(key = "empty_filtered") {
                        Spacer(Modifier.height(12.dp))
                        EmptyView(
                            icon = Icons.Rounded.SearchOff,
                            title = "No matching recordings",
                            message = "Try a different search keyword or reset the course filter.",
                            actionLabel = "Clear filters",
                            onAction = {
                                query = ""
                                selectedCourse = null
                            },
                            compact = true
                        )
                    }
                }
                else -> {
                    items(
                        items = groupedByDay,
                        key = { it.key.toEpochDay() }
                    ) { (date, dayList) ->
                        ListPageCard(
                            title = UiFormatters.formatDayHeader(date),
                            trailingTitle = dayList.size.toString()
                        ) {
                            dayList.forEachIndexed { idx, rec ->
                                if (idx > 0) {
                                    ListCardDivider(startIndent = 64.dp)
                                }
                                RecordingLibraryRow(
                                    rec = rec,
                                    isPlaying = playingId == rec.id,
                                    selectionMode = selectionMode,
                                    isSelected = selectedIds.contains(rec.id),
                                    onRowClick = {
                                        if (selectionMode) {
                                            selectedIds = if (selectedIds.contains(rec.id)) {
                                                selectedIds - rec.id
                                            } else {
                                                selectedIds + rec.id
                                            }
                                        } else {
                                            stopInlinePlayer()
                                            onOpenPlayback(rec)
                                        }
                                    },
                                    onRowLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectionMode = true
                                        selectedIds = selectedIds + rec.id
                                    },
                                    onTogglePlay = { toggleInlinePlayback(rec) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (selectionMode && selectedIds.isNotEmpty()) {
            BackdropVeil(
                fromTop = false,
                solidHeight = navBottom + 76.dp,
                feather = 18.dp,
                alpha = 0.92f,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            SelectionDock(
                selectedCount = selectedIds.size,
                isExporting = isExporting,
                onExportSelected = {
                    exportSelectedOnly = true
                    pendingExportCourse = null
                    exportLauncher.launch("recordings_selected_${selectedIds.size}.zip")
                },
                onDeleteSelected = { showDeleteConfirm = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = AppSpace.floatInset + sideInset,
                        end = AppSpace.floatInset + sideInset,
                        bottom = navBottom + AppSpace.floatInset
                    )
            )
        }
    }

    if (showExportSheet) {
        ExportRecordingsSheet(
            totalCount = ui.recordings.size,
            courseCounts = courseCounts.map { it.key to it.value },
            isExporting = isExporting,
            onExportAll = {
                exportSelectedOnly = false
                pendingExportCourse = null
                exportLauncher.launch("class_recordings_all.zip")
            },
            onExportCourse = { course ->
                exportSelectedOnly = false
                pendingExportCourse = course
                exportLauncher.launch("${course}_recordings.zip")
            },
            onDismiss = { showExportSheet = false }
        )
    }

    if (showMcpSheet) {
        McpServerSheet(
            ui = ui,
            onDismiss = {
                showMcpSheet = false
                onDismissMcpSheet()
            },
            onToggleMcp = onToggleMcp,
            onCopied = onShowMessage
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${selectedIds.size} ${if (selectedIds.size == 1) "recording" else "recordings"}?") },
            text = { Text("Deleted lecture audio files are removed permanently from device storage.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = selectedIds.toList()
                        showDeleteConfirm = false
                        selectionMode = false
                        selectedIds = emptySet()
                        toDelete.forEach { id ->
                            if (playingId == id) stopInlinePlayer()
                            onDeleteRecording(id)
                        }
                    }
                ) {
                    Text("Delete", color = dsh.danger)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SelectionHeaderRow(
    selectedCount: Int,
    allSelected: Boolean,
    onCancel: () -> Unit,
    onToggleAll: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlassIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "Exit selection",
            onClick = onCancel,
            tier = GlassTier.Pill,
            visualSize = 40.dp
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$selectedCount selected",
            style = MaterialTheme.typography.titleMedium.tabular(),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        CapsuleChoiceChip(
            label = if (allSelected) "Deselect all" else "Select all",
            selected = allSelected,
            onClick = onToggleAll
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecordingLibraryRow(
    rec: RecordingRecord,
    isPlaying: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onRowClick: () -> Unit,
    onRowLongClick: () -> Unit,
    onTogglePlay: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val dsh = ClassRecorderTheme.dshColors
    val surfaces = ClassRecorderTheme.surfaces
    val courseTitle = rec.courseName.ifBlank { "Uncategorized" }
    val (identityBg, identityFg) = dsh.identityPair(courseTitle)

    val metaLine = buildString {
        append(UiFormatters.formatClockTime(rec.startedAt))
        append(" · ")
        append(UiFormatters.formatDuration(rec.durationMs))
        if (rec.room.isNotBlank()) {
            append(" · ")
            append(rec.room)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 68.dp)
            .combinedClickable(
                onClick = onRowClick,
                onLongClick = onRowLongClick
            )
            .padding(start = 16.dp, top = 10.dp, end = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            selectionMode -> {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) scheme.primary else scheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.Check else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isSelected) scheme.onPrimary else dsh.textTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            isPlaying -> {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(dsh.dangerContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = dsh.onDangerContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(identityBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = identityFg,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = courseTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                val firstTag = rec.tags.firstOrNull()
                if (!firstTag.isNullOrBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(
                                if (surfaces.darkTheme) scheme.onSurface.copy(alpha = 0.09f)
                                else scheme.onSurface.copy(alpha = 0.06f)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (firstTag.startsWith("#")) firstTag else "#$firstTag",
                            style = MaterialTheme.typography.labelSmall,
                            color = scheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))
            Text(
                text = metaLine,
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = dsh.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (rec.notes.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = rec.notes.lineSequence().firstOrNull().orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (!selectionMode) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (isPlaying) "Stop preview" else "Preview audio",
                        onClick = onTogglePlay
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Stop preview" else "Preview audio",
                    tint = if (isPlaying) dsh.danger else scheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun SelectionDock(
    selectedCount: Int,
    isExporting: Boolean,
    onExportSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        tier = GlassTier.Chrome,
        shape = AppShape.radiusXl,
        contentPadding = PaddingValues(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DshSecondaryButton(
                label = if (isExporting) "Exporting…" else "Export (.zip)",
                icon = Icons.Rounded.FolderZip,
                enabled = !isExporting,
                onClick = onExportSelected,
                modifier = Modifier.weight(1f)
            )
            DshPrimaryButton(
                label = "Delete ($selectedCount)",
                icon = Icons.Rounded.DeleteOutline,
                danger = true,
                onClick = onDeleteSelected,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ExportRecordingsSheet(
    totalCount: Int,
    courseCounts: List<Pair<String, Int>>,
    isExporting: Boolean,
    onExportAll: () -> Unit,
    onExportCourse: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dsh = ClassRecorderTheme.dshColors
    AppSheet(
        onDismiss = onDismiss,
        title = "Export recordings (.zip)"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpace.sheetGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Bundles .m4a lecture files organized by course folder alongside a metadata.json manifest of notes and tags.",
                style = MaterialTheme.typography.bodySmall,
                color = dsh.textTertiary
            )
            DshPrimaryButton(
                label = if (isExporting) "Exporting…" else "Export all ($totalCount)",
                icon = Icons.Rounded.FolderZip,
                enabled = !isExporting && totalCount > 0,
                onClick = onExportAll,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (courseCounts.size > 1) {
            Spacer(Modifier.height(8.dp))
            SheetGroup(title = "By course") {
                courseCounts.forEachIndexed { idx, (course, count) ->
                    if (idx > 0) ListCardDivider(startIndent = 60.dp)
                    ListPageRow(
                        title = course,
                        meta = "$count ${if (count == 1) "file" else "files"}",
                        leading = {
                            SettingsIcon(
                                icon = Icons.Rounded.FolderZip,
                                identityKey = course
                            )
                        },
                        onClick = if (!isExporting) {
                            { onExportCourse(course) }
                        } else null
                    )
                }
            }
        }
    }
}

private suspend fun exportRecordingsToZip(
    context: Context,
    destUri: Uri,
    recordings: List<RecordingRecord>
): String = withContext(Dispatchers.IO) {
    try {
        val os = context.contentResolver.openOutputStream(destUri)
            ?: return@withContext "Failed to open destination file"
        var exportedCount = 0
        val metaArr = JSONArray()
        ZipOutputStream(os).use { zos ->
            for (rec in recordings) {
                val f = File(rec.filePath)
                if (!f.exists()) continue
                val folder = rec.courseName.ifBlank { "Uncategorized" }
                val entryName = "$folder/${f.name}"
                zos.putNextEntry(ZipEntry(entryName))
                f.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
                exportedCount++

                metaArr.put(
                    JSONObject().apply {
                        put("id", rec.id)
                        put("archivePath", entryName)
                        put("courseName", rec.courseName)
                        put("teacher", rec.teacher)
                        put("room", rec.room)
                        put("slot", rec.slot)
                        put("startedAt", rec.startedAt)
                        put("durationMs", rec.durationMs)
                        put("notes", rec.notes)
                        put("tags", JSONArray(rec.tags))
                    }
                )
            }
            zos.putNextEntry(ZipEntry("metadata.json"))
            zos.write(metaArr.toString(2).toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        "Exported $exportedCount recordings to ZIP"
    } catch (e: Exception) {
        "Export failed: ${e.message}"
    }
}
