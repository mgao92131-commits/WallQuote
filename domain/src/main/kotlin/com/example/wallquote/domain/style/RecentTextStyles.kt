package com.example.wallquote.domain.style

import com.example.wallquote.domain.model.TextStyleConfig

/**
 * MRU list of recently used [TextStyleConfig] values. Not a user-managed style library:
 * no names, no CRUD, no explicit save. Identical styles (after normalize) are deduped
 * and moved to the front.
 */
object RecentTextStyles {
    const val MAX_SIZE = 6

    fun push(style: TextStyleConfig, existing: List<TextStyleConfig>): List<TextStyleConfig> {
        val canonical = canonicalize(style)
        val rest = existing.map { canonicalize(it) }.filterNot { it == canonical }
        return (listOf(canonical) + rest).take(MAX_SIZE)
    }

    private fun canonicalize(style: TextStyleConfig): TextStyleConfig {
        val normalized = TextStyleNormalizer.normalize(style)
        return normalized.copy(
            colorHex = normalized.colorHex.uppercase(),
            blockColorHex = normalized.blockColorHex?.uppercase(),
            blockBorderColorHex = normalized.blockBorderColorHex?.uppercase(),
            shadowColorHex = normalized.shadowColorHex.uppercase(),
        )
    }
}
