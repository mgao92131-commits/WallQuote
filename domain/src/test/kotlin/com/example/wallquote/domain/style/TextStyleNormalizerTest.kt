package com.example.wallquote.domain.style

import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextStyleNormalizerTest {

    @Test
    fun normalize_clampsOutOfRangeNumericFields() {
        val style = TextStyleConfig(
            textAlpha = 5f,
            textSizeSp = 1000f,
            letterSpacingEm = 10f,
            lineHeightMultiplier = 10f,
            blockAlpha = -5f,
            blockPaddingDp = 999f,
            blockCornerRadiusDp = 999f,
            blockBorderWidthDp = 999f,
            blockBorderAlpha = -1f,
            shadowRadiusDp = 999f,
            shadowDistanceDp = 999f,
            shadowAlpha = -1f,
        )
        val normalized = TextStyleNormalizer.normalize(style)
        assertEquals(TextStyleLimits.TEXT_ALPHA_MAX, normalized.textAlpha, 0.0001f)
        assertEquals(TextStyleLimits.TEXT_SIZE_MAX, normalized.textSizeSp, 0.0001f)
        assertEquals(TextStyleLimits.LETTER_SPACING_MAX, normalized.letterSpacingEm, 0.0001f)
        assertEquals(TextStyleLimits.LINE_HEIGHT_MAX, normalized.lineHeightMultiplier, 0.0001f)
        assertEquals(TextStyleLimits.BLOCK_ALPHA_MIN, normalized.blockAlpha, 0.0001f)
        assertEquals(TextStyleLimits.BLOCK_PADDING_MAX, normalized.blockPaddingDp, 0.0001f)
        assertEquals(TextStyleLimits.BLOCK_CORNER_MAX, normalized.blockCornerRadiusDp, 0.0001f)
        assertEquals(TextStyleLimits.BORDER_WIDTH_MAX, normalized.blockBorderWidthDp, 0.0001f)
        assertEquals(TextStyleLimits.BORDER_ALPHA_MIN, normalized.blockBorderAlpha, 0.0001f)
        assertEquals(TextStyleLimits.SHADOW_RADIUS_MAX, normalized.shadowRadiusDp, 0.0001f)
        assertEquals(TextStyleLimits.SHADOW_DISTANCE_MAX, normalized.shadowDistanceDp, 0.0001f)
        assertEquals(TextStyleLimits.SHADOW_ALPHA_MIN, normalized.shadowAlpha, 0.0001f)
    }

    @Test
    fun normalize_repairsInvalidColors() {
        val style = TextStyleConfig(
            colorHex = "not-a-color",
            blockColorHex = "also-bad",
            blockBorderColorHex = "still-bad",
            shadowColorHex = "nope",
        )
        val normalized = TextStyleNormalizer.normalize(style)
        assertEquals("#FFFFFF", normalized.colorHex)
        assertNull(normalized.blockColorHex)
        assertNull(normalized.blockBorderColorHex)
        assertEquals("#000000", normalized.shadowColorHex)
    }

    @Test
    fun normalize_expandsShorthandHexAndAddsHash() {
        val style = TextStyleConfig(colorHex = "abc", shadowColorHex = "fff")
        val normalized = TextStyleNormalizer.normalize(style)
        assertEquals("#AABBCC", normalized.colorHex.uppercase())
        assertEquals("#FFFFFF", normalized.shadowColorHex.uppercase())
    }

    @Test
    fun normalize_nonFiniteFallsBackToDefault() {
        val style = TextStyleConfig(textAlpha = Float.NaN, textSizeSp = Float.POSITIVE_INFINITY)
        val normalized = TextStyleNormalizer.normalize(style)
        assertEquals(1f, normalized.textAlpha, 0.0001f)
        assertEquals(32f, normalized.textSizeSp, 0.0001f)
    }

    @Test
    fun validate_returnsNullForValidStyle() {
        assertNull(TextStyleNormalizer.validate(TextStyleConfig()))
    }

    @Test
    fun validate_invalidColorReturnsError() {
        // P4-007: validate() must delegate to TextStyleValidator on the RAW style, not normalize
        // first — normalizing before validating would silently repair the bad color into a valid
        // default and never surface the error.
        assertEquals("文字颜色无效", TextStyleNormalizer.validate(TextStyleConfig(colorHex = "zzzzzz")))
    }

    @Test
    fun hasVisibleBlock_requiresColorAndAlpha() {
        assertFalse(TextStyleNormalizer.hasVisibleBlock(TextStyleConfig()))
        assertFalse(
            TextStyleNormalizer.hasVisibleBlock(
                TextStyleConfig(blockColorHex = "#000000", blockAlpha = 0f),
            ),
        )
        assertTrue(
            TextStyleNormalizer.hasVisibleBlock(
                TextStyleConfig(blockColorHex = "#000000", blockAlpha = 0.5f),
            ),
        )
    }

    @Test
    fun hasVisibleShadow_requiresAlphaAndRadiusOrDistance() {
        assertFalse(TextStyleNormalizer.hasVisibleShadow(TextStyleConfig()))
        assertFalse(
            TextStyleNormalizer.hasVisibleShadow(
                TextStyleConfig(shadowAlpha = 0.5f, shadowRadiusDp = 0f, shadowDistanceDp = 0f),
            ),
        )
        assertTrue(
            TextStyleNormalizer.hasVisibleShadow(
                TextStyleConfig(shadowAlpha = 0.5f, shadowRadiusDp = 4f),
            ),
        )
    }
}
