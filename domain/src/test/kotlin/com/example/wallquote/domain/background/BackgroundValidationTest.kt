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
                assetId = "bg_abc",
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
        assertFalse(BackgroundValidation.isUsablePhotoAssetId("content://media/1"))
        assertEquals(
            "图片资产无效，请重新选择",
            BackgroundValidation.validate(BackgroundSpec.Photo(assetId = "content://media/1")),
        )
    }

    @Test
    fun angleWraps() {
        assertEquals(270f, BackgroundValidation.normalizeAngle(-90f), 0.01f)
        assertEquals(10f, BackgroundValidation.normalizeAngle(370f), 0.01f)
    }

    @Test
    fun validGradient() {
        assertNull(
            BackgroundValidation.validate(
                BackgroundSpec.Gradient("#111111", "#222222", angleDegrees = 45f),
            ),
        )
        assertTrue(BackgroundValidation.isValidColorHex("#FFFFFF"))
    }
}
