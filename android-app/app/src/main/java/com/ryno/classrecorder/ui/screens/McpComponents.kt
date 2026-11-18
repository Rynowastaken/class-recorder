package com.ryno.classrecorder.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.ui.MainUiState
import com.ryno.classrecorder.ui.components.AppSheet
import com.ryno.classrecorder.ui.components.CodeWell
import com.ryno.classrecorder.ui.components.DshIcons
import com.ryno.classrecorder.ui.components.ListCardDivider
import com.ryno.classrecorder.ui.components.ListPageRow
import com.ryno.classrecorder.ui.components.OverviewCard
import com.ryno.classrecorder.ui.components.OverviewFooterRow
import com.ryno.classrecorder.ui.components.OverviewGlyph
import com.ryno.classrecorder.ui.components.SheetGroup
import com.ryno.classrecorder.ui.components.dshSwitchColors
import com.ryno.classrecorder.ui.theme.AppSpace
import com.ryno.classrecorder.ui.theme.StatusTone

/**
 * Shared LAN endpoint + ADB forward copy wells used in both [SettingsScreen] and [McpServerSheet].
 */
@Composable
fun McpEndpointsBlock(
    lanIp: String,
    port: Int,
    onCopied: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lanUrl = "http://$lanIp:$port/mcp"
    val adbCmd = "adb forward tcp:$port tcp:$port"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
}

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
    val running = ui.mcpStatus.running
    val port = ui.mcpStatus.port
    val lanUrl = "http://${ui.lanIp}:$port/mcp"

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
                        icon = DshIcons.Hub,
                        tintSlot = 0
                    )
                },
                title = if (running) "Server active" else "Server stopped",
                subtitle = if (running) "Listening on port $port" else "Enable so local or LAN agents can connect",
                trailing = {
                    Switch(
                        checked = running,
                        onCheckedChange = { onToggleMcp() },
                        colors = dshSwitchColors()
                    )
                },
                footer = {
                    OverviewFooterRow(
                        tone = if (running) StatusTone.Ok else StatusTone.Idle,
                        text = if (running) lanUrl else "Port $port · JSON-RPC 2.0 over HTTP"
                    )
                }
            )

            McpEndpointsBlock(
                lanIp = ui.lanIp,
                port = port,
                onCopied = onCopied
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
                        icon = DshIcons.Terminal
                    )
                }
            }
        }
    }
}
