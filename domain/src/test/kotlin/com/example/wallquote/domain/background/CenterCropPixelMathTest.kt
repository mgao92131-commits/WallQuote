package com.example.wallquote.domain.background

import com.example.wallquote.domain.background.CenterCropCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Deterministic crop math assertions (not Robolectric pixels).
 */
class CenterCropPixelMathTest {

    @Test
    fun landscapeIntoSquare_sourceRectIsCentered() {
        val crop = CenterCropCalculator.calculate(200, 100, 100, 100)
        assertNotNull(crop)
        assertEquals(50, crop!!.srcLeft.roundToInt())
        assertEquals(150, crop.srcRight.roundToInt())
        assertEquals(0, crop.srcTop.roundToInt())
        assertEquals(100, crop.srcBottom.roundToInt())
    }
}
