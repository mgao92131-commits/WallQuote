package com.example.wallquote.domain.layout

import com.example.wallquote.domain.model.QuoteTransform
import kotlin.math.roundToInt

data class TextMetrics(
    val layoutWidth: Float,
    val layoutHeight: Float,
)

data class QuoteLayout(
    val maxTextWidth: Int,
    val centerX: Float,
    val centerY: Float,
    val rotationDegrees: Float,
)

/**
 * Shared placement rules for Compose preview and Canvas wallpaper.
 */
object QuoteLayoutCalculator {

    const val MAX_TEXT_WIDTH_FRACTION = 0.84f

    fun calculate(
        surfaceWidth: Int,
        surfaceHeight: Int,
        transform: QuoteTransform,
        textMetrics: TextMetrics? = null,
    ): QuoteLayout {
        val safeWidth = surfaceWidth.coerceAtLeast(0)
        val safeHeight = surfaceHeight.coerceAtLeast(0)
        val maxTextWidth = (safeWidth * MAX_TEXT_WIDTH_FRACTION).roundToInt().coerceAtLeast(0)
        val xFraction = transform.centerXFraction.coerceIn(0f, 1f)
        val yFraction = transform.centerYFraction.coerceIn(0f, 1f)
        return QuoteLayout(
            maxTextWidth = maxTextWidth,
            centerX = safeWidth * xFraction,
            centerY = safeHeight * yFraction,
            rotationDegrees = transform.rotationDegrees,
        ).also {
            // textMetrics reserved for future padding around layout bounds.
            @Suppress("UNUSED_EXPRESSION")
            textMetrics
        }
    }
}
