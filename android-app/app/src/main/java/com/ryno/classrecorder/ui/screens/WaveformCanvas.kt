package com.ryno.classrecorder.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.dp
import com.ryno.classrecorder.ui.theme.ClassRecorderTheme
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Real-time microphone level waveform rendered from a zero-allocation [ringBuffer].
 *
 * [frameTick] is read inside the draw scope so Compose invalidates ONLY the draw phase
 * at 30 fps without recomposing parent layouts.
 */
@Composable
fun LiveWaveformCanvas(
    ringBuffer: FloatArray,
    writeCursor: Int,
    sampleCount: Int,
    frameTick: Int,
    active: Boolean,
    paused: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    idleColor: Color = ClassRecorderTheme.dshColors.textTertiary,
    playheadColor: Color = ClassRecorderTheme.dshColors.danger
) {
    val a11yLabel = when {
        !active -> "Audio waveform idle"
        paused -> "Audio waveform paused"
        else -> "Live audio waveform"
    }
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 96.dp)
            .semantics { contentDescription = a11yLabel }
    ) {
        // Read frameTick inside DrawScope so updates only invalidate draw, never composition/layout
        val unusedTick = frameTick
        if (unusedTick < 0 || size.width <= 0f || size.height <= 0f) return@Canvas

        val barWidth = 3.dp.toPx()
        val gap = 3.dp.toPx()
        val stride = barWidth + gap
        val slotCount = max(12, (size.width / stride).toInt())
        val centerY = size.height * 0.5f
        val minBarH = 4.dp.toPx()
        val maxBarH = (size.height * 0.86f).coerceAtLeast(minBarH)
        val corner = CornerRadius(barWidth * 0.5f, barWidth * 0.5f)

        val totalWidth = slotCount * stride - gap
        val startX = ((size.width - totalWidth) * 0.5f).coerceAtLeast(0f)
        val cap = ringBuffer.size

        for (i in 0 until slotCount) {
            val offsetFromNewest = slotCount - 1 - i
            val hasSample = active && offsetFromNewest < sampleCount && cap > 0
            val sample = if (hasSample) {
                val ringIdx = ((writeCursor - 1 - offsetFromNewest) % cap + cap) % cap
                ringBuffer[ringIdx].coerceIn(0f, 1f)
            } else {
                0f
            }
            val height = if (!active) minBarH else max(minBarH, sample * maxBarH)
            val recency = (i + 1).toFloat() / slotCount.toFloat()
            val barColor = when {
                !active -> idleColor.copy(alpha = 0.78f)
                paused -> activeColor.copy(alpha = 0.55f + 0.30f * recency)
                !hasSample -> idleColor.copy(alpha = 0.68f)
                else -> activeColor.copy(alpha = 0.62f + 0.38f * recency)
            }
            val x = startX + i * stride
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, centerY - height * 0.5f),
                size = Size(barWidth, height),
                cornerRadius = corner
            )
        }

        if (active) {
            val headX = (startX + totalWidth).coerceAtMost(size.width - 1.dp.toPx())
            drawLine(
                color = if (paused) idleColor else playheadColor.copy(alpha = 0.90f),
                start = Offset(headX, size.height * 0.14f),
                end = Offset(headX, size.height * 0.86f),
                strokeWidth = 1.75.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Interactive scrubbable playback waveform canvas.
 */
@Composable
fun ScrubWaveformCanvas(
    peaks: List<Float>,
    progress: Float,
    onScrub: (Float) -> Unit,
    modifier: Modifier = Modifier,
    onScrubFinished: (() -> Unit)? = null,
    playedColor: Color = MaterialTheme.colorScheme.primary,
    unplayedColor: Color = ClassRecorderTheme.dshColors.textTertiary,
    playheadColor: Color = MaterialTheme.colorScheme.onSurface
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(progress) }
    val displayProgress = (if (dragging) dragFraction else progress).coerceIn(0f, 1f)
    // Quantize accessibility announcements to 5% buckets so playback doesn't spam AccessibilityManager at 31 fps
    val quantizedPercent = ((displayProgress * 20f).roundToInt() * 5).coerceIn(0, 100)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .semantics {
                contentDescription = "Playback position $quantizedPercent percent"
                setProgress { target ->
                    val clamped = target.coerceIn(0f, 1f)
                    onScrub(clamped)
                    onScrubFinished?.invoke()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    if (size.width > 0) {
                        val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onScrub(frac)
                        onScrubFinished?.invoke()
                    }
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { start ->
                        if (size.width > 0) {
                            dragging = true
                            val frac = (start.x / size.width.toFloat()).coerceIn(0f, 1f)
                            dragFraction = frac
                            onScrub(frac)
                        }
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        if (size.width > 0) {
                            val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                            dragFraction = frac
                            onScrub(frac)
                        }
                    },
                    onDragEnd = {
                        dragging = false
                        onScrubFinished?.invoke()
                    },
                    onDragCancel = {
                        dragging = false
                        onScrubFinished?.invoke()
                    }
                )
            }
    ) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val barWidth = 3.dp.toPx()
        val gap = 2.5.dp.toPx()
        val stride = barWidth + gap
        val slotCount = max(16, (size.width / stride).toInt())
        val centerY = size.height * 0.5f
        val minBarH = 5.dp.toPx()
        val maxBarH = (size.height * 0.84f).coerceAtLeast(minBarH)
        val corner = CornerRadius(barWidth * 0.5f, barWidth * 0.5f)

        val totalWidth = slotCount * stride - gap
        val startX = ((size.width - totalWidth) * 0.5f).coerceAtLeast(0f)
        val playheadX = startX + displayProgress * totalWidth

        for (i in 0 until slotCount) {
            val h = if (peaks.isEmpty()) {
                minBarH
            } else {
                val srcIdx = ((i.toFloat() / slotCount.toFloat()) * peaks.size)
                    .toInt()
                    .coerceIn(0, peaks.lastIndex)
                max(minBarH, peaks[srcIdx].coerceIn(0f, 1f) * maxBarH)
            }
            val x = startX + i * stride
            val isPlayed = x + barWidth * 0.5f <= playheadX
            drawRoundRect(
                color = if (isPlayed) playedColor else unplayedColor.copy(alpha = 0.82f),
                topLeft = Offset(x, centerY - h * 0.5f),
                size = Size(barWidth, h),
                cornerRadius = corner
            )
        }

        val clampedHeadX = playheadX.coerceIn(1.dp.toPx(), size.width - 1.dp.toPx())
        drawLine(
            color = playheadColor,
            start = Offset(clampedHeadX, size.height * 0.08f),
            end = Offset(clampedHeadX, size.height * 0.92f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = playheadColor,
            radius = 3.5.dp.toPx(),
            center = Offset(clampedHeadX, size.height * 0.08f)
        )
    }
}
