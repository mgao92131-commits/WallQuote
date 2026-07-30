package com.example.wallquote.wallpaper

import android.graphics.Bitmap
import android.view.SurfaceHolder
import com.example.wallquote.domain.model.WallpaperRenderSpec

interface WallpaperClock {
    /** Monotonic clock for hide-duration (SystemClock.elapsedRealtime). */
    fun elapsedRealtimeMillis(): Long

    /** Wall clock for diagnostics / advance timestamps. */
    fun currentWallTimeMillis(): Long

    /** Calendar minute of day in the current default time zone. */
    fun minuteOfDay(): Int

    /** Seconds within the current minute (0..59). */
    fun secondOfMinute(): Int

    /** Milliseconds within the current second (0..999). */
    fun millisOfSecond(): Int
}

sealed interface RenderOutcome {
    data object Success : RenderOutcome
    data object Skipped : RenderOutcome
    data class Failed(val reason: String) : RenderOutcome
}

data class PreparedPhotoFrame(
    val bitmap: Bitmap,
    val dimAmount: Float,
)

interface WallpaperRenderTarget {
    fun render(
        holder: SurfaceHolder?,
        surfaceWidth: Int,
        surfaceHeight: Int,
        renderSpec: WallpaperRenderSpec,
        surfaceGeneration: Long,
        preparedPhoto: PreparedPhotoFrame? = null,
    ): RenderOutcome
}

interface BoundaryScheduler {
    fun scheduleAfterMillis(delayMillis: Long, onFire: () -> Unit)
    fun cancel()
}

interface WallpaperDiagnostics {
    fun log(event: String, details: Map<String, Any?> = emptyMap())
}
