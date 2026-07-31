package com.example.wallquote.wallpaper.background

import com.example.wallquote.domain.background.BackgroundAssetId
import com.example.wallquote.domain.background.BackgroundAssetStore
import com.example.wallquote.domain.background.ProcessedImageSizeCalculator
import com.example.wallquote.wallpaper.WallpaperDiagnostics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultBackgroundImageLoader @Inject constructor(
    private val assetStore: BackgroundAssetStore,
    private val processor: BackgroundImageProcessor,
    private val cache: BackgroundBitmapCache,
) : BackgroundImageLoader, BackgroundMemoryController {

    override suspend fun load(
        assetId: String,
        targetWidth: Int,
        targetHeight: Int,
        blurRadiusDp: Float,
        density: Float,
        diagnostics: WallpaperDiagnostics?,
    ): BackgroundImageResult = withContext(Dispatchers.Default) {
        if (targetWidth <= 0 || targetHeight <= 0) {
            return@withContext BackgroundImageResult.Failed(BackgroundImageFailure.Unknown)
        }
        val planned = ProcessedImageSizeCalculator.calculate(targetWidth, targetHeight, blurRadiusDp)
        val blurPx = ProcessedImageSizeCalculator.effectiveBlurRadiusPx(blurRadiusDp, density)
        val plannedKey = BackgroundImageKey(
            assetId = assetId,
            processedWidth = planned.width,
            processedHeight = planned.height,
            effectiveBlurRadiusPx = blurPx,
        )
        cache.get(plannedKey)?.let {
            diagnostics?.log("background_cache_hit", mapOf("assetId" to assetId))
            return@withContext BackgroundImageResult.Success(plannedKey, it)
        }
        diagnostics?.log("background_cache_miss", mapOf("assetId" to assetId))
        diagnostics?.log(
            "background_decode_started",
            mapOf(
                "assetId" to assetId,
                "w" to planned.width,
                "h" to planned.height,
                "blurPx" to blurPx,
            ),
        )

        val path = assetStore.resolvePath(BackgroundAssetId(assetId))
        if (path == null) {
            diagnostics?.log("background_asset_missing", mapOf("assetId" to assetId))
            return@withContext BackgroundImageResult.Failed(BackgroundImageFailure.Missing)
        }

        try {
            coroutineContext.ensureActive()
            val result = processor.process(
                sourcePath = path,
                targetWidth = targetWidth,
                targetHeight = targetHeight,
                blurRadiusDp = blurRadiusDp,
                density = density,
            )
            val resultKey = BackgroundImageKey(
                assetId = assetId,
                processedWidth = result.processedWidth,
                processedHeight = result.processedHeight,
                effectiveBlurRadiusPx = result.effectiveBlurRadiusPx,
            )
            cache.put(resultKey, result.bitmap)
            diagnostics?.log("background_decode_completed", mapOf("assetId" to assetId))
            BackgroundImageResult.Success(resultKey, result.bitmap)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: OutOfMemoryError) {
            cache.trimToHalf()
            diagnostics?.log("background_decode_failed", mapOf("assetId" to assetId, "error" to "oom"))
            BackgroundImageResult.Failed(BackgroundImageFailure.OutOfMemory)
        } catch (error: Throwable) {
            diagnostics?.log(
                "background_decode_failed",
                mapOf("assetId" to assetId, "error" to error.javaClass.simpleName),
            )
            if (error.message == "decode_failed") {
                BackgroundImageResult.Failed(BackgroundImageFailure.DecodeFailed)
            } else {
                BackgroundImageResult.Failed(BackgroundImageFailure.Unknown)
            }
        }
    }

    override fun trimToHalf() {
        cache.trimToHalf()
    }

    override fun evictAll() {
        cache.evictAll()
    }
}
