package com.example.wallquote.domain.background

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CenterCropCalculatorTest {

    @Test
    fun landscapeIntoPortrait_cropsSides() {
        val crop = CenterCropCalculator.calculate(
            sourceWidth = 200,
            sourceHeight = 100,
            targetWidth = 50,
            targetHeight = 100,
        )
        assertNotNull(crop)
        assertTrue(crop!!.srcLeft > 0f)
        assertTrue(crop.srcRight < 200f)
        assertEquals(0f, crop.srcTop, 0.01f)
        assertEquals(100f, crop.srcBottom, 0.01f)
    }

    @Test
    fun portraitIntoLandscape_cropsTopBottom() {
        val crop = CenterCropCalculator.calculate(
            sourceWidth = 100,
            sourceHeight = 200,
            targetWidth = 100,
            targetHeight = 50,
        )
        assertNotNull(crop)
        assertEquals(0f, crop!!.srcLeft, 0.01f)
        assertEquals(100f, crop.srcRight, 0.01f)
        assertTrue(crop.srcTop > 0f)
        assertTrue(crop.srcBottom < 200f)
    }

    @Test
    fun sameAspect_usesFullSource() {
        val crop = CenterCropCalculator.calculate(100, 100, 50, 50)
        assertNotNull(crop)
        assertEquals(0f, crop!!.srcLeft, 0.01f)
        assertEquals(0f, crop.srcTop, 0.01f)
        assertEquals(100f, crop.srcRight, 0.01f)
        assertEquals(100f, crop.srcBottom, 0.01f)
    }

    @Test
    fun ultraWide_cropsHeavily() {
        val crop = CenterCropCalculator.calculate(1000, 100, 100, 100)
        assertNotNull(crop)
        assertTrue(crop!!.srcLeft > 100f)
        assertTrue(crop.srcRight < 900f)
    }

    @Test
    fun ultraTall_cropsHeavily() {
        val crop = CenterCropCalculator.calculate(100, 1000, 100, 100)
        assertNotNull(crop)
        assertTrue(crop!!.srcTop > 100f)
        assertTrue(crop.srcBottom < 900f)
    }

    @Test
    fun zeroSize_returnsNull() {
        assertNull(CenterCropCalculator.calculate(0, 100, 50, 50))
        assertNull(CenterCropCalculator.calculate(100, 100, 0, 50))
    }
}
