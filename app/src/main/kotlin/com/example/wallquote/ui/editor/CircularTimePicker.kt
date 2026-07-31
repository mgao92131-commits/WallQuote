package com.example.wallquote.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.wallquote.domain.time.CircularTimeGeometry
import com.example.wallquote.domain.time.HALF_HOUR_SLOTS
import com.example.wallquote.domain.time.TimeDragHandle
import com.example.wallquote.domain.time.halfHourIndexFromMinute
import com.example.wallquote.ui.util.formatMinuteOfDay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 24h circular dial: 00:00 at the top, time increases clockwise. Dragging near the start or end
 * handle moves it to the nearest half-hour slot. Stepper buttons and text below remain for users
 * who cannot perform the drag gesture.
 */
@Composable
fun CircularTimePicker(
    startMinute: Int,
    endMinute: Int,
    onStartSlotChange: (Int) -> Unit,
    onEndSlotChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val startSlot = halfHourIndexFromMinute(startMinute)
    val endSlot = halfHourIndexFromMinute(endMinute)
    val isAllDay = startSlot == endSlot
    var activeDrag by remember { mutableStateOf<TimeDragHandle?>(null) }
    // P4-016: remembers which handle was last picked so overlapping/equidistant touches (e.g.
    // the "all day" state where both handles sit on top of each other) alternate between Start
    // and End instead of always resolving to the same one.
    var lastSelectedHandle by remember { mutableStateOf(TimeDragHandle.Start) }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = MaterialTheme.colorScheme.primary
    val startHandleColor = MaterialTheme.colorScheme.primary
    val endHandleColor = MaterialTheme.colorScheme.tertiary

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .size(220.dp)
                .pointerInput(startSlot, endSlot) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val touchSlot = CircularTimeGeometry.angleToNearestSlot(angleFromCenter(offset, center))
                            val handle = CircularTimeGeometry.resolveDragHandle(
                                touchSlot = touchSlot,
                                startSlot = startSlot,
                                endSlot = endSlot,
                                lastSelected = lastSelectedHandle,
                            )
                            activeDrag = handle
                            lastSelectedHandle = handle
                        },
                        onDragEnd = { activeDrag = null },
                        onDragCancel = { activeDrag = null },
                    ) { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val slot = CircularTimeGeometry.angleToNearestSlot(angleFromCenter(change.position, center))
                        when (activeDrag) {
                            TimeDragHandle.Start -> onStartSlotChange(slot)
                            TimeDragHandle.End -> onEndSlotChange(slot)
                            null -> Unit
                        }
                    }
                },
        ) {
            val strokeWidthPx = 16.dp.toPx()
            val radius = (min(size.width, size.height) - strokeWidthPx) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )

            val arc = CircularTimeGeometry.buildActiveArc(startSlot, endSlot)
            drawArc(
                color = activeColor.copy(alpha = if (isAllDay) 0.35f else 0.85f),
                startAngle = arc.startAngleDegrees - 90f,
                sweepAngle = arc.sweepDegrees,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = androidx.compose.ui.graphics.StrokeCap.Round),
            )

            drawHandle(center, radius, CircularTimeGeometry.slotToAngle(startSlot), startHandleColor)
            drawHandle(center, radius, CircularTimeGeometry.slotToAngle(endSlot), endHandleColor)
        }

        Text(
            text = if (isAllDay) "全天有效" else "${formatMinuteOfDay(startMinute)} – ${formatMinuteOfDay(endMinute)}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            TimeStepper(
                label = "开始",
                minute = startMinute,
                onDecrement = { onStartSlotChange(wrapSlot(startSlot - 1)) },
                onIncrement = { onStartSlotChange(wrapSlot(startSlot + 1)) },
            )
            TimeStepper(
                label = "结束",
                minute = endMinute,
                onDecrement = { onEndSlotChange(wrapSlot(endSlot - 1)) },
                onIncrement = { onEndSlotChange(wrapSlot(endSlot + 1)) },
            )
        }

        TextButton(onClick = { onStartSlotChange(0); onEndSlotChange(0) }) {
            Text(if (isAllDay) "已设为全天" else "设为全天")
        }
        Text(
            text = "起止相同表示全天有效。步长 30 分钟（00:00–23:30）。",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TimeStepper(
    label: String,
    minute: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onDecrement) {
                Text("−", style = MaterialTheme.typography.titleMedium)
            }
            Text(formatMinuteOfDay(minute), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onIncrement) {
                Text("+", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHandle(
    center: Offset,
    radius: Float,
    angleDegrees: Float,
    color: Color,
) {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val handleCenter = Offset(
        x = center.x + radius * sin(radians).toFloat(),
        y = center.y - radius * cos(radians).toFloat(),
    )
    drawCircle(color = Color.White, radius = 12.dp.toPx(), center = handleCenter)
    drawCircle(color = color, radius = 8.dp.toPx(), center = handleCenter)
}

/** Angle in degrees clockwise from the top (12 o'clock), matching [CircularTimeGeometry]. */
private fun angleFromCenter(point: Offset, center: Offset): Float {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val mathDegrees = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    var angle = mathDegrees + 90f
    if (angle < 0f) angle += 360f
    return angle % 360f
}

private fun wrapSlot(slot: Int): Int = ((slot % HALF_HOUR_SLOTS) + HALF_HOUR_SLOTS) % HALF_HOUR_SLOTS
