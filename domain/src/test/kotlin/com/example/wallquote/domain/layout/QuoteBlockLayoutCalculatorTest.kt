package com.example.wallquote.domain.layout

import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteBlockLayoutCalculatorTest {

    @Test
    fun centeredTransform_pivotsAtSurfaceCenter() {
        val layout = QuoteBlockLayoutCalculator.calculate(
            surfaceWidth = 1000,
            surfaceHeight = 2000,
            transform = QuoteTransform(0.5f, 0.5f, 0f),
            measuredText = MeasuredQuoteText(widthPx = 400f, heightPx = 100f),
            style = TextStyleConfig(),
            density = 2f,
        )
        assertEquals(500f, layout.pivotXPx, 0.001f)
        assertEquals(1000f, layout.pivotYPx, 0.001f)
        assertEquals(0f, layout.rotationDegrees, 0.001f)
    }

    @Test
    fun blockPaddingAndBorderExpandBounds() {
        val noBlock = QuoteBlockLayoutCalculator.calculate(
            surfaceWidth = 1000,
            surfaceHeight = 2000,
            transform = QuoteTransform(0.5f, 0.5f, 0f),
            measuredText = MeasuredQuoteText(widthPx = 400f, heightPx = 100f),
            style = TextStyleConfig(),
            density = 2f,
        )
        val withBlock = QuoteBlockLayoutCalculator.calculate(
            surfaceWidth = 1000,
            surfaceHeight = 2000,
            transform = QuoteTransform(0.5f, 0.5f, 0f),
            measuredText = MeasuredQuoteText(widthPx = 400f, heightPx = 100f),
            style = TextStyleConfig(
                blockColorHex = "#000000",
                blockAlpha = 0.5f,
                blockPaddingDp = 16f,
                blockBorderWidthDp = 4f,
                blockBorderColorHex = "#FFFFFF",
                blockBorderAlpha = 1f,
            ),
            density = 2f,
        )
        val noBlockWidth = noBlock.blockRightPx - noBlock.blockLeftPx
        val withBlockWidth = withBlock.blockRightPx - withBlock.blockLeftPx
        assertTrue(withBlockWidth > noBlockWidth)
    }

    @Test
    fun textWiderThanMaxIsClamped() {
        val layout = QuoteBlockLayoutCalculator.calculate(
            surfaceWidth = 1000,
            surfaceHeight = 2000,
            transform = QuoteTransform(0.5f, 0.5f, 0f),
            measuredText = MeasuredQuoteText(widthPx = 5000f, heightPx = 100f),
            style = TextStyleConfig(),
            density = 2f,
        )
        assertEquals(840, layout.textLayoutWidthPx)
        assertEquals(840f, layout.textWidthPx, 0.001f)
    }

    @Test
    fun shadowOffset_pointsInAngleDirection() {
        val (dx, dy) = QuoteBlockLayoutCalculator.shadowOffsetPx(
            TextStyleConfig(shadowDistanceDp = 10f, shadowAngleDegrees = 0f),
            density = 1f,
        )
        assertEquals(10f, dx, 0.01f)
        assertEquals(0f, dy, 0.01f)

        val (dx90, dy90) = QuoteBlockLayoutCalculator.shadowOffsetPx(
            TextStyleConfig(shadowDistanceDp = 10f, shadowAngleDegrees = 90f),
            density = 1f,
        )
        assertEquals(0f, dx90, 0.01f)
        assertEquals(10f, dy90, 0.01f)
    }

    @Test
    fun clampTransform_recentersOversizedBlock() {
        val transform = QuoteTransform(0.9f, 0.9f, 0f)
        val clamped = QuoteBlockLayoutCalculator.clampTransform(
            surfaceWidth = 100,
            surfaceHeight = 100,
            transform = transform,
            measuredText = MeasuredQuoteText(widthPx = 200f, heightPx = 200f),
            style = TextStyleConfig(),
            density = 1f,
        )
        assertEquals(0.5f, clamped.centerXFraction, 0.001f)
        assertEquals(0.5f, clamped.centerYFraction, 0.001f)
    }

    @Test
    fun clampTransform_keepsWellWithinBoundsUnchanged() {
        val transform = QuoteTransform(0.5f, 0.5f, 12f)
        val clamped = QuoteBlockLayoutCalculator.clampTransform(
            surfaceWidth = 1000,
            surfaceHeight = 1000,
            transform = transform,
            measuredText = MeasuredQuoteText(widthPx = 100f, heightPx = 50f),
            style = TextStyleConfig(),
            density = 1f,
        )
        assertEquals(0.5f, clamped.centerXFraction, 0.001f)
        assertEquals(0.5f, clamped.centerYFraction, 0.001f)
        assertEquals(12f, clamped.rotationDegrees, 0.001f)
    }

    @Test
    fun clampTransform_at0Degrees_usesUnrotatedFootprint() {
        // blockW = 200, blockH = 50 (no padding/border in default style).
        val clamped = QuoteBlockLayoutCalculator.clampTransform(
            surfaceWidth = 1000,
            surfaceHeight = 1000,
            transform = QuoteTransform(0.995f, 0.995f, 0f),
            measuredText = MeasuredQuoteText(widthPx = 200f, heightPx = 50f),
            style = TextStyleConfig(),
            density = 1f,
        )
        assertEquals(0.96f, clamped.centerXFraction, 0.001f)
        assertEquals(0.99f, clamped.centerYFraction, 0.001f)
    }

    @Test
    fun clampTransform_at90Degrees_swapsFootprintAxes() {
        // Same block as the 0deg case, but rotated 90deg: width/height footprint swap.
        val clamped = QuoteBlockLayoutCalculator.clampTransform(
            surfaceWidth = 1000,
            surfaceHeight = 1000,
            transform = QuoteTransform(0.995f, 0.995f, 90f),
            measuredText = MeasuredQuoteText(widthPx = 200f, heightPx = 50f),
            style = TextStyleConfig(),
            density = 1f,
        )
        assertEquals(0.99f, clamped.centerXFraction, 0.001f)
        assertEquals(0.96f, clamped.centerYFraction, 0.001f)
    }

    @Test
    fun clampTransform_at45Degrees_expandsBoundingBoxBeyondEitherAxis() {
        // A square block (140x140) fits unrotated in a 150x150 surface, but its 45deg rotated
        // AABB (140 * sqrt(2) ~= 198) does not, so it must fall back to recentering.
        val unrotated = QuoteBlockLayoutCalculator.clampTransform(
            surfaceWidth = 150,
            surfaceHeight = 150,
            transform = QuoteTransform(0.9f, 0.9f, 0f),
            measuredText = MeasuredQuoteText(widthPx = 140f, heightPx = 140f),
            style = TextStyleConfig(),
            density = 1f,
        )
        assertTrue(unrotated.centerXFraction > 0.5f)

        val rotated45 = QuoteBlockLayoutCalculator.clampTransform(
            surfaceWidth = 150,
            surfaceHeight = 150,
            transform = QuoteTransform(0.9f, 0.9f, 45f),
            measuredText = MeasuredQuoteText(widthPx = 140f, heightPx = 140f),
            style = TextStyleConfig(),
            density = 1f,
        )
        assertEquals(0.5f, rotated45.centerXFraction, 0.001f)
        assertEquals(0.5f, rotated45.centerYFraction, 0.001f)
    }
}
