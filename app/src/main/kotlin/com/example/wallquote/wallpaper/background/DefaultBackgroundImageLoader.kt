package com.example.wallquote.wallpaper.background

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.example.wallquote.data.background.FileBackgroundAssetStore
import com.example.wallquote.domain.background.BackgroundAssetId
import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.background.BackgroundLimits
import com.example.wallquote.domain.background.CenterCropCalculator
import com.example.wallquote.wallpaper.WallpaperDiagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt

class DefaultBackgroundImageLoader(
    private val assetStore: BackgroundAssetStore,
    private val cache: BackgroundBitmapCache = BackgroundBitmapCache(),
    private val diagnostics: WallpaperDiagnostics? = null,
) : BackgroundImageLoader {

    override suspend fun load(
        assetId: String,
        targetWidth: Int,
        targetHeight: Int,
        blurRadiusDp: Float,
        density: Float,
    ): BackgroundImageResult = withContext(Dispatchers.Default) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            return@withContext BackgroundImageResult.Failed(BackgroundImageFailure.Unknown)
        }
        val blurBucket = BackgroundBitmapCache.blurBucket(blurRadiusDp)
        val key = BackgroundImageKey(assetId, targetWidth, targetHeight, blurBucket)
        cache.get(key)?.let {
            diagnostics?.log("background_cache_hit", mapOf("assetId" to assetId))
            return@withContext BackgroundImageResult.Success(key, it)
        }
        diagnostics?.log("background_cache_miss", mapOf("assetId" to assetId))
        diagnostics?.log(
            "background_decode_started",
            mapOf("assetId" to assetId, "w" to targetWidth, "h" to targetHeight),
        )

        val path = assetStore.resolvePath(BackgroundAssetId(assetId))
        if (path == null) {
            diagnostics?.log("background_asset_missing", mapOf("assetId" to assetId))
            return@withContext BackgroundImageResult.Failed(BackgroundImageFailure.Missing)
        }

        try {
            coroutineContext.ensureActive()
            val bitmap = decodeCenterCropped(path, targetWidth, targetHeight)
                ?: return@withContext BackgroundImageResult.Failed(BackgroundImageFailure.DecodeFailed)

            val blurred = if (blurRadiusDp > 0f) {
                diagnostics?.log("blur_started", mapOf("assetId" to assetId, "radiusDp" to blurRadiusDp))
                val radiusPx = (blurRadiusDp * density).roundToInt().coerceIn(1, 25)
                val result = StackBlur.blur(bitmap, radiusPx)
                if (result !== bitmap) bitmap.recycle()
                diagnostics?.log("blur_completed", mapOf("assetId" to assetId))
                result
            } else {
                bitmap
            }

            cache.put(key, blurred)
            diagnostics?.log("background_decode_completed", mapOf("assetId" to assetId))
            BackgroundImageResult.Success(key, blurred)
        } catch (_: OutOfMemoryError) {
            cache.trimToHalf()
            diagnostics?.log("background_decode_failed", mapOf("assetId" to assetId, "error" to "oom"))
            // One retry at half resolution.
            return@withContext try {
                val halfW = max(1, targetWidth / 2)
                val halfH = max(1, targetHeight / 2)
                val halfKey = BackgroundImageKey(assetId, halfW, halfH, blurBucket)
                cache.get(halfKey)?.let {
                    return@withContext BackgroundImageResult.Success(halfKey, it)
                }
                val pathRetry = assetStore.resolvePath(BackgroundAssetId(assetId))
                    ?: return@withContext BackgroundImageResult.Failed(BackgroundImageFailure.Missing)
                val bitmap = decodeCenterCropped(pathRetry, halfW, halfH)
                    ?: return@withContext BackgroundImageResult.Failed(BackgroundImageFailure.DecodeFailed)
                cache.put(halfKey, bitmap)
                BackgroundImageResult.Success(halfKey, bitmap)
            } catch (_: OutOfMemoryError) {
                BackgroundImageResult.Failed(BackgroundImageFailure.OutOfMemory)
            }
        } catch (_: kotlinx.coroutines.CancellationException) {
            BackgroundImageResult.Failed(BackgroundImageFailure.Cancelled)
        } catch (error: Throwable) {
            diagnostics?.log(
                "background_decode_failed",
                mapOf("assetId" to assetId, "error" to error.javaClass.simpleName),
            )
            BackgroundImageResult.Failed(BackgroundImageFailure.Unknown)
        }
    }

    override fun trimMemory() {
        cache.trimToHalf()
    }

    private fun decodeCenterCropped(
        path: String,
        targetWidth: Int,
        targetHeight: Int,
    ): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val sample = FileBackgroundAssetStore.largestSampleSize(
            bounds.outWidth,
            bounds.outHeight,
            targetWidth,
            targetHeight,
        ).coerceAtLeast(1)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(path, options) ?: return null
        val crop = CenterCropCalculator.calculate(
            sourceWidth = decoded.width,
            sourceHeight = decoded.height,
            targetWidth = targetWidth,
            targetHeight = targetHeight,
        ) ?: run {
            decoded.recycle()
            return null
        }

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(
            decoded,
            android.graphics.Rect(
                crop.srcLeft.roundToInt(),
                crop.srcTop.roundToInt(),
                crop.srcRight.roundToInt(),
                crop.srcBottom.roundToInt(),
            ),
            android.graphics.Rect(
                crop.dstLeft.roundToInt(),
                crop.dstTop.roundToInt(),
                crop.dstRight.roundToInt(),
                crop.dstBottom.roundToInt(),
            ),
            paint,
        )
        decoded.recycle()
        return output
    }
}

/**
 * Compact stack blur (Mario Klingemann), radius clamped for wallpaper backgrounds.
 */
internal object StackBlur {
    fun blur(sentBitmap: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return sentBitmap
        val bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        val vmin = IntArray(max(w, h))

        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }

        yi = 0
        var yw = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + minOf(wm, maxOf(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) vmin[x] = minOf(x + radius + 1, wm)
                p = pix[yw + vmin[x]]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = maxOf(0, yp) + x
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - kotlin.math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) yp += w
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = (0xff000000.toInt() and pix[yi]) or
                    (dv[rsum] shl 16) or
                    (dv[gsum] shl 8) or
                    dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) vmin[y] = minOf(y + r1, hm) * w
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
            x++
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }
}
