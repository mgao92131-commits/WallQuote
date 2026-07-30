package com.example.wallquote.domain.layout

import com.example.wallquote.domain.model.QuoteTransform
import org.junit.Assert.assertEquals
import org.junit.Test

class QuoteLayoutCalculatorTest {

    @Test
    fun centerIsHalfSurface() {
        val layout = QuoteLayoutCalculator.calculate(
            surfaceWidth = 1000,
            surfaceHeight = 2000,
            transform = QuoteTransform(0.5f, 0.5f, 0f),
        )
        assertEquals(840, layout.maxTextWidth)
        assertEquals(500f, layout.centerX)
        assertEquals(1000f, layout.centerY)
    }

    @Test
    fun clampsFractions() {
        val layout = QuoteLayoutCalculator.calculate(
            surfaceWidth = 100,
            surfaceHeight = 100,
            transform = QuoteTransform(2f, -1f, 15f),
        )
        assertEquals(100f, layout.centerX)
        assertEquals(0f, layout.centerY)
        assertEquals(15f, layout.rotationDegrees)
    }
}
