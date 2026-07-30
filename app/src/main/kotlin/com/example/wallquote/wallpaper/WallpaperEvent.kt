package com.example.wallquote.wallpaper

import android.view.SurfaceHolder
import com.example.wallquote.domain.model.CollectionConfig

sealed interface WallpaperEvent {
    data class SurfaceCreated(val holder: SurfaceHolder) : WallpaperEvent
    data class SurfaceChanged(
        val holder: SurfaceHolder,
        val width: Int,
        val height: Int,
    ) : WallpaperEvent

    data object SurfaceDestroyed : WallpaperEvent
    data class VisibilityChanged(val visible: Boolean) : WallpaperEvent
    data class CollectionsChanged(
        val collections: List<CollectionConfig>,
    ) : WallpaperEvent

    data object ScreenOn : WallpaperEvent
    data object ScheduleBoundaryReached : WallpaperEvent
    data object Destroy : WallpaperEvent
}
