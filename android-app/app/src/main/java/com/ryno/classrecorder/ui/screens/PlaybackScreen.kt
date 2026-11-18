package com.ryno.classrecorder.ui.screens

import android.media.MediaPlayer
import android.media.PlaybackParams
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay5
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.core.RecordingRecord
import com.ryno.classrecorder.ui.components.AppPage
import com.ryno.classrecorder.ui.components.AppSheet
import com.ryno.classrecorder.ui.components.CodeWell
import com.ryno.classrecorder.ui.components.DshConfirmSheet
import com.ryno.classrecorder.ui.components.DshPrimaryButton
import com.ryno.classrecorder.ui.components.DshSecondaryButton
import com.ryno.classrecorder.ui.components.DshTextField
import com.ryno.classrecorder.ui.components.GlassIconButton
import com.ryno.classrecorder.ui.components.GlassSurface
import com.ryno.classrecorder.ui.components.ListPageCard
import com.ryno.classrecorder.ui.components.OverviewGlyph
import com.ryno.classrecorder.ui.components.PageHero
import com.ryno.classrecorder.ui.components.PillSegment
import com.ryno.classrecorder.ui.components.SegmentedPill
import com.ryno.classrecorder.ui.theme.AppShape
import com.ryno.classrecorder.ui.theme.AppSpace
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.GlassTier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.File

private val SPEED_SEGMENTS = listOf(
    PillSegment(0.5f, "0.5×"),
    PillSegment(1.0f, "1×"),
    PillSegment(1.25f, "1.25×"),
    PillSegment(1.5f, "1.5×"),
    PillSegment(2.0f, "2×")
)

/**
 * Full-screen lecture playback & note editor built on DSH Go's [AppPage] + [PageHero]:
 * - Interactive Android [PredictiveBackHandler] gesture scaling
 * - Scroll-collapsing hero with course identity glyph, tabular metadata, tags, and [PageHero] stats
 * - Real PCM-decoded [ScrubWaveformCanvas] card + >= 3:1 seek readout/track
 * - -5s / Play-Pause / +10s transport row
 * - SegmentedPill playback speed selector
 * - Synced notes card + CodeWell file identity + Mist Glass [DshConfirmSheet] delete guard
 */
@Composable
fun PlaybackScreen(
    recording: RecordingRecord,
    onDismiss: () -> Unit,
    onUpdateNotes: (String, String, List<String>) -> Unit,
    onDeleteRecording: (String) -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scheme = MaterialTheme.colorScheme
    val dsh = ClassRecorderTheme.dshColors
    val mono = ClassRecorderTheme.mono

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

    var isPlaying by remember(recording.id) { mutableStateOf(false) }
    var currentPosMs by remember(recording.id) { mutableLongStateOf(0L) }
    var durationMs by remember(recording.id) { mutableLongStateOf(recording.durationMs.coerceAtLeast(1L)) }
    var speed by remember(recording.id) { mutableFloatStateOf(1.0f) }
    var showNotesSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var decodedPeaks by remember(recording.id) { mutableStateOf<List<Float>>(emptyList()) }

    val audioFile = remember(recording.filePath) { File(recording.filePath) }
    val fileExists = remember(recording.filePath) { audioFile.exists() && audioFile.length() > 0L }
    val fileSizeLabel = remember(recording.filePath) {
        if (fileExists) UiFormatters.formatFileSize(audioFile.length()) else "Unavailable"
    }

    LaunchedEffect(recording.id, recording.filePath) {
        decodedPeaks = AudioWaveformDecoder.extractPeaks(recording.filePath, bucketCount = 72)
    }

    val mediaPlayer = remember(recording.id) {
        MediaPlayer().apply {
            runCatching {
                setDataSource(recording.filePath)
                prepare()
                durationMs = duration.toLong().coerceAtLeast(1L)
                setOnCompletionListener {
                    isPlaying = false
                    currentPosMs = durationMs
                }
            }
        }
    }

    DisposableEffect(recording.id) {
        onDispose {
            runCatching { if (mediaPlayer.isPlaying) mediaPlayer.stop() }
            runCatching { mediaPlayer.release() }
        }
    }

    LaunchedEffect(recording.id, isPlaying) {
        while (isPlaying) {
            runCatching {
                currentPosMs = mediaPlayer.currentPosition.toLong().coerceIn(0L, durationMs)
            }
            delay(32L)
        }
    }

    fun togglePlayPause() {
        runCatching {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.pause()
                isPlaying = false
            } else {
                if (currentPosMs >= durationMs - 150L) {
                    mediaPlayer.seekTo(0)
                    currentPosMs = 0L
                }
                mediaPlayer.start()
                if (speed != 1.0f) {
                    mediaPlayer.playbackParams = PlaybackParams().setSpeed(speed)
                }
                isPlaying = true
            }
        }
    }

    fun seekTo(targetMs: Long) {
        val clamped = targetMs.coerceIn(0L, durationMs)
        runCatching {
            mediaPlayer.seekTo(clamped.toInt())
            currentPosMs = clamped
        }
    }

    fun applySpeed(next: Float) {
        speed = next
        runCatching {
            val wasPlaying = mediaPlayer.isPlaying
            mediaPlayer.playbackParams = PlaybackParams().setSpeed(next)
            if (!wasPlaying) mediaPlayer.pause()
        }
    }

    val courseTitle = recording.courseName.ifBlank { "Uncategorized" }
    val fileName = remember(recording.filePath) { audioFile.name }
    val progress = (currentPosMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)

    val heroSubtitle = remember(recording) {
        listOfNotNull(
            UiFormatters.formatTimestamp(recording.startedAt).takeIf { it.isNotBlank() },
            recording.slot.takeIf { it.isNotBlank() }?.let { "P$it" },
            recording.room.takeIf { it.isNotBlank() },
            recording.teacher.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
    }

    val heroStats = remember(durationMs, fileSizeLabel) {
        listOf(
            UiFormatters.formatDuration(durationMs) to "Duration",
            "128 kbps" to "AAC audio",
            fileSizeLabel to "File size"
        )
    }

    val slidePx = with(density) { 40.dp.toPx() } * backProgress

    AppPage(
        title = courseTitle,
        onBack = onDismiss,
        modifier = Modifier.graphicsLayer {
            val s = 1f - 0.05f * backProgress
            scaleX = s
            scaleY = s
            translationX = slidePx
            alpha = 1f - 0.18f * backProgress
        },
        actions = {
            GlassIconButton(
                icon = Icons.Rounded.EditNote,
                contentDescription = "Edit notes and tags",
                onClick = { showNotesSheet = true }
            )
        },
        hero = {
            PageHero(
                title = courseTitle,
                subtitle = heroSubtitle,
                tags = recording.tags,
                stats = heroStats,
                leading = {
                    OverviewGlyph(
                        icon = Icons.Rounded.GraphicEq,
                        identityKey = courseTitle
                    )
                }
            )
        }
    ) {
        item(key = "waveform_card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppSpace.gutter),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    tier = GlassTier.Card,
                    shape = AppShape.radiusLg,
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ScrubWaveformCanvas(
                            peaks = decodedPeaks,
                            progress = progress,
                            onScrub = { frac ->
                                seekTo((frac * durationMs.toFloat()).toLong())
                            },
                            playedColor = scheme.primary,
                            unplayedColor = dsh.textTertiary,
                            playheadColor = scheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(152.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(dsh.textTertiary.copy(alpha = 0.60f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progress)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(scheme.primary)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = UiFormatters.formatDuration(currentPosMs),
                                style = mono.seekLabel,
                                color = scheme.onSurface
                            )
                            Text(
                                text = UiFormatters.formatDuration(durationMs),
                                style = mono.seekLabel,
                                color = dsh.textTertiary
                            )
                        }
                    }
                }

                // Transport controls: -5s · Play/Pause · +10s
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassSurface(
                        modifier = Modifier.size(52.dp),
                        tier = GlassTier.Card,
                        circle = true,
                        enabled = fileExists,
                        onClick = { seekTo(currentPosMs - 5_000L) }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Replay5,
                                contentDescription = "Rewind 5 seconds",
                                tint = if (fileExists) scheme.onSurface else dsh.textDisabled,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    DshPrimaryButton(
                        label = if (isPlaying) "Pause" else "Play",
                        icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        enabled = fileExists,
                        onClick = ::togglePlayPause,
                        minHeight = 56.dp,
                        modifier = Modifier.weight(1f)
                    )

                    GlassSurface(
                        modifier = Modifier.size(52.dp),
                        tier = GlassTier.Card,
                        circle = true,
                        enabled = fileExists,
                        onClick = { seekTo(currentPosMs + 10_000L) }
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Forward10,
                                contentDescription = "Forward 10 seconds",
                                tint = if (fileExists) scheme.onSurface else dsh.textDisabled,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Playback speed
                ListPageCard(
                    title = "Playback speed",
                    contentPadding = PaddingValues(12.dp)
                ) {
                    SegmentedPill(
                        segments = SPEED_SEGMENTS,
                        selected = speed,
                        onSelect = ::applySpeed,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Lecture notes
                ListPageCard(
                    title = "Lecture notes",
                    contentPadding = PaddingValues(0.dp)
                ) {
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        tier = GlassTier.Pill,
                        shape = AppShape.radiusLg,
                        onClick = { showNotesSheet = true },
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (recording.notes.isBlank()) {
                                Text(
                                    text = "Tap to add lecture notes, summary points, or #tags…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = dsh.textTertiary
                                )
                            } else {
                                Text(
                                    text = recording.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = scheme.onSurface
                                )
                            }
                        }
                    }
                }

                // File identity & delete
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CodeWell(
                        label = "Audio file",
                        text = fileName,
                        onCopy = {
                            UiFormatters.copyToClipboard(context, "Filename", fileName)
                        }
                    )

                    DshSecondaryButton(
                        label = "Delete recording",
                        icon = Icons.Rounded.DeleteOutline,
                        danger = true,
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    if (showNotesSheet) {
        NotesEditorSheet(
            initialNotes = recording.notes,
            initialTags = recording.tags,
            onSave = { notes, tags ->
                onUpdateNotes(recording.id, notes, tags)
                showNotesSheet = false
            },
            onDismiss = { showNotesSheet = false }
        )
    }

    if (showDeleteDialog) {
        DshConfirmSheet(
            title = "Delete recording?",
            message = "“$fileName” will be permanently deleted from device storage.",
            confirmLabel = "Delete",
            danger = true,
            onConfirm = {
                showDeleteDialog = false
                runCatching { if (mediaPlayer.isPlaying) mediaPlayer.stop() }
                onDeleteRecording(recording.id)
                onDismiss()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NotesEditorSheet(
    initialNotes: String,
    initialTags: List<String>,
    onSave: (String, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var notes by remember(initialNotes) { mutableStateOf(initialNotes) }
    var tagInput by remember { mutableStateOf("") }
    val tags = remember(initialTags) {
        mutableStateListOf<String>().apply { addAll(initialTags) }
    }
    val scheme = MaterialTheme.colorScheme
    val surfaces = ClassRecorderTheme.surfaces

    fun addTag() {
        val cleaned = tagInput.trim().removePrefix("#").trim()
        if (cleaned.isNotEmpty() && !tags.contains(cleaned)) {
            tags.add(cleaned)
        }
        tagInput = ""
    }

    AppSheet(
        onDismiss = onDismiss,
        title = "Notes & tags"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpace.sheetGutter),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            DshTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Lecture notes",
                placeholder = "Key takeaways, exam topics, timestamps…",
                singleLine = false,
                minLines = 4,
                maxLines = 8
            )

            DshTextField(
                value = tagInput,
                onValueChange = { tagInput = it },
                label = "Tags",
                placeholder = "Add a tag (e.g. midterm, ch4)",
                imeAction = ImeAction.Done,
                onImeAction = ::addTag,
                trailing = {
                    GlassIconButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Add tag",
                        onClick = ::addTag,
                        tier = GlassTier.Pill,
                        visualSize = 36.dp,
                        iconSize = 18.dp
                    )
                }
            )

            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tags.forEach { tag ->
                        GlassSurface(
                            tier = GlassTier.Pill,
                            stadium = true,
                            onClick = { tags.remove(tag) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "#$tag",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (surfaces.darkTheme) scheme.onSurface else scheme.onSurfaceVariant
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove $tag",
                                    modifier = Modifier.size(14.dp),
                                    tint = scheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            DshPrimaryButton(
                label = "Save notes",
                onClick = { onSave(notes.trim(), tags.toList()) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
