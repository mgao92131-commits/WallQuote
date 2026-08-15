package com.example.wallquote.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.time.ScheduleTimelineGeometry

/** Renders a 24h horizontal bar highlighting the collection's active window(s) and the current time. */
@Composable
fun ScheduleTimelineBar(
    config: CollectionConfig,
    nowMinuteOfDay: Int,
    modifier: Modifier = Modifier,
) {
    val result = ScheduleTimelineGeometry.compute(config.schedule, nowMinuteOfDay)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val activeColor = if (result.isActiveNow) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    }
    val nowColor = MaterialTheme.colorScheme.onSurface
    Canvas(modifier = modifier.background(trackColor, CircleShape)) {
        val corner = CornerRadius(size.height / 2f, size.height / 2f)
        result.segments.forEach { segment ->
            val left = segment.startFraction * size.width
            val right = segment.endFraction * size.width
            if (right > left) {
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(left, 0f),
                    size = Size(right - left, size.height),
                    cornerRadius = corner,
                )
            }
        }
        val nowX = (result.nowFraction * size.width).coerceIn(1f, size.width - 1f)
        drawLine(
            color = nowColor,
            start = Offset(nowX, 0f),
            end = Offset(nowX, size.height),
            strokeWidth = 2f,
        )
    }
}
