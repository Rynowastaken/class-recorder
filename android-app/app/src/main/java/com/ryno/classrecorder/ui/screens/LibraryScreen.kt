package com.ryno.classrecorder.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.core.RecordingRecord
import com.ryno.classrecorder.ui.MainUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    ui: MainUiState,
    showMcpSheetInitial: Boolean,
    onDismissMcpSheet: () -> Unit,
    onToggleMcp: () -> Unit,
    onUpdateNotes: (String, String, List<String>) -> Unit,
    onDeleteRecording: (String) -> Unit
) {
    var playingId by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var selectedRec by remember { mutableStateOf<RecordingRecord?>(null) }
    var showMcpSheet by remember(showMcpSheetInitial) { mutableStateOf(showMcpSheetInitial) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    fun togglePlay(rec: RecordingRecord) {
        if (playingId == rec.id) {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            playingId = null
        } else {
            mediaPlayer?.release()
            try {
                val mp = MediaPlayer().apply {
                    setDataSource(rec.filePath)
                    prepare()
                    setOnCompletionListener {
                        playingId = null
                    }
                    start()
                }
                mediaPlayer = mp
                playingId = rec.id
            } catch (_: Exception) {
                playingId = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Recordings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${ui.recordings.size} files organized by course",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalButton(onClick = { showMcpSheet = true }) {
                Icon(Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (ui.mcpStatus.running) "MCP :${ui.mcpStatus.port}" else "MCP Off")
            }
        }

        Spacer(Modifier.height(12.dp))

        if (ui.recordings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No recordings yet. Record a class or trigger one via MCP!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val grouped = remember(ui.recordings) {
                ui.recordings.groupBy { it.courseName.ifBlank { "Uncategorized" } }
            }
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                grouped.forEach { (courseName, itemsForCourse) ->
                    item(key = "header-$courseName") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "$courseName (${itemsForCourse.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    items(itemsForCourse, key = { it.id }) { rec ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRec = rec },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalIconButton(onClick = { togglePlay(rec) }) {
                                    Icon(
                                        imageVector = if (playingId == rec.id) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "Play/Stop"
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = formatTimestamp(rec.startedAt),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${formatDuration(rec.durationMs)} • ${rec.room.ifBlank { rec.status }}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (rec.notes.isNotBlank()) {
                                        Text(
                                            text = rec.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 2
                                        )
                                    }
                                }
                                IconButton(onClick = { selectedRec = rec }) {
                                    Icon(Icons.Default.EditNote, contentDescription = "Details")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedRec?.let { rec ->
        var notesText by remember(rec.id) { mutableStateOf(rec.notes) }
        var tagsText by remember(rec.id) { mutableStateOf(rec.tags.joinToString(", ")) }
        ModalBottomSheet(onDismissRequest = { selectedRec = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(rec.courseName, style = MaterialTheme.typography.titleMedium)
                Text(
                    "File: ${rec.filePath}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Notes (synced with MCP agents)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text("Tags (comma-separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            onDeleteRecording(rec.id)
                            selectedRec = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                    Button(onClick = {
                        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onUpdateNotes(rec.id, notesText, tags)
                        selectedRec = null
                    }) {
                        Text("Save")
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }

    if (showMcpSheet) {
        val clipboard = LocalClipboardManager.current
        val lanUrl = "http://${ui.lanIp}:${ui.mcpStatus.port}/mcp"
        val adbCmd = "adb forward tcp:${ui.mcpStatus.port} tcp:${ui.mcpStatus.port}"
        ModalBottomSheet(onDismissRequest = {
            showMcpSheet = false
            onDismissMcpSheet()
        }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("On-Device MCP Server", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (ui.mcpStatus.running) "Listening on 0.0.0.0:${ui.mcpStatus.port}"
                            else "Stopped",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = ui.mcpStatus.running,
                        onCheckedChange = { onToggleMcp() }
                    )
                }

                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Wi-Fi LAN Endpoint", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(lanUrl, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { clipboard.setText(AnnotatedString(lanUrl)) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy URL")
                            }
                        }
                        Text("USB ADB Tunnel", style = MaterialTheme.typography.labelMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(adbCmd, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                            IconButton(onClick = { clipboard.setText(AnnotatedString(adbCmd)) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy ADB command")
                            }
                        }
                    }
                }

                if (ui.mcpStatus.recentCalls.isNotEmpty()) {
                    Text("Recent Agent Calls", style = MaterialTheme.typography.labelMedium)
                    ui.mcpStatus.recentCalls.takeLast(6).reversed().forEach { line ->
                        Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private fun formatTimestamp(epochSec: Long): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        sdf.format(Date(epochSec * 1000))
    } catch (_: Exception) {
        epochSec.toString()
    }
}
