package com.example.wallquote.core.preview

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.TextStyleConfig

/**
 * Shared render input for Compose preview and future wallpaper canvas rendering.
 */
data class WallpaperPreviewState(
    val background: BackgroundSpec,
    val lines: List<String>,
    val previewLineIndex: Int,
    val textStyle: TextStyleConfig,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDegrees: Float = 0f,
) {
    val previewText: String
        get() = lines.getOrElse(previewLineIndex.coerceIn(0, (lines.size - 1).coerceAtLeast(0))) { "" }

    companion object {
        fun fromCollection(
            config: CollectionConfig,
            previewLineIndex: Int = 0,
        ): WallpaperPreviewState =
            WallpaperPreviewState(
                background = config.background,
                lines = config.texts.ifEmpty { listOf("") },
                previewLineIndex = previewLineIndex,
                textStyle = config.textStyle,
                offsetX = config.offsetX,
                offsetY = config.offsetY,
                rotationDegrees = config.rotation,
            )
    }
}
