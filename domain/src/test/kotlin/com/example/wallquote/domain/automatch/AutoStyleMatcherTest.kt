package com.example.wallquote.domain.automatch

import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoStyleMatcherTest {

    private fun sample(
        averageColorArgb: Long,
        contrastWithWhite: Float,
        contrastWithBlack: Float,
        visualComplexity: Float,
    ) = BackgroundSample(
        averageColorArgb = averageColorArgb,
        dominantColorArgb = averageColorArgb,
        averageLuminance = Contrast.relativeLuminance(averageColorArgb),
        contrastWithWhite = contrastWithWhite,
        contrastWithBlack = contrastWithBlack,
        visualComplexity = visualComplexity,
    )

    @Test
    fun darkSimpleBackground_prefersWhiteTextWithShadow() {
        val darkSample = sample(
            averageColorArgb = 0xFF000000L,
            contrastWithWhite = 21f,
            contrastWithBlack = 1f,
            visualComplexity = 0.1f,
        )
        val suggestion = AutoStyleMatcher.suggest(darkSample, TextStyleConfig())
        assertEquals("#FFFFFF", suggestion.style.colorHex)
        assertTrue(suggestion.style.shadowAlpha > 0f)
        assertEquals(null, suggestion.style.blockColorHex)
    }

    @Test
    fun lightSimpleBackground_prefersDarkTextWithShadow() {
        val lightSample = sample(
            averageColorArgb = 0xFFFFFFFFL,
            contrastWithWhite = 1f,
            contrastWithBlack = 21f,
            visualComplexity = 0.1f,
        )
        val suggestion = AutoStyleMatcher.suggest(lightSample, TextStyleConfig())
        assertEquals("#1A1A1A", suggestion.style.colorHex)
        assertTrue(suggestion.style.shadowAlpha > 0f)
    }

    @Test
    fun complexOrLowContrastBackground_enablesTextBlock() {
        val complexSample = sample(
            averageColorArgb = 0xFF808080L,
            contrastWithWhite = 2f,
            contrastWithBlack = 2f,
            visualComplexity = 0.6f,
        )
        val suggestion = AutoStyleMatcher.suggest(complexSample, TextStyleConfig())
        assertTrue(suggestion.style.blockColorHex != null)
        assertTrue(suggestion.style.blockAlpha > 0f)
        assertEquals(0f, suggestion.style.shadowAlpha, 0.0001f)
    }

    @Test
    fun doesNotMutateSizeFontWeightAlignOrSpacing() {
        val base = TextStyleConfig(
            textSizeSp = 40f,
            isBold = true,
            isItalic = true,
            horizontalAlignment = com.example.wallquote.domain.model.HorizontalTextAlignment.Start,
            letterSpacingEm = 0.1f,
            lineHeightMultiplier = 1.6f,
        )
        val sample = sample(0xFF444444L, contrastWithWhite = 10f, contrastWithBlack = 3f, visualComplexity = 0.1f)
        val suggestion = AutoStyleMatcher.suggest(sample, base)
        assertEquals(base.textSizeSp, suggestion.style.textSizeSp, 0.0001f)
        assertEquals(base.isBold, suggestion.style.isBold)
        assertEquals(base.isItalic, suggestion.style.isItalic)
        assertEquals(base.horizontalAlignment, suggestion.style.horizontalAlignment)
        assertEquals(base.letterSpacingEm, suggestion.style.letterSpacingEm, 0.0001f)
        assertEquals(base.lineHeightMultiplier, suggestion.style.lineHeightMultiplier, 0.0001f)
    }
}
