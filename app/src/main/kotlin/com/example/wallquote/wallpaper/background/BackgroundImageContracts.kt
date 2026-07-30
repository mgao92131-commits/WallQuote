package com.example.wallquote.wallpaper.background

import android.graphics.Bitmap
import kotlin.math.max
import kotlin.math.roundToInt

data class BackgroundImageKey(
    val assetId: String,
    val processedWidth: Int,
    val processedHeight: Int,
    val effectiveBlurRadiusPx: Int,
)

data class BackgroundLoadToken(
    val surfaceGeneration: Long,
    val collectionId: Long,
    val assetId: String,
    val processedWidth: Int,
    val processedHeight: Int,
    val effectiveBlurRadiusPx: Int,
)

enum class BackgroundImageFailure {
    Missing,
    DecodeFailed,
    OutOfMemory,
    Unknown,
}

sealed interface BackgroundImageResult {
    data class Success(
        val key: BackgroundImageKey,
        val bitmap: Bitmap,
    ) : BackgroundImageResult

    data class Failed(
        val reason: BackgroundImageFailure,
    ) : BackgroundImageResult
}

data class ProcessedBackgroundImage(
    val bitmap: Bitmap,
    val processedWidth: Int,
    val processedHeight: Int,
    val effectiveBlurRadiusPx: Int,
)

interface BackgroundImageProcessor {
    suspend fun process(
        sourcePath: String,
        targetWidth: Int,
        targetHeight: Int,
        blurRadiusDp: Float,
        density: Float,
    ): ProcessedBackgroundImage
}

interface BackgroundImageLoader {
    suspend fun load(
        assetId: String,
        targetWidth: Int,
        targetHeight: Int,
        blurRadiusDp: Float,
        density: Float,
    ): BackgroundImageResult

    fun trimToHalf()

    fun evictAll()
}

interface BackgroundMemoryController {
    fun trimToHalf()
    fun evictAll()
}

/**
 * In-memory LRU of processed wallpaper/editor backgrounds.
 * Dim is applied at draw time and is not part of the cache key.
 */
class BackgroundBitmapCache(
    maxBytes: Int = defaultMaxBytes(),
) : BackgroundMemoryController {
    private val cache = object : android.util.LruCache<BackgroundImageKey, Bitmap>(maxBytes) {
        override fun sizeOf(key: BackgroundImageKey, value: Bitmap): Int = value.byteCount
    }

    @Synchronized
    fun get(key: BackgroundImageKey): Bitmap? = cache.get(key)

    @Synchronized
    fun put(key: BackgroundImageKey, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    @Synchronized
    override fun evictAll() {
        cache.evictAll()
    }

    @Synchronized
    override fun trimToHalf() {
        cache.trimToSize(max(cache.maxSize() / 2, 1))
    }

    companion object {
        fun defaultMaxBytes(): Int {
            val maxMemory = (Runtime.getRuntime().maxMemory() / 8).toInt()
            return maxMemory.coerceIn(8 * 1024 * 1024, 48 * 1024 * 1024)
        }
    }
}
