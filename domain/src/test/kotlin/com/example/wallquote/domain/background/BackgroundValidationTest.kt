package com.example.wallquote.domain.background

import com.example.wallquote.domain.model.BackgroundSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundValidationTest {

    @Test
    fun validSolid() {
        assertNull(BackgroundValidation.validate(BackgroundSpec.Solid("#AABBCC")))
    }

    @Test
    fun invalidSolidColor() {
        assertEquals("背景颜色无效", BackgroundValidation.validate(BackgroundSpec.Solid("red")))
    }

    @Test
    fun clampsDimAndBlur() {
        val photo = BackgroundValidation.normalize(
            BackgroundSpec.Photo(
                assetId = "bg_0123456789abcdef0123456789abcdef",
                dimAmount = 2f,
                blurRadiusDp = 99f,
            ),
        ) as BackgroundSpec.Photo
        assertEquals(1f, photo.dimAmount, 0f)
        assertEquals(25f, photo.blurRadiusDp, 0f)
    }

    @Test
    fun blankAssetRejected() {
        assertEquals(
            "请先选择图片",
            BackgroundValidation.validate(BackgroundSpec.Photo(assetId = "  ")),
        )
    }

    @Test
    fun externalUriRejected() {
        assertFalse(BackgroundValidation.isValidAssetId("content://media/1"))
        assertEquals(
            "图片资产无效，请重新选择",
            BackgroundValidation.validate(BackgroundSpec.Photo(assetId = "content://media/1")),
        )
    }

    @Test
    fun formalAssetIdFormat() {
        assertTrue(BackgroundValidation.isValidAssetId("bg_0123456789abcdef0123456789abcdef"))
        assertFalse(BackgroundValidation.isValidAssetId("bg_short"))
        assertTrue(
            BackgroundValidation.isValidStagingToken("draft_01234567-89ab-cdef-0123-456789abcdef"),
        )
    }

    @Test
    fun angleWraps() {
        assertEquals(270f, BackgroundValidation.normalizeAngle(-90f), 0.01f)
        assertEquals(10f, BackgroundValidation.normalizeAngle(370f), 0.01f)
    }

    @Test
    fun processedSizeBoundsBlur() {
        val size = ProcessedImageSizeCalculator.calculate(4000, 4000, blurRadiusDp = 8f)
        assertTrue(size.width <= BackgroundLimits.MAX_PROCESS_EDGE_BLUR)
        assertTrue(size.height <= BackgroundLimits.MAX_PROCESS_EDGE_BLUR)
        assertTrue(size.width.toLong() * size.height <= BackgroundLimits.MAX_PROCESS_PIXELS_BLUR)
    }
}
