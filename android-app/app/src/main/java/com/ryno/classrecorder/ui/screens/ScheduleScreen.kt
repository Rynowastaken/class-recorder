package com.ryno.classrecorder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.core.CourseSlot
import com.ryno.classrecorder.ui.MainUiState
import java.time.LocalDate

private val DAY_NAMES = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    ui: MainUiState,
    onSyncSchedule: (String) -> Unit,
    onRecordCourse: (CourseSlot, String) -> Unit
) {
    val todayIdx = remember { (LocalDate.now().dayOfWeek.value - 1).coerceIn(0, 6) }
    var selectedDay by remember { mutableIntStateOf(todayIdx) }
    var showSourceSheet by remember { mutableStateOf(false) }
    var urlInput by remember(ui.scheduleSourceInput) { mutableStateOf(ui.scheduleSourceInput) }

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
                    text = ui.schedule?.schoolName ?: "Class Schedule",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (ui.schedule != null) "${ui.schedule.slots.size} time slots synced"
                    else "Not synced yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalIconButton(onClick = { onSyncSchedule(ui.scheduleSourceInput) }) {
                    if (ui.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh schedule")
                    }
                }
                FilledTonalButton(onClick = { showSourceSheet = true }) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Source")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(DAY_NAMES) { idx, label ->
                FilterChip(
                    selected = selectedDay == idx,
                    onClick = { selectedDay = idx },
                    label = {
                        Text(if (idx == todayIdx) "$label (Today)" else label)
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        val dayRows = remember(ui.schedule, selectedDay) {
            ui.schedule?.slots?.mapNotNull { row ->
                val course = row.days.getOrNull(selectedDay)
                if (course != null) Triple(row.slot, row.time, course) else null
            } ?: emptyList()
        }

        if (ui.schedule == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No schedule loaded yet", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onSyncSchedule(ui.scheduleSourceInput) }) {
                        Text("Sync NUTC Schedule (qVaaFS5A)")
                    }
                }
            }
        } else if (dayRows.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No classes on ${DAY_NAMES[selectedDay]}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(dayRows) { _, (slot, time, course) ->
                    val dotColor = parseHexColor(course.color)
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRecordCourse(course, slot) },
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(dotColor)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = course.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Slot $slot ($time) • Room ${course.room} • ${course.teacher}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalIconButton(onClick = { onRecordCourse(course, slot) }) {
                                Icon(Icons.Default.Mic, contentDescription = "Record this class")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSourceSheet) {
        ModalBottomSheet(onDismissRequest = { showSourceSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Schedule Source", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Paste a uni-campus-app share link/code (e.g. https://uni-campus-app.pages.dev/s/qVaaFS5A) or a Personal-NUTC-Eportal schedule JSON URL.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Schedule URL or Share Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showSourceSheet = false }) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        onSyncSchedule(urlInput)
                        showSourceSheet = false
                    }) {
                        Text("Sync Now")
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color(0xFF6750A4)
    }
}
