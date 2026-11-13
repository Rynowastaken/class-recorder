package com.ryno.classrecorder.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.core.CourseSlot
import com.ryno.classrecorder.service.RecorderState
import com.ryno.classrecorder.ui.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    ui: MainUiState,
    recorder: RecorderState,
    hasMicPermission: Boolean,
    onRequestMicPermission: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onTogglePause: () -> Unit,
    onSelectCourseOverride: (CourseSlot?) -> Unit,
    onOpenMcpSheet: () -> Unit
) {
    var showCoursePicker by remember { mutableStateOf(false) }

    val activeCourseName = when {
        recorder.isRecording -> recorder.activeRecording?.courseName ?: "Recording"
        ui.selectedCourseOverride != null -> ui.selectedCourseOverride.name
        ui.currentClass != null -> ui.currentClass.course.name
        else -> "Uncategorized"
    }

    val activeSubline = when {
        recorder.isRecording -> {
            val r = recorder.activeRecording
            listOfNotNull(r?.room?.takeIf { it.isNotBlank() }, r?.teacher?.takeIf { it.isNotBlank() })
                .joinToString(" • ")
                .ifBlank { "Saving to $activeCourseName/" }
        }
        ui.selectedCourseOverride != null -> {
            val c = ui.selectedCourseOverride
            "Manual: ${c.room} ${c.teacher}".trim()
        }
        ui.currentClass != null -> {
            val prefix = if (ui.currentClass.kind == "upcoming") "Upcoming" else "In Class Now"
            "$prefix • Slot ${ui.currentClass.slot} (${ui.currentClass.time}) • ${ui.currentClass.course.room}"
        }
        ui.schedule == null -> "No schedule synced yet — tap Schedule tab to import"
        else -> "No class right now — will save to Uncategorized/"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top status row: Current Class Card + MCP pill
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AssistChip(
                onClick = onOpenMcpSheet,
                label = {
                    Text(
                        if (ui.mcpStatus.running) "MCP Active :${ui.mcpStatus.port}"
                        else "MCP Off"
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (ui.mcpStatus.running) Icons.Default.Hub else Icons.Default.PortableWifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !recorder.isRecording) { showCoursePicker = true },
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeCourseName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeSubline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!recorder.isRecording) {
                        FilledTonalIconButton(onClick = { showCoursePicker = true }) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = "Change class")
                        }
                    }
                }
            }
        }

        // Center timer + live amplitude waveform
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = formatDuration(recorder.elapsedMs),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Light,
                color = if (recorder.isRecording) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )

            WaveformBars(
                amplitude = if (recorder.isRecording && !recorder.isPaused) recorder.amplitude else 0.04f,
                isActive = recorder.isRecording && !recorder.isPaused
            )

            if (!hasMicPermission) {
                FilledTonalButton(onClick = onRequestMicPermission) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Grant Microphone Permission")
                }
            }
        }

        // Bottom Material 3 Record / Pause / Stop controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (recorder.isRecording) {
                FilledTonalIconButton(
                    onClick = onTogglePause,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = if (recorder.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (recorder.isPaused) "Resume" else "Pause"
                    )
                }
                Spacer(Modifier.width(24.dp))
                LargeFloatingActionButton(
                    onClick = onStopRecording,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop recording", modifier = Modifier.size(36.dp))
                }
            } else {
                LargeFloatingActionButton(
                    onClick = {
                        if (hasMicPermission) onStartRecording() else onRequestMicPermission()
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Start recording", modifier = Modifier.size(36.dp))
                }
            }
        }
    }

    if (showCoursePicker) {
        val uniqueCourses = remember(ui.schedule) {
            ui.schedule?.slots
                ?.flatMap { it.days }
                ?.filterNotNull()
                ?.distinctBy { it.name }
                ?: emptyList()
        }
        ModalBottomSheet(onDismissRequest = { showCoursePicker = false }) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                Text("Tag Recording to Class", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                ListItem(
                    headlineContent = { Text("Auto-detect from schedule") },
                    supportingContent = { Text(ui.currentClass?.course?.name ?: "Uncategorized if no class active") },
                    leadingContent = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                    modifier = Modifier.clickable {
                        onSelectCourseOverride(null)
                        showCoursePicker = false
                    }
                )
                HorizontalDivider()
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(uniqueCourses) { course ->
                        ListItem(
                            headlineContent = { Text(course.name) },
                            supportingContent = { Text("${course.teacher} ${course.room}".trim()) },
                            leadingContent = { Icon(Icons.Default.School, contentDescription = null) },
                            modifier = Modifier.clickable {
                                onSelectCourseOverride(course)
                                showCoursePicker = false
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun WaveformBars(amplitude: Float, isActive: Boolean) {
    val barCount = 24
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val factor = 0.35f + 0.65f * kotlin.math.sin((i + 1) * 0.55f).let { kotlin.math.abs(it) }
            val target by animateFloatAsState(
                targetValue = if (isActive) (0.12f + amplitude * factor).coerceIn(0.08f, 1f) else 0.08f,
                label = "bar$i"
            )
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight(target)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val min = totalSec / 60
    val sec = totalSec % 60
    val hr = min / 60
    return if (hr > 0) String.format("%02d:%02d:%02d", hr, min % 60, sec)
    else String.format("%02d:%02d", min, sec)
}
