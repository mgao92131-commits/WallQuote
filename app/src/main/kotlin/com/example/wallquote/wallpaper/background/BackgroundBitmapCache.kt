package com.example.wallquote.wallpaper.background

import android.graphics.Bitmap
import android.util.LruCache
import kotlin.math.max

/**
 * In-memory LRU of decoded (and optionally blurred) wallpaper backgrounds.
 * Dim is applied at draw time and is not part of the cache key.
 */
class BackgroundBitmapCache(
    maxBytes: Int = defaultMaxBytes(),
) {
    private val cache = object : LruCache<BackgroundImageKey, Bitmap>(maxBytes) {
        override fun sizeOf(key: BackgroundImageKey, value: Bitmap): Int = value.byteCount
    }

    @Synchronized
    fun get(key: BackgroundImageKey): Bitmap? = cache.get(key)

    @Synchronized
    fun put(key: BackgroundImageKey, bitmap: Bitmap) {
        cache.put(key, bitmap)
    }

    @Synchronized
    fun evictAll() {
        cache.evictAll()
    }

    @Synchronized
    fun trimToHalf() {
        cache.trimToSize(max(cache.maxSize() / 2, 1))
    }

    companion object {
        fun defaultMaxBytes(): Int {
            val maxMemory = (Runtime.getRuntime().maxMemory() / 8).toInt()
            return maxMemory.coerceIn(8 * 1024 * 1024, 48 * 1024 * 1024)
        }

        fun blurBucket(blurRadiusDp: Float): Int =
            when {
                blurRadiusDp <= 0f -> 0
                else -> ((blurRadiusDp * 2f).toInt()).coerceIn(1, 50)
            }
    }
}
