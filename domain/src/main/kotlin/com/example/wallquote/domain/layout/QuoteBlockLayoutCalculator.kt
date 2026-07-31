package com.example.wallquote.domain.layout

import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.style.TextStyleNormalizer
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

data class MeasuredQuoteText(
    val widthPx: Float,
    val heightPx: Float,
)

data class QuoteBlockLayout(
    val textLayoutWidthPx: Int,
    val textWidthPx: Float,
    val textHeightPx: Float,
    val blockLeftPx: Float,
    val blockTopPx: Float,
    val blockRightPx: Float,
    val blockBottomPx: Float,
    val pivotXPx: Float,
    val pivotYPx: Float,
    val rotationDegrees: Float,
)

object QuoteBlockLayoutCalculator {
    private const val MAX_TEXT_WIDTH_FRACTION = 0.84f
    private const val MIN_VISIBLE_FRACTION = 0.70f

    fun calculate(
        surfaceWidth: Int,
        surfaceHeight: Int,
        transform: QuoteTransform,
        measuredText: MeasuredQuoteText,
        style: TextStyleConfig,
        density: Float,
    ): QuoteBlockLayout {
        val normalized = TextStyleNormalizer.normalize(style)
        val maxTextWidth = max(1, (surfaceWidth * MAX_TEXT_WIDTH_FRACTION).toInt())
        val textW = measuredText.widthPx.coerceAtMost(maxTextWidth.toFloat())
        val textH = measuredText.heightPx.coerceAtLeast(0f)
        val paddingPx = normalized.blockPaddingDp * density
        val borderPx = if (normalized.blockBorderWidthDp > 0f &&
            normalized.blockBorderColorHex != null &&
            normalized.blockBorderAlpha > 0f
        ) {
            normalized.blockBorderWidthDp * density
        } else {
            0f
        }
        val halfW = textW / 2f + paddingPx + borderPx
        val halfH = textH / 2f + paddingPx + borderPx
        val pivotX = surfaceWidth * transform.centerXFraction.coerceIn(0f, 1f)
        val pivotY = surfaceHeight * transform.centerYFraction.coerceIn(0f, 1f)
        return QuoteBlockLayout(
            textLayoutWidthPx = maxTextWidth,
            textWidthPx = textW,
            textHeightPx = textH,
            blockLeftPx = pivotX - halfW,
            blockTopPx = pivotY - halfH,
            blockRightPx = pivotX + halfW,
            blockBottomPx = pivotY + halfH,
            pivotXPx = pivotX,
            pivotYPx = pivotY,
            rotationDegrees = transform.rotationDegrees,
        )
    }

    /**
     * Clamp transform so at least [MIN_VISIBLE_FRACTION] of the block's rotated axis-aligned
     * bounding box stays on-screen (P4-013). A rotated rectangle of width [blockW] and height
     * [blockH] occupies a larger AABB footprint than the unrotated rectangle:
     *
     * ```
     * rotatedW = blockW * |cos(theta)| + blockH * |sin(theta)|
     * rotatedH = blockW * |sin(theta)| + blockH * |cos(theta)|
     * ```
     *
     * Oversized blocks remain centered.
     */
    fun clampTransform(
        surfaceWidth: Int,
        surfaceHeight: Int,
        transform: QuoteTransform,
        measuredText: MeasuredQuoteText,
        style: TextStyleConfig,
        density: Float,
    ): QuoteTransform {
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return transform
        val layout = calculate(surfaceWidth, surfaceHeight, transform, measuredText, style, density)
        val blockW = layout.blockRightPx - layout.blockLeftPx
        val blockH = layout.blockBottomPx - layout.blockTopPx
        val radians = Math.toRadians(transform.rotationDegrees.toDouble())
        val absCos = kotlin.math.abs(cos(radians)).toFloat()
        val absSin = kotlin.math.abs(sin(radians)).toFloat()
        val rotatedW = blockW * absCos + blockH * absSin
        val rotatedH = blockW * absSin + blockH * absCos
        if (rotatedW >= surfaceWidth || rotatedH >= surfaceHeight) {
            return transform.copy(centerXFraction = 0.5f, centerYFraction = 0.5f)
        }
        val minVisibleW = rotatedW * MIN_VISIBLE_FRACTION
        val minVisibleH = rotatedH * MIN_VISIBLE_FRACTION
        val minCenterX = (minVisibleW - rotatedW / 2f) / surfaceWidth
        val maxCenterX = (surfaceWidth - (minVisibleW - rotatedW / 2f)) / surfaceWidth
        val minCenterY = (minVisibleH - rotatedH / 2f) / surfaceHeight
        val maxCenterY = (surfaceHeight - (minVisibleH - rotatedH / 2f)) / surfaceHeight
        return transform.copy(
            centerXFraction = transform.centerXFraction.coerceIn(
                min(minCenterX, maxCenterX),
                max(minCenterX, maxCenterX),
            ),
            centerYFraction = transform.centerYFraction.coerceIn(
                min(minCenterY, maxCenterY),
                max(minCenterY, maxCenterY),
            ),
        )
    }

    fun shadowOffsetPx(style: TextStyleConfig, density: Float): Pair<Float, Float> {
        val n = TextStyleNormalizer.normalize(style)
        val distancePx = n.shadowDistanceDp * density
        val radians = Math.toRadians(n.shadowAngleDegrees.toDouble())
        val dx = cos(radians).toFloat() * distancePx
        val dy = sin(radians).toFloat() * distancePx
        return dx to dy
    }
}
