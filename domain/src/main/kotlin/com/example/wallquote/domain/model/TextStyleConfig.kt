package com.example.wallquote.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class HorizontalTextAlignment {
    @SerialName("start")
    Start,

    @SerialName("center")
    Center,

    @SerialName("end")
    End,
}

@Serializable
enum class SystemFontFamily {
    @SerialName("serif")
    Serif,

    @SerialName("sans_serif")
    SansSerif,

    @SerialName("monospace")
    Monospace,

    @SerialName("cursive")
    Cursive,
}

@Serializable
data class TextStyleConfig(
    val colorHex: String = "#FFFFFF",
    val textAlpha: Float = 1f,
    val textSizeSp: Float = 32f,
    val fontFamily: SystemFontFamily = SystemFontFamily.Serif,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val horizontalAlignment: HorizontalTextAlignment = HorizontalTextAlignment.Center,
    val letterSpacingEm: Float = 0f,
    val lineHeightMultiplier: Float = 1.3f,
    val blockColorHex: String? = null,
    val blockAlpha: Float = 0f,
    val blockPaddingDp: Float = 0f,
    val blockCornerRadiusDp: Float = 0f,
    val blockBorderWidthDp: Float = 0f,
    val blockBorderColorHex: String? = null,
    val blockBorderAlpha: Float = 1f,
    val shadowRadiusDp: Float = 0f,
    val shadowDistanceDp: Float = 0f,
    val shadowAngleDegrees: Float = 90f,
    val shadowColorHex: String = "#000000",
    val shadowAlpha: Float = 0f,
)
