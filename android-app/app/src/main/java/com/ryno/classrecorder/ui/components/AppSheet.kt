package com.ryno.classrecorder.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.ui.theme.AppShape
import com.ryno.classrecorder.ui.theme.AppSpace
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.GlassTier

/**
 * Unified modal bottom sheet ported from DSH Go's app_sheet.dart:
 * 28dp top corners in Sheet-tier Mist Glass, 36×4dp drag handle inside a 48dp
 * accessible dismiss target, and IME + navigation-bar safe padding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            tier = GlassTier.Sheet,
            shape = AppShape.sheetTop
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 16.dp)
            ) {
                SheetDragHandle(onDismiss = onDismiss)
                if (!title.isNullOrBlank()) {
                    SheetHeader(
                        title = title,
                        actions = actions
                    )
                }
                content()
            }
        }
    }
}

/**
 * 36×4dp stadium drag indicator inside a full-width 48dp accessible close button.
 */
@Composable
fun SheetDragHandle(
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val handleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(
                if (onDismiss != null) {
                    Modifier.clickable(
                        role = Role.Button,
                        onClickLabel = "Close sheet",
                        onClick = onDismiss
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(handleColor)
        )
    }
}

@Composable
fun SheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = AppSpace.sheetGutter,
                end = AppSpace.sheetGutter,
                bottom = 12.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

/**
 * Grouped card container inside an [AppSheet] (20dp horizontal sheet gutter,
 * Card-tier glass with 20dp corners).
 */
@Composable
fun SheetGroup(
    title: String? = null,
    modifier: Modifier = Modifier,
    horizontalMargin: Dp = AppSpace.sheetGutter,
    content: @Composable ColumnScope.() -> Unit
) {
    val dsh = ClassRecorderTheme.dshColors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalMargin, vertical = 6.dp)
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = dsh.textTertiary,
                modifier = Modifier
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                    .semantics { heading() }
            )
        }
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            tier = GlassTier.Card,
            shape = AppShape.radiusLg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                content = content
            )
        }
    }
}
