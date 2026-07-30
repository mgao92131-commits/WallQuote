package com.example.wallquote.domain.model

/**
 * Pure render input for Canvas wallpaper (and shared layout rules with Compose preview).
 */
data class WallpaperRenderSpec(
    val background: BackgroundSpec,
    val text: String?,
    val textStyle: TextStyleConfig,
    val transform: QuoteTransform,
    val showEmptyHint: Boolean = false,
)
