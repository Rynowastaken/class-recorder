package com.ryno.classrecorder.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.ui.MainUiState
import com.ryno.classrecorder.ui.components.AppSheet
import com.ryno.classrecorder.ui.components.CodeWell
import com.ryno.classrecorder.ui.components.ListCardDivider
import com.ryno.classrecorder.ui.components.ListPageRow
import com.ryno.classrecorder.ui.components.OverviewCard
import com.ryno.classrecorder.ui.components.OverviewFooterRow
import com.ryno.classrecorder.ui.components.OverviewGlyph
import com.ryno.classrecorder.ui.components.SheetGroup
import com.ryno.classrecorder.ui.theme.AppSpace
import com.ryno.classrecorder.ui.theme.StatusTone
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object UiFormatters {
    private val SAMPLE_PREVIEW_DATE: LocalDate = LocalDate.of(2026, 11, 18)
    private val TIME_RANGE_SPLITTER = Regex("""\s*(?:~|–|—|-)\s*""")
    private val CLOCK_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val TIMESTAMP_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)
    private val DAY_HEADER_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE · MMM d", Locale.US)

    /**
     * Formats duration milliseconds with tabular numerals (`mm:ss` or `h:mm:ss`).
     */
    fun formatDuration(ms: Long): String {
        val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Normalizes timetable range strings (`08:10~09:00`, `13:25 - 14:15`) to a clean en-dash (`08:10–09:00`).
     */
    fun formatTimeRange(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val parts = trimmed.split(TIME_RANGE_SPLITTER).filter { it.isNotBlank() }
        return if (parts.size == 2) {
            "${parts[0]}–${parts[1]}"
        } else {
            trimmed
        }
    }

    /**
     * Accepts either Unix epoch seconds (used by RustCore SQLite rows) or epoch milliseconds.
     */
    fun normalizeEpochMillis(epoch: Long): Long {
        if (epoch <= 0L) return 0L
        return if (epoch < 100_000_000_000L) epoch * 1000L else epoch
    }

    fun toLocalDate(epoch: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate {
        val ms = normalizeEpochMillis(epoch)
        return Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    }

    fun formatClockTime(epoch: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        if (epoch <= 0L) return "--:--"
        return runCatching {
            Instant.ofEpochMilli(normalizeEpochMillis(epoch)).atZone(zone).format(CLOCK_FMT)
        }.getOrDefault("--:--")
    }

    fun formatTimestamp(epoch: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        if (epoch <= 0L) return ""
        return runCatching {
            Instant.ofEpochMilli(normalizeEpochMillis(epoch)).atZone(zone).format(TIMESTAMP_FMT)
        }.getOrDefault(epoch.toString())
    }

    fun formatDayHeader(date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(DAY_HEADER_FMT)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        val mb = 1024L * 1024L
        return if (bytes >= mb) {
            String.format(Locale.US, "%.1f MB", bytes.toDouble() / mb.toDouble())
        } else {
            val kb = (bytes / 1024L).coerceAtLeast(1L)
            "$kb KB"
        }
    }

    /**
     * Deterministically previews `.m4a` filenames against `2026-11-18`, supporting all
     * RustCore template placeholders (including `{lesson number}` and `{lession number}`).
     */
    fun previewFilename(
        template: String,
        dateFormat: String,
        courseName: String = "大學之道",
        room: String = "3309",
        teacher: String = "何昕家",
        slot: String = "3"
    ): String {
        val effectiveTemplate = template.trim().ifEmpty { "{date}_{time}_{class}_{room}" }
        val formattedDate = formatPreviewDate(SAMPLE_PREVIEW_DATE, dateFormat.trim().ifEmpty { "yyyy-MM-dd" })
        val stem = effectiveTemplate
            .replace("{class}", courseName)
            .replace("{course}", courseName)
            .replace("{date}", formattedDate)
            .replace("{lesson number}", "1")
            .replace("{lession number}", "1")
            .replace("{lesson}", "1")
            .replace("{room}", room)
            .replace("{teacher}", teacher)
            .replace("{time}", "1015")
            .replace("{slot}", slot)
            .trim()
        return if (stem.endsWith(".m4a", ignoreCase = true)) stem else "$stem.m4a"
    }

    private fun formatPreviewDate(date: LocalDate, pattern: String): String {
        return runCatching {
            DateTimeFormatter.ofPattern(pattern, Locale.US).format(date)
        }.getOrElse {
            DateTimeFormatter.ISO_LOCAL_DATE.format(date)
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String) {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}

/** Package-level compatibility helpers for screen callers. */
fun formatDuration(ms: Long): String = UiFormatters.formatDuration(ms)
fun formatTimestamp(epoch: Long): String = UiFormatters.formatTimestamp(epoch)

/**
 * Shared Mist Glass MCP Server inspector & toggle sheet used from Recorder and Recordings.
 */
@Composable
fun McpServerSheet(
    ui: MainUiState,
    onDismiss: () -> Unit,
    onToggleMcp: () -> Unit,
    onCopied: (String) -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val running = ui.mcpStatus.running
    val port = ui.mcpStatus.port
    val lanUrl = "http://${ui.lanIp}:$port/mcp"
    val adbCmd = "adb forward tcp:$port tcp:$port"

    AppSheet(
        onDismiss = onDismiss,
        title = "On-device MCP server"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpace.sheetGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewCard(
                leading = {
                    OverviewGlyph(
                        icon = Icons.Rounded.Hub,
                        tintSlot = 0
                    )
                },
                title = if (running) "Server active" else "Server stopped",
                subtitle = if (running) "Listening on port $port" else "Enable so local or LAN agents can connect",
                trailing = {
                    Switch(
                        checked = running,
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
                        tone = if (running) StatusTone.Ok else StatusTone.Idle,
                        text = if (running) lanUrl else "Port $port · JSON-RPC 2.0 over HTTP"
                    )
                }
            )

            CodeWell(
                label = "LAN endpoint",
                text = lanUrl,
                onCopy = {
                    UiFormatters.copyToClipboard(context, "MCP LAN URL", lanUrl)
                    onCopied("Copied LAN endpoint")
                }
            )

            CodeWell(
                label = "USB / ADB tunnel",
                text = adbCmd,
                onCopy = {
                    UiFormatters.copyToClipboard(context, "ADB forward command", adbCmd)
                    onCopied("Copied ADB command")
                }
            )
        }

        if (ui.mcpStatus.recentCalls.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SheetGroup(title = "Recent agent calls") {
                val recent = ui.mcpStatus.recentCalls.takeLast(5).asReversed()
                recent.forEachIndexed { idx, call ->
                    if (idx > 0) ListCardDivider(startIndent = 60.dp)
                    ListPageRow(
                        title = call,
                        icon = Icons.Rounded.Terminal
                    )
                }
            }
        }
    }
}
