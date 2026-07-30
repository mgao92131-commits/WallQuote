package com.example.wallquote.domain.background

object BackgroundLimits {
    const val DIM_MIN = 0f
    const val DIM_MAX = 1f
    const val BLUR_MIN_DP = 0f
    const val BLUR_MAX_DP = 25f
    const val ANGLE_MIN = 0f
    const val ANGLE_MAX = 360f

    /** Max decoded edge length for wallpaper bitmaps (px). */
    const val MAX_DECODE_EDGE_PX = 2048

    /** Soft ceiling for decoded pixel count before downsampling further. */
    const val MAX_DECODE_PIXELS = 2048 * 2048

    /** Staging files older than this may be purged by orphan cleanup. */
    const val STAGING_RETENTION_MILLIS = 24L * 60L * 60L * 1000L

    /** Unreferenced formal assets older than this may be purged. */
    const val ORPHAN_ASSET_RETENTION_MILLIS = 60L * 60L * 1000L
}
