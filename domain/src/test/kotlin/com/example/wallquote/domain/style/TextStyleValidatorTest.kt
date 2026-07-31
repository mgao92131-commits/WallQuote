package com.example.wallquote.domain.style

import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextStyleValidatorTest {

    @Test
    fun validate_returnsNullForDefaultStyle() {
        assertNull(TextStyleValidator.validate(TextStyleConfig()))
    }

    @Test
    fun validate_invalidTextColor_returnsError() {
        assertEquals("文字颜色无效", TextStyleValidator.validate(TextStyleConfig(colorHex = "not-a-color")))
    }

    @Test
    fun validate_invalidBlockColor_returnsError() {
        assertEquals(
            "文字背景颜色无效",
            TextStyleValidator.validate(TextStyleConfig(blockColorHex = "also-bad")),
        )
    }

    @Test
    fun validate_invalidBorderColor_returnsError() {
        assertEquals(
            "边框颜色无效",
            TextStyleValidator.validate(TextStyleConfig(blockBorderColorHex = "still-bad")),
        )
    }

    @Test
    fun validate_invalidShadowColor_returnsError() {
        assertEquals("阴影颜色无效", TextStyleValidator.validate(TextStyleConfig(shadowColorHex = "nope")))
    }

    @Test
    fun validate_nullOptionalColors_areValid() {
        assertNull(
            TextStyleValidator.validate(
                TextStyleConfig(blockColorHex = null, blockBorderColorHex = null),
            ),
        )
    }

    @Test
    fun validate_shorthandHexColors_areValid() {
        assertNull(TextStyleValidator.validate(TextStyleConfig(colorHex = "abc", shadowColorHex = "fff")))
    }
}
