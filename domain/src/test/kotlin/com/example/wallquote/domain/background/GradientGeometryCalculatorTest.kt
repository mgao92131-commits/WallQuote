package com.example.wallquote.domain.background

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class GradientGeometryCalculatorTest {

    @Test
    fun zeroDegrees_leftToRight() {
        val g = GradientGeometryCalculator.calculate(200f, 100f, 0f)
        assertTrue(g.startX < g.endX)
        assertEquals(50f, g.startY, 0.01f)
        assertEquals(50f, g.endY, 0.01f)
    }

    @Test
    fun ninetyDegrees_topToBottom() {
        val g = GradientGeometryCalculator.calculate(200f, 100f, 90f)
        assertEquals(100f, g.startX, 0.01f)
        assertEquals(100f, g.endX, 0.01f)
        assertTrue(g.startY < g.endY)
    }

    @Test
    fun oneEighty_rightToLeft() {
        val g = GradientGeometryCalculator.calculate(200f, 100f, 180f)
        assertTrue(g.startX > g.endX)
        assertEquals(50f, g.startY, 0.01f)
    }

    @Test
    fun twoSeventy_bottomToTop() {
        val g = GradientGeometryCalculator.calculate(200f, 100f, 270f)
        assertEquals(100f, g.startX, 0.01f)
        assertTrue(g.startY > g.endY)
    }

    @Test
    fun fortyFive_coversNonSquare() {
        val g = GradientGeometryCalculator.calculate(400f, 100f, 45f)
        assertTrue(abs(g.endX - g.startX) > 0f)
        assertTrue(abs(g.endY - g.startY) > 0f)
    }

    @Test
    fun negativeAndOver360_normalize() {
        val a = GradientGeometryCalculator.calculate(100f, 100f, -90f)
        val b = GradientGeometryCalculator.calculate(100f, 100f, 270f)
        assertEquals(a.startX, b.startX, 0.01f)
        assertEquals(a.startY, b.startY, 0.01f)
        assertEquals(a.endX, b.endX, 0.01f)
        assertEquals(a.endY, b.endY, 0.01f)
    }

    @Test
    fun zeroSize_returnsZeros() {
        val g = GradientGeometryCalculator.calculate(0f, 100f, 0f)
        assertEquals(0f, g.startX, 0f)
        assertEquals(0f, g.endX, 0f)
    }
}
