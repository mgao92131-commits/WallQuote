package com.example.wallquote.ui.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.wallquote.domain.time.CircularTimeGeometry
import com.example.wallquote.domain.time.TimeDragHandle
import com.example.wallquote.domain.time.halfHourIndexFromMinute
import com.example.wallquote.ui.theme.WallQuoteColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 24h circular dial: 00:00 at the top, time increases clockwise. Dragging near the start or end
 * handle moves it to the nearest half-hour slot.
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
    var lastSelectedHandle by remember { mutableStateOf(TimeDragHandle.Start) }

    val trackColor = WallQuoteColors.SurfaceRaised
    val activeColor = WallQuoteColors.Beige
    val startHandleColor = WallQuoteColors.HandleStart
    val endHandleColor = WallQuoteColors.HandleEnd

    Canvas(
        modifier = modifier
            .size(240.dp)
            .pointerInput(startSlot, endSlot) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val touchSlot = CircularTimeGeometry.angleToNearestSlot(
                            angleFromCenter(offset, center),
                        )
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
                    val slot = CircularTimeGeometry.angleToNearestSlot(
                        angleFromCenter(change.position, center),
                    )
                    when (activeDrag) {
                        TimeDragHandle.Start -> onStartSlotChange(slot)
                        TimeDragHandle.End -> onEndSlotChange(slot)
                        null -> Unit
                    }
                }
            },
    ) {
        val strokeWidthPx = 16.dp.toPx()
        val radius = (min(size.width, size.height) - strokeWidthPx * 2.4f) / 2f
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

        val labelRadius = radius + 22.dp.toPx()
        val labelPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(180, 244, 240, 232)
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 12.dp.toPx()
            isAntiAlias = true
        }
        listOf(0 to 0f, 6 to 90f, 12 to 180f, 18 to 270f).forEach { (label, angle) ->
            val radians = Math.toRadians(angle.toDouble())
            val x = center.x + labelRadius * sin(radians).toFloat()
            val y = center.y - labelRadius * cos(radians).toFloat() + 4.dp.toPx()
            drawContext.canvas.nativeCanvas.drawText(label.toString(), x, y, labelPaint)
        }

        drawLabeledHandle(center, radius, CircularTimeGeometry.slotToAngle(startSlot), startHandleColor, "S")
        drawLabeledHandle(center, radius, CircularTimeGeometry.slotToAngle(endSlot), endHandleColor, "E")
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLabeledHandle(
    center: Offset,
    radius: Float,
    angleDegrees: Float,
    color: Color,
    label: String,
) {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val handleCenter = Offset(
        x = center.x + radius * sin(radians).toFloat(),
        y = center.y - radius * cos(radians).toFloat(),
    )
    drawCircle(color = color.copy(alpha = 0.28f), radius = 18.dp.toPx(), center = handleCenter)
    drawCircle(color = color, radius = 9.dp.toPx(), center = handleCenter)
    val paint = android.graphics.Paint().apply {
        this.color = android.graphics.Color.WHITE
        textAlign = android.graphics.Paint.Align.CENTER
        textSize = 11.dp.toPx()
        isFakeBoldText = true
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(
        label,
        handleCenter.x,
        handleCenter.y + 4.dp.toPx(),
        paint,
    )
}

private fun angleFromCenter(point: Offset, center: Offset): Float {
    val dx = point.x - center.x
    val dy = point.y - center.y
    val mathDegrees = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    var angle = mathDegrees + 90f
    if (angle < 0f) angle += 360f
    return angle % 360f
}
