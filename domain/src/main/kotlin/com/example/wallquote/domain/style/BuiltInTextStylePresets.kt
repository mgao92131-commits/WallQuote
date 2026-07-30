package com.example.wallquote.domain.style

import com.example.wallquote.domain.model.HorizontalTextAlignment
import com.example.wallquote.domain.model.SystemFontFamily
import com.example.wallquote.domain.model.TextStyleConfig

data class BuiltInTextStylePreset(
    val id: String,
    val displayName: String,
    val style: TextStyleConfig,
)

object BuiltInTextStylePresets {
    val all: List<BuiltInTextStylePreset> = listOf(
        BuiltInTextStylePreset(
            id = "classic_light",
            displayName = "经典白字",
            style = TextStyleConfig(colorHex = "#FFFFFF", textAlpha = 1f, shadowAlpha = 0f),
        ),
        BuiltInTextStylePreset(
            id = "classic_dark",
            displayName = "经典深色",
            style = TextStyleConfig(colorHex = "#1A1A1A", textAlpha = 1f, shadowAlpha = 0f),
        ),
        BuiltInTextStylePreset(
            id = "soft_shadow",
            displayName = "柔和阴影",
            style = TextStyleConfig(
                colorHex = "#FFFFFF",
                shadowRadiusDp = 8f,
                shadowDistanceDp = 4f,
                shadowAngleDegrees = 90f,
                shadowColorHex = "#000000",
                shadowAlpha = 0.45f,
            ),
        ),
        BuiltInTextStylePreset(
            id = "strong_shadow",
            displayName = "强阴影",
            style = TextStyleConfig(
                colorHex = "#FFFFFF",
                shadowRadiusDp = 14f,
                shadowDistanceDp = 8f,
                shadowAngleDegrees = 110f,
                shadowColorHex = "#000000",
                shadowAlpha = 0.75f,
            ),
        ),
        BuiltInTextStylePreset(
            id = "glass_dark",
            displayName = "深色半透明块",
            style = TextStyleConfig(
                colorHex = "#FFFFFF",
                blockColorHex = "#000000",
                blockAlpha = 0.45f,
                blockPaddingDp = 16f,
                blockCornerRadiusDp = 12f,
            ),
        ),
        BuiltInTextStylePreset(
            id = "glass_light",
            displayName = "浅色半透明块",
            style = TextStyleConfig(
                colorHex = "#1A1A1A",
                blockColorHex = "#FFFFFF",
                blockAlpha = 0.55f,
                blockPaddingDp = 16f,
                blockCornerRadiusDp = 12f,
            ),
        ),
        BuiltInTextStylePreset(
            id = "minimal_serif",
            displayName = "极简衬线",
            style = TextStyleConfig(
                colorHex = "#F5F5F5",
                fontFamily = SystemFontFamily.Serif,
                textSizeSp = 36f,
                letterSpacingEm = 0.04f,
                lineHeightMultiplier = 1.4f,
            ),
        ),
        BuiltInTextStylePreset(
            id = "terminal",
            displayName = "终端风格",
            style = TextStyleConfig(
                colorHex = "#A3BE8C",
                fontFamily = SystemFontFamily.Monospace,
                horizontalAlignment = HorizontalTextAlignment.Start,
                blockColorHex = "#2E3440",
                blockAlpha = 0.85f,
                blockPaddingDp = 12f,
                blockCornerRadiusDp = 4f,
                blockBorderWidthDp = 1f,
                blockBorderColorHex = "#A3BE8C",
                blockBorderAlpha = 0.6f,
            ),
        ),
    )

    fun findById(id: String): BuiltInTextStylePreset? = all.firstOrNull { it.id == id }
}
