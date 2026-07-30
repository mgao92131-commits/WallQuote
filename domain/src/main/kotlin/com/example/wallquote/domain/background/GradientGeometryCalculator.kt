package com.example.wallquote.domain.background

import kotlin.math.cos
import kotlin.math.sin

data class GradientGeometry(
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
)

/**
 * Unified gradient axis for Compose and Canvas.
 *
 * Angle convention (clockwise from left→right):
 * - 0°   left → right
 * - 90°  top → bottom
 * - 180° right → left
 * - 270° bottom → top
 */
object GradientGeometryCalculator {

    fun calculate(
        width: Float,
        height: Float,
        angleDegrees: Float,
    ): GradientGeometry {
        if (width <= 0f || height <= 0f) {
            return GradientGeometry(0f, 0f, 0f, 0f)
        }
        val radians = Math.toRadians(BackgroundValidation.normalizeAngle(angleDegrees).toDouble())
        // Clockwise from +X; screen Y grows downward, so positive sin goes downward.
        val dx = cos(radians).toFloat()
        val dy = sin(radians).toFloat()

        val cx = width / 2f
        val cy = height / 2f
        // Extend to cover the full rect (half-diagonal projected onto axis).
        val halfSpan = (kotlin.math.abs(width * dx) + kotlin.math.abs(height * dy)) / 2f

        return GradientGeometry(
            startX = cx - dx * halfSpan,
            startY = cy - dy * halfSpan,
            endX = cx + dx * halfSpan,
            endY = cy + dy * halfSpan,
        )
    }
}
