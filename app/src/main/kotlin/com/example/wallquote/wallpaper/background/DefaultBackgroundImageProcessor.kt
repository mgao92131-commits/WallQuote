package com.example.wallquote.wallpaper.background

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.example.wallquote.data.background.FileBackgroundAssetStore
import com.example.wallquote.domain.background.CenterCropCalculator
import com.example.wallquote.domain.background.ProcessedImageSize
import com.example.wallquote.domain.background.ProcessedImageSizeCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.math.max
import kotlin.math.roundToInt
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBackgroundImageProcessor @Inject constructor() : BackgroundImageProcessor {

    override suspend fun process(
        sourcePath: String,
        targetWidth: Int,
        targetHeight: Int,
        blurRadiusDp: Float,
        density: Float,
    ): ProcessedBackgroundImage = withContext(Dispatchers.Default) {
        coroutineContext.ensureActive()
        var size = ProcessedImageSizeCalculator.calculate(targetWidth, targetHeight, blurRadiusDp)
        var lastError: OutOfMemoryError? = null
        repeat(2) { attempt ->
            try {
                coroutineContext.ensureActive()
                val bitmap = decodeCenterCropped(sourcePath, size.width, size.height)
                    ?: error("decode_failed")
                val blurPx = ProcessedImageSizeCalculator.effectiveBlurRadiusPx(blurRadiusDp, density)
                val processed = if (blurPx > 0) {
                    val blurred = StackBlur.blur(bitmap, blurPx)
                    if (blurred !== bitmap) bitmap.recycle()
                    blurred
                } else {
                    bitmap
                }
                return@withContext ProcessedBackgroundImage(
                    bitmap = processed,
                    processedWidth = size.width,
                    processedHeight = size.height,
                    effectiveBlurRadiusPx = blurPx,
                )
            } catch (oom: OutOfMemoryError) {
                lastError = oom
                size = ProcessedImageSize(
                    width = max(1, size.width / 2),
                    height = max(1, size.height / 2),
                )
            }
        }
        throw lastError ?: OutOfMemoryError("background_process_oom")
    }

    private fun decodeCenterCropped(
        path: String,
        targetWidth: Int,
        targetHeight: Int,
    ): Bitmap? {
        if (targetWidth <= 0 || targetHeight <= 0) return null
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
