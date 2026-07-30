package com.example.wallquote.domain.background

object BackgroundLimits {
    const val DIM_MIN = 0f
    const val DIM_MAX = 1f
    const val BLUR_MIN_DP = 0f
    const val BLUR_MAX_DP = 25f
    const val ANGLE_MIN = 0f
    const val ANGLE_MAX = 360f

    /** Max edge for stored/normalized formal assets. */
    const val MAX_DECODE_EDGE_PX = 2048

    /** Soft ceiling for decoded pixel count before downsampling further. */
    const val MAX_DECODE_PIXELS = 2048 * 2048

    /** Hard limit on source image file size at import. */
    const val MAX_IMPORT_BYTES = 50L * 1024L * 1024L

    /** Hard limit on source pixel count at import. */
    const val MAX_SOURCE_PIXELS = 100_000_000L

    /** Hard limit on a single source edge at import. */
    const val MAX_SOURCE_EDGE = 32_768

    /** Max processed pixels without blur (~4MP). */
    const val MAX_PROCESS_PIXELS_SHARP = 4_000_000

    /** Max processed pixels with blur (~1.5MP). */
    const val MAX_PROCESS_PIXELS_BLUR = 1_500_000

    /** Max processed edge when blur is applied. */
    const val MAX_PROCESS_EDGE_BLUR = 1280

    /** Staging files older than this may be purged by orphan cleanup. */
    const val STAGING_RETENTION_MILLIS = 24L * 60L * 60L * 1000L

    /** Unreferenced formal assets older than this may be purged. */
    const val ORPHAN_ASSET_RETENTION_MILLIS = 60L * 60L * 1000L
}
