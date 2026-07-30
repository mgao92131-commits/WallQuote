package com.example.wallquote.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TextStyleConfig(
    val colorHex: String = "#FFFFFF",
    val textAlpha: Float = 1f,
    val textSizeSp: Float = 32f,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val fontFamilyName: String = "Serif",
    val alignment: Int = 1,
    val verticalAlignment: Int = 1,
    val isVerticalText: Boolean = false,
    val letterSpacingSp: Float = 0f,
    val lineHeightMultiplier: Float = 1.3f,
    val bgColorHex: String? = null,
    val bgAlpha: Float = 0.5f,
    val cornerRadius: Float = 0f,
    val padding: Float = 0f,
    val borderWidth: Float = 0f,
    val borderColorHex: String? = null,
    val borderAlpha: Float = 1f,
    val shadowRadius: Float = 0f,
    val shadowDistance: Float = 8f,
    val shadowAngle: Float = 90f,
    val shadowAlpha: Float = 1f,
    val shadowColorHex: String = "#80000000",
)
