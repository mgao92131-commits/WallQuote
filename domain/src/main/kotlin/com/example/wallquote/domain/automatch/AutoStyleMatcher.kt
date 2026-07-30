package com.example.wallquote.domain.automatch

data class BackgroundSample(
    val averageColorArgb: Long,
    val dominantColorArgb: Long,
    val averageLuminance: Float,
    val contrastWithWhite: Float,
    val contrastWithBlack: Float,
    val visualComplexity: Float,
)

data class TextStyleSuggestion(
    val style: com.example.wallquote.domain.model.TextStyleConfig,
    val reason: String,
)

object Contrast {
    fun relativeLuminance(argb: Long): Float {
        val r = ((argb shr 16) and 0xFF) / 255f
        val g = ((argb shr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        fun channel(c: Float): Float =
            if (c <= 0.03928f) c / 12.92f else Math.pow(((c + 0.055) / 1.055).toDouble(), 2.4).toFloat()
        return 0.2126f * channel(r) + 0.7152f * channel(g) + 0.0722f * channel(b)
    }

    fun contrastRatio(l1: Float, l2: Float): Float {
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05f) / (darker + 0.05f)
    }

    fun contrastWithWhite(argb: Long): Float =
        contrastRatio(relativeLuminance(argb), 1f)

    fun contrastWithBlack(argb: Long): Float =
        contrastRatio(relativeLuminance(argb), 0f)
}

object AutoStyleMatcher {
    private const val TARGET_CONTRAST = 4.5f

    fun suggest(
        sample: BackgroundSample,
        base: com.example.wallquote.domain.model.TextStyleConfig,
    ): TextStyleSuggestion {
        val preferWhite = sample.contrastWithWhite >= sample.contrastWithBlack
        val textColor = if (preferWhite) "#FFFFFF" else "#1A1A1A"
        val textLum = if (preferWhite) 1f else Contrast.relativeLuminance(0xFF1A1A1AL)
        val contrast = if (preferWhite) sample.contrastWithWhite else sample.contrastWithBlack
        val complex = sample.visualComplexity >= 0.35f ||
            (sample.contrastWithWhite < TARGET_CONTRAST && sample.contrastWithBlack < TARGET_CONTRAST)

        var style = base.copy(
            colorHex = textColor,
            textAlpha = 1f,
            // Auto Match must not change size/font/weight/align/spacing/transform fields beyond colors/shadow/block.
            shadowRadiusDp = 0f,
            shadowDistanceDp = 0f,
            shadowAlpha = 0f,
            blockColorHex = null,
            blockAlpha = 0f,
            blockPaddingDp = 0f,
            blockCornerRadiusDp = 0f,
            blockBorderWidthDp = 0f,
        )

        val reason: String
        when {
            !complex && contrast >= TARGET_CONTRAST -> {
                style = style.copy(
                    shadowRadiusDp = 6f,
                    shadowDistanceDp = 3f,
                    shadowAngleDegrees = 90f,
                    shadowColorHex = if (preferWhite) "#000000" else "#FFFFFF",
                    shadowAlpha = 0.35f,
                )
                reason = "对比度足够，使用轻阴影"
            }
            else -> {
                val blockColor = if (preferWhite) "#000000" else "#FFFFFF"
                var blockAlpha = 0.45f
                // Increase block until contrast heuristic is satisfied.
                while (blockAlpha < 0.85f) {
                    val blended = blend(sample.averageColorArgb, parseHex(blockColor), blockAlpha)
                    val c = Contrast.contrastRatio(Contrast.relativeLuminance(blended), textLum)
                    if (c >= TARGET_CONTRAST) break
                    blockAlpha += 0.1f
                }
                style = style.copy(
                    blockColorHex = blockColor,
                    blockAlpha = blockAlpha.coerceAtMost(0.85f),
                    blockPaddingDp = 14f,
                    blockCornerRadiusDp = 10f,
                )
                reason = "背景复杂或对比不足，启用半透明文字块"
            }
        }
        return TextStyleSuggestion(style = style, reason = reason)
    }

    private fun parseHex(hex: String): Long {
        val n = hex.removePrefix("#")
        return when (n.length) {
            6 -> 0xFF000000L or n.toLong(16)
            8 -> n.toLong(16)
            else -> 0xFF000000L
        }
    }

    private fun blend(base: Long, overlay: Long, alpha: Float): Long {
        val a = alpha.coerceIn(0f, 1f)
        fun ch(value: Long, shift: Int): Int = ((value shr shift) and 0xFF).toInt()
        fun mix(b: Int, o: Int): Int = (b * (1 - a) + o * a).toInt().coerceIn(0, 255)
        val r = mix(ch(base, 16), ch(overlay, 16))
        val g = mix(ch(base, 8), ch(overlay, 8))
        val b = mix(ch(base, 0), ch(overlay, 0))
        return 0xFF000000L or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }
}
