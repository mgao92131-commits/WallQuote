package com.example.wallquote.domain.model

/**
 * Normalized quote placement for wallpaper canvas and preview (fractions of the drawable area).
 */
data class QuoteTransform(
    val centerXFraction: Float = 0.5f,
    val centerYFraction: Float = 0.5f,
    val rotationDegrees: Float = 0f,
)
