package com.example.wallquote.domain.layout

import com.example.wallquote.domain.model.QuoteTransform
import org.junit.Assert.assertEquals
import org.junit.Test

class QuoteGestureTransformerTest {

    @Test
    fun rotationDelta_plusFiveDegrees_addsDirectly() {
        val result = QuoteGestureTransformer.apply(
            current = QuoteTransform(0.5f, 0.5f, 10f),
            panX = 0f,
            panY = 0f,
            viewportWidth = 1000,
            viewportHeight = 1000,
            rotationDeltaDegrees = 5f,
        )
        assertEquals(15f, result.rotationDegrees, 0.001f)
    }

    @Test
    fun rotationDelta_minusFiveDegrees_subtractsDirectly() {
        val result = QuoteGestureTransformer.apply(
            current = QuoteTransform(0.5f, 0.5f, 10f),
            panX = 0f,
            panY = 0f,
            viewportWidth = 1000,
            viewportHeight = 1000,
            rotationDeltaDegrees = -5f,
        )
        assertEquals(5f, result.rotationDegrees, 0.001f)
    }

    @Test
    fun rotationDelta_wrapsAcross360() {
        val result = QuoteGestureTransformer.apply(
            current = QuoteTransform(0.5f, 0.5f, 350f),
            panX = 0f,
            panY = 0f,
            viewportWidth = 1000,
            viewportHeight = 1000,
            rotationDeltaDegrees = 20f,
        )
        assertEquals(10f, result.rotationDegrees, 0.001f)
    }

    @Test
    fun pan_isConvertedToNormalizedFractionDelta() {
        val result = QuoteGestureTransformer.apply(
            current = QuoteTransform(0.5f, 0.5f, 0f),
            panX = 100f,
            panY = -50f,
            viewportWidth = 1000,
            viewportHeight = 500,
            rotationDeltaDegrees = 0f,
        )
        assertEquals(0.6f, result.centerXFraction, 0.001f)
        assertEquals(0.4f, result.centerYFraction, 0.001f)
    }

    @Test
    fun panAndRotation_applySimultaneously() {
        val result = QuoteGestureTransformer.apply(
            current = QuoteTransform(0.5f, 0.5f, 0f),
            panX = 50f,
            panY = 50f,
            viewportWidth = 1000,
            viewportHeight = 1000,
            rotationDeltaDegrees = 5f,
        )
        assertEquals(0.55f, result.centerXFraction, 0.001f)
        assertEquals(0.55f, result.centerYFraction, 0.001f)
        assertEquals(5f, result.rotationDegrees, 0.001f)
    }

    @Test
    fun zeroViewport_returnsCurrentUnchanged() {
        val current = QuoteTransform(0.4f, 0.3f, 20f)
        val result = QuoteGestureTransformer.apply(
            current = current,
            panX = 100f,
            panY = 100f,
            viewportWidth = 0,
            viewportHeight = 0,
            rotationDeltaDegrees = 15f,
        )
        assertEquals(current, result)
    }
}
