package com.example.wallquote.domain.style

import com.example.wallquote.domain.background.BackgroundValidation
import com.example.wallquote.domain.model.TextStyleConfig

/**
 * Validates a [TextStyleConfig] as the user entered it, WITHOUT normalizing first (P4-007).
 *
 * [TextStyleNormalizer.normalize] silently repairs invalid colors by falling back to a default,
 * so validating a normalized style can never surface a color error to the user. This validator
 * checks the raw fields so invalid input is rejected rather than silently fixed.
 */
object TextStyleValidator {
    fun validate(style: TextStyleConfig): String? {
        if (!isValidColor(style.colorHex)) return "文字颜色无效"
        if (style.blockColorHex != null && !isValidColor(style.blockColorHex)) {
            return "文字背景颜色无效"
        }
        if (style.blockBorderColorHex != null && !isValidColor(style.blockBorderColorHex)) {
            return "边框颜色无效"
        }
        if (!isValidColor(style.shadowColorHex)) return "阴影颜色无效"
        return null
    }

    private fun isValidColor(hex: String): Boolean {
        val trimmed = hex.trim()
        val withHash = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        return BackgroundValidation.isValidColorHex(withHash)
    }
}
