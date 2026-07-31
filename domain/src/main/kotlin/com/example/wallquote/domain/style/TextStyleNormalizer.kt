package com.example.wallquote.domain.style

import com.example.wallquote.domain.background.BackgroundValidation
import com.example.wallquote.domain.model.HorizontalTextAlignment
import com.example.wallquote.domain.model.SystemFontFamily
import com.example.wallquote.domain.model.TextStyleConfig

object TextStyleLimits {
    const val TEXT_ALPHA_MIN = 0f
    const val TEXT_ALPHA_MAX = 1f
    const val TEXT_SIZE_MIN = 12f
    const val TEXT_SIZE_MAX = 96f
    const val LETTER_SPACING_MIN = -0.05f
    const val LETTER_SPACING_MAX = 0.50f
    const val LINE_HEIGHT_MIN = 0.8f
    const val LINE_HEIGHT_MAX = 2.0f
    const val BLOCK_ALPHA_MIN = 0f
    const val BLOCK_ALPHA_MAX = 1f
    const val BLOCK_PADDING_MAX = 48f
    const val BLOCK_CORNER_MAX = 48f
    const val BORDER_WIDTH_MAX = 8f
    const val BORDER_ALPHA_MIN = 0f
    const val BORDER_ALPHA_MAX = 1f
    const val SHADOW_RADIUS_MAX = 32f
    const val SHADOW_DISTANCE_MAX = 32f
    const val SHADOW_ALPHA_MIN = 0f
    const val SHADOW_ALPHA_MAX = 1f
}

object TextStyleNormalizer {
    fun normalize(style: TextStyleConfig): TextStyleConfig =
        style.copy(
            colorHex = normalizeRequiredColor(style.colorHex, "#FFFFFF"),
            textAlpha = style.textAlpha.finiteOr(1f).coerceIn(TextStyleLimits.TEXT_ALPHA_MIN, TextStyleLimits.TEXT_ALPHA_MAX),
            textSizeSp = style.textSizeSp.finiteOr(32f).coerceIn(TextStyleLimits.TEXT_SIZE_MIN, TextStyleLimits.TEXT_SIZE_MAX),
            fontFamily = style.fontFamily,
            horizontalAlignment = style.horizontalAlignment,
            letterSpacingEm = style.letterSpacingEm.finiteOr(0f)
                .coerceIn(TextStyleLimits.LETTER_SPACING_MIN, TextStyleLimits.LETTER_SPACING_MAX),
            lineHeightMultiplier = style.lineHeightMultiplier.finiteOr(1.3f)
                .coerceIn(TextStyleLimits.LINE_HEIGHT_MIN, TextStyleLimits.LINE_HEIGHT_MAX),
            blockColorHex = style.blockColorHex?.let { normalizeOptionalColor(it) },
            blockAlpha = style.blockAlpha.finiteOr(0f).coerceIn(TextStyleLimits.BLOCK_ALPHA_MIN, TextStyleLimits.BLOCK_ALPHA_MAX),
            blockPaddingDp = style.blockPaddingDp.finiteOr(0f).coerceIn(0f, TextStyleLimits.BLOCK_PADDING_MAX),
            blockCornerRadiusDp = style.blockCornerRadiusDp.finiteOr(0f).coerceIn(0f, TextStyleLimits.BLOCK_CORNER_MAX),
            blockBorderWidthDp = style.blockBorderWidthDp.finiteOr(0f).coerceIn(0f, TextStyleLimits.BORDER_WIDTH_MAX),
            blockBorderColorHex = style.blockBorderColorHex?.let { normalizeOptionalColor(it) },
            blockBorderAlpha = style.blockBorderAlpha.finiteOr(1f)
                .coerceIn(TextStyleLimits.BORDER_ALPHA_MIN, TextStyleLimits.BORDER_ALPHA_MAX),
            shadowRadiusDp = style.shadowRadiusDp.finiteOr(0f).coerceIn(0f, TextStyleLimits.SHADOW_RADIUS_MAX),
            shadowDistanceDp = style.shadowDistanceDp.finiteOr(0f).coerceIn(0f, TextStyleLimits.SHADOW_DISTANCE_MAX),
            shadowAngleDegrees = BackgroundValidation.normalizeAngle(style.shadowAngleDegrees.finiteOr(90f)),
            shadowColorHex = normalizeRequiredColor(style.shadowColorHex, "#000000"),
            shadowAlpha = style.shadowAlpha.finiteOr(0f)
                .coerceIn(TextStyleLimits.SHADOW_ALPHA_MIN, TextStyleLimits.SHADOW_ALPHA_MAX),
        )

    /**
     * @deprecated Validates against the *normalized* style, which silently repairs invalid
     * colors and therefore can never report a color error (see P4-007). Prefer
     * [TextStyleValidator.validate] on the raw, pre-normalize style for user-facing validation.
     */
    @Deprecated(
        message = "Normalizing before validating hides invalid input; use TextStyleValidator instead.",
        replaceWith = ReplaceWith("TextStyleValidator.validate(style)", "com.example.wallquote.domain.style.TextStyleValidator"),
    )
    fun validate(style: TextStyleConfig): String? = TextStyleValidator.validate(style)

    fun hasVisibleBlock(style: TextStyleConfig): Boolean {
        val n = normalize(style)
        return n.blockColorHex != null && n.blockAlpha > 0.01f
    }

    fun hasVisibleShadow(style: TextStyleConfig): Boolean {
        val n = normalize(style)
        return n.shadowAlpha > 0.01f && (n.shadowRadiusDp > 0f || n.shadowDistanceDp > 0f)
    }

    private fun normalizeRequiredColor(hex: String, fallback: String): String {
        val withHash = if (hex.trim().startsWith("#")) hex.trim() else "#${hex.trim()}"
        return if (BackgroundValidation.isValidColorHex(withHash)) expand(withHash) else fallback
    }

    private fun normalizeOptionalColor(hex: String): String? {
        val withHash = if (hex.trim().startsWith("#")) hex.trim() else "#${hex.trim()}"
        return if (BackgroundValidation.isValidColorHex(withHash)) expand(withHash) else null
    }

    private fun expand(hex: String): String {
        val body = hex.removePrefix("#")
        return when (body.length) {
            3 -> "#" + body.map { "$it$it" }.joinToString("")
            else -> "#$body"
        }
    }

    private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback
}

object QuoteTransformNormalizer {
    fun normalize(
        centerXFraction: Float,
        centerYFraction: Float,
        rotationDegrees: Float,
    ): com.example.wallquote.domain.model.QuoteTransform =
        com.example.wallquote.domain.model.QuoteTransform(
            centerXFraction = centerXFraction.finiteOr(0.5f).coerceIn(0f, 1f),
            centerYFraction = centerYFraction.finiteOr(0.5f).coerceIn(0f, 1f),
            rotationDegrees = BackgroundValidation.normalizeAngle(rotationDegrees.finiteOr(0f)),
        )

    private fun Float.finiteOr(fallback: Float): Float = if (isFinite()) this else fallback
}
