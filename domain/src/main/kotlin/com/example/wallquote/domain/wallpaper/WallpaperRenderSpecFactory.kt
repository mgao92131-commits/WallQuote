package com.example.wallquote.domain.wallpaper

import com.example.wallquote.domain.CollectionDefaults
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.PlaybackState
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.model.WallpaperRenderSpec

object WallpaperRenderSpecFactory {

    fun fromState(
        collections: List<CollectionConfig>,
        state: PlaybackState,
        showEmptyHint: Boolean,
    ): WallpaperRenderSpec {
        val collectionId = state.cursor.collectionId
        val quoteLineId = state.cursor.quoteLineId
        if (collectionId == null || quoteLineId == null) {
            return WallpaperRenderSpec(
                background = BackgroundSpec.Solid(CollectionDefaults.DEFAULT_SOLID_HEX),
                text = null,
                textStyle = TextStyleConfig(),
                transform = QuoteTransform(),
                showEmptyHint = showEmptyHint,
            )
        }
        val collection = collections.firstOrNull { it.id == collectionId }
            ?: return WallpaperRenderSpec(
                background = BackgroundSpec.Solid(CollectionDefaults.DEFAULT_SOLID_HEX),
                text = null,
                textStyle = TextStyleConfig(),
                transform = QuoteTransform(),
                showEmptyHint = showEmptyHint,
            )
        val line = collection.lines.firstOrNull { it.id == quoteLineId }
        return WallpaperRenderSpec(
            background = collection.background,
            text = line?.text?.takeIf { it.isNotBlank() },
            textStyle = collection.textStyle,
            transform = collection.transform,
            showEmptyHint = false,
        )
    }
}
