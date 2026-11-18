package com.ryno.classrecorder.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import com.ryno.classrecorder.ui.theme.AppShape
import com.ryno.classrecorder.ui.theme.AppSpace
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import com.ryno.classrecorder.ui.theme.GlassTier
import kotlin.math.roundToInt

/**
 * Unified modal bottom sheet ported from DSH Go's app_sheet.dart.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val surfaces = ClassRecorderTheme.surfaces
    val reduced = ClassRecorderTheme.reducedMotion
    val density = LocalDensity.current
    val sheetStyle = surfaces.sheet

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = surfaces.scrim,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        val view = LocalView.current
        var windowBlurReady by remember { mutableStateOf(false) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && surfaces.blurEnabled && !reduced && sheetStyle.blurs) {
            val blurPx = with(density) { sheetStyle.blurSigma.dp.toPx().roundToInt() }
            SideEffect {
                val dialogWindow = (view.parent as? DialogWindowProvider)?.window
                val wm = view.context.getSystemService(WindowManager::class.java)
                val crossBlurSupported = wm?.isCrossWindowBlurEnabled == true
                if (dialogWindow != null && crossBlurSupported) {
                    dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                    dialogWindow.attributes = dialogWindow.attributes.apply {
                        blurBehindRadius = blurPx
                    }
                    runCatching { dialogWindow.setBackgroundBlurRadius(blurPx) }
                    windowBlurReady = true
                } else {
                    windowBlurReady = false
                }
            }
        }

        CompositionLocalProvider(LocalWindowBlurActive provides windowBlurReady) {
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
                        SheetHeader(title = title)
                    }
                    content()
                }
            }
        }
    }
}

/**
 * Mist Glass confirmation sheet replacing stock Material AlertDialogs for destructive actions.
 */
@Composable
fun DshConfirmSheet(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = true,
    cancelLabel: String = "Cancel"
) {
    val dsh = ClassRecorderTheme.dshColors
    AppSheet(
        onDismiss = onDismiss,
        title = title
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpace.sheetGutter),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = dsh.textTertiary
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DshSecondaryButton(
                    label = cancelLabel,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                DshPrimaryButton(
                    label = confirmLabel,
                    danger = danger,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}

@Composable
private fun SheetDragHandle(onDismiss: () -> Unit) {
    val handleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = "Close sheet",
                onClick = onDismiss
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
private fun SheetHeader(title: String) {
    Row(
        modifier = Modifier
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
    }
}

/**
 * Grouped card container inside an [AppSheet] (20dp horizontal sheet gutter,
 * Card-tier glass with 20dp corners).
 */
@Composable
fun SheetGroup(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val dsh = ClassRecorderTheme.dshColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AppSpace.sheetGutter, vertical = 6.dp)
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
