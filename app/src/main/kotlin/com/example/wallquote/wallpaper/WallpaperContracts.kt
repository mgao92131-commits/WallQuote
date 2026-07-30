package com.example.wallquote.wallpaper

import com.example.wallquote.domain.model.WallpaperRenderSpec

interface WallpaperClock {
    fun nowMillis(): Long
    fun minuteOfDay(): Int
}

sealed interface RenderOutcome {
    data object Success : RenderOutcome
    data object Skipped : RenderOutcome
    data class Failed(val reason: String) : RenderOutcome
}

interface WallpaperRenderTarget {
    fun render(
        holder: android.view.SurfaceHolder?,
        surfaceWidth: Int,
        surfaceHeight: Int,
        renderSpec: WallpaperRenderSpec,
        surfaceGeneration: Long,
    ): RenderOutcome
}

interface BoundaryScheduler {
    fun scheduleAfterMillis(delayMillis: Long, onFire: () -> Unit)
    fun cancel()
}

interface WallpaperDiagnostics {
    fun log(event: String, details: Map<String, Any?> = emptyMap())
}
