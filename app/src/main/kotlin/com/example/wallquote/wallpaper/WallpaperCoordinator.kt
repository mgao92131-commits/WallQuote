package com.example.wallquote.wallpaper

import android.view.SurfaceHolder
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.PlaybackState
import com.example.wallquote.domain.wallpaper.PlaybackController
import com.example.wallquote.domain.wallpaper.ScheduleBoundaryCalculator
import com.example.wallquote.domain.wallpaper.WallpaperRenderSpecFactory
import com.example.wallquote.wallpaper.background.BackgroundBitmapCache
import com.example.wallquote.wallpaper.background.BackgroundImageLoader
import com.example.wallquote.wallpaper.background.BackgroundImageResult
import com.example.wallquote.wallpaper.background.BackgroundLoadToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/**
 * Single-consumer event loop for wallpaper playback and rendering.
 */
class WallpaperCoordinator(
    private val scope: CoroutineScope,
    private val clock: WallpaperClock,
    private val renderer: WallpaperRenderTarget,
    private val boundaryScheduler: BoundaryScheduler,
    private val diagnostics: WallpaperDiagnostics,
    private val imageLoader: BackgroundImageLoader? = null,
    private val density: Float = 1f,
    private val hideAdvanceThresholdMillis: Long = 30_000L,
    private val showEmptyHint: Boolean = true,
) {
    private val eventChannel = Channel<WallpaperEvent>(Channel.UNLIMITED)
    private var loopJob: Job? = null

    private var collections: List<CollectionConfig> = emptyList()
    private var playbackState: PlaybackState = PlaybackState()
    private var holder: SurfaceHolder? = null
    private var surfaceWidth: Int = 0
    private var surfaceHeight: Int = 0
    private var surfaceAvailable: Boolean = false
    private var surfaceGeneration: Long = 0
    private var visible: Boolean = false
    private var hiddenAtElapsedMillis: Long? = null
    /** Guards against double-advance if visibility and other reveal events race. */
    private var advancedForCurrentReveal: Boolean = false
    private var destroyed: Boolean = false
    private var rendering: Boolean = false

    private var backgroundLoadJob: Job? = null
    private var pendingBackgroundToken: BackgroundLoadToken? = null
    private var loadedBackground: PreparedPhotoFrame? = null
    private var loadedAssetId: String? = null
    private var loadedBlurBucket: Int = -1

    fun start() {
        if (loopJob != null) return
        diagnostics.log("engine_created")
        loopJob = scope.launch {
            for (event in eventChannel) {
                handleEvent(event)
                if (destroyed) break
            }
        }
    }

    fun offer(event: WallpaperEvent) {
        if (destroyed && event !is WallpaperEvent.Destroy) return
        eventChannel.trySend(event)
    }

    /**
     * Synchronously tear down coordinator state. Call before cancelling the coroutine scope.
     */
    fun close() {
        if (destroyed) return
        destroyed = true
        cancelBackgroundLoad(reason = "engine_destroyed")
        boundaryScheduler.cancel()
        surfaceAvailable = false
        holder = null
        loadedBackground = null
        loadedAssetId = null
        loadedBlurBucket = -1
        diagnostics.log("engine_destroyed")
        eventChannel.close()
    }

    fun trimMemory() {
        imageLoader?.trimMemory()
    }

    private fun handleEvent(event: WallpaperEvent) {
        when (event) {
            is WallpaperEvent.SurfaceCreated -> onSurfaceCreated(event.holder)
            is WallpaperEvent.SurfaceChanged -> onSurfaceChanged(event.holder, event.width, event.height)
            WallpaperEvent.SurfaceDestroyed -> onSurfaceDestroyed()
            is WallpaperEvent.VisibilityChanged -> onVisibilityChanged(event.visible)
            is WallpaperEvent.CollectionsChanged -> onCollectionsChanged(event.collections)
            WallpaperEvent.ScreenOn -> onScreenOn()
            WallpaperEvent.ScheduleBoundaryReached -> onScheduleBoundaryReached()
            WallpaperEvent.TimeOrZoneChanged -> onTimeOrZoneChanged()
            WallpaperEvent.Destroy -> close()
            is WallpaperEvent.BackgroundLoadCompleted -> onBackgroundLoadCompleted(event.token, event.result)
        }
    }

    private fun onSurfaceCreated(newHolder: SurfaceHolder) {
        holder = newHolder
        surfaceAvailable = true
        surfaceGeneration += 1
        diagnostics.log("surface_created", mapOf("gen" to surfaceGeneration))
        syncOnly(source = "surface_created")
        ensureBackgroundAndRender(source = "surface_created")
    }

    private fun onSurfaceChanged(newHolder: SurfaceHolder, width: Int, height: Int) {
        holder = newHolder
        val sizeChanged = surfaceWidth != width || surfaceHeight != height
        surfaceWidth = width
        surfaceHeight = height
        surfaceAvailable = true
        diagnostics.log(
            "surface_changed",
            mapOf("gen" to surfaceGeneration, "w" to width, "h" to height),
        )
        if (sizeChanged) {
            loadedBackground = null
            loadedAssetId = null
            loadedBlurBucket = -1
        }
        syncOnly(source = "surface_changed")
        ensureBackgroundAndRender(source = "surface_changed")
    }

    private fun onSurfaceDestroyed() {
        diagnostics.log("surface_destroyed", mapOf("gen" to surfaceGeneration))
        cancelBackgroundLoad(reason = "surface_destroyed")
        surfaceAvailable = false
        holder = null
        surfaceGeneration += 1
        loadedBackground = null
        loadedAssetId = null
        loadedBlurBucket = -1
    }

    private fun onVisibilityChanged(nowVisible: Boolean) {
        diagnostics.log("visibility_changed", mapOf("visible" to nowVisible))
        if (!nowVisible) {
            visible = false
            hiddenAtElapsedMillis = clock.elapsedRealtimeMillis()
            advancedForCurrentReveal = false
            boundaryScheduler.cancel()
            return
        }

        val hiddenFor = hiddenAtElapsedMillis?.let { clock.elapsedRealtimeMillis() - it } ?: 0L
        hiddenAtElapsedMillis = null
        visible = true

        if (hiddenFor >= hideAdvanceThresholdMillis) {
            if (!advancedForCurrentReveal) {
                advancedForCurrentReveal = true
                advanceOnce(source = "visibility_long_hide")
            } else {
                syncOnly(source = "visibility_deduped")
            }
        } else {
            syncOnly(source = "visibility_short_hide")
        }
        scheduleNextBoundary()
        ensureBackgroundAndRender(source = "visibility_changed")
    }

    private fun onScreenOn() {
        diagnostics.log("screen_on")
        syncOnly(source = "screen_on")
        if (visible) {
            ensureBackgroundAndRender(source = "screen_on")
        }
    }

    private fun onTimeOrZoneChanged() {
        diagnostics.log("time_or_zone_changed")
        syncOnly(source = "time_or_zone_changed")
        scheduleNextBoundary()
        if (visible) {
            ensureBackgroundAndRender(source = "time_or_zone_changed")
        }
    }

    private fun onCollectionsChanged(next: List<CollectionConfig>) {
        collections = next
        diagnostics.log(
            "collections_snapshot_changed",
            mapOf("count" to next.size),
        )
        syncOnly(source = "collections_changed")
        scheduleNextBoundary()
        if (visible) {
            ensureBackgroundAndRender(source = "collections_changed")
        }
    }

    private fun onScheduleBoundaryReached() {
        diagnostics.log("schedule_boundary_reached")
        syncOnly(source = "schedule_boundary")
        scheduleNextBoundary()
        if (visible) {
            ensureBackgroundAndRender(source = "schedule_boundary")
        }
    }

    private fun onBackgroundLoadCompleted(token: BackgroundLoadToken, result: BackgroundImageResult) {
        val pending = pendingBackgroundToken
        if (pending == null || pending != token || destroyed || !surfaceAvailable) {
            diagnostics.log("background_load_result_stale", mapOf("assetId" to token.assetId))
            return
        }
        pendingBackgroundToken = null
        when (result) {
            is BackgroundImageResult.Success -> {
                val photo = currentPhotoBackground()
                if (photo == null || photo.assetId != token.assetId) {
                    diagnostics.log("background_load_result_stale", mapOf("assetId" to token.assetId))
                    return
                }
                loadedBackground = PreparedPhotoFrame(
                    bitmap = result.bitmap,
                    dimAmount = photo.dimAmount,
                )
                loadedAssetId = token.assetId
                loadedBlurBucket = token.blurRadiusBucket
                renderIfPossible(source = "background_ready")
            }
            is BackgroundImageResult.Failed -> {
                diagnostics.log(
                    "background_decode_failed",
                    mapOf("assetId" to token.assetId, "reason" to result.reason.name),
                )
                loadedBackground = null
                loadedAssetId = null
                loadedBlurBucket = -1
                renderIfPossible(source = "background_failed")
            }
        }
    }

    private fun syncOnly(source: String) {
        val previous = playbackState
        playbackState = PlaybackController.sync(
            collections = collections,
            minuteOfDay = clock.minuteOfDay(),
            previous = previous,
        )
        logCursorIfChanged(previous, source)
    }

    private fun advanceOnce(source: String) {
        val previous = playbackState
        playbackState = PlaybackController.advance(
            collections = collections,
            minuteOfDay = clock.minuteOfDay(),
            previous = previous,
            nowMillis = clock.currentWallTimeMillis(),
        )
        logCursorIfChanged(previous, source)
    }

    private fun logCursorIfChanged(previous: PlaybackState, source: String) {
        if (previous.cursor != playbackState.cursor ||
            previous.activeCollectionIds != playbackState.activeCollectionIds
        ) {
            diagnostics.log(
                "playback_cursor_changed",
                mapOf(
                    "source" to source,
                    "collectionId" to playbackState.cursor.collectionId,
                    "quoteLineId" to playbackState.cursor.quoteLineId,
                    "activeCount" to playbackState.activeCollectionIds.size,
                ),
            )
            diagnostics.log(
                "active_collections_changed",
                mapOf("ids" to playbackState.activeCollectionIds),
            )
        }
    }

    private fun scheduleNextBoundary() {
        boundaryScheduler.cancel()
        if (!visible || destroyed) return
        val delayMillis = ScheduleBoundaryCalculator.millisUntilNextBoundary(
            collections = collections,
            minuteOfDay = clock.minuteOfDay(),
            secondOfMinute = clock.secondOfMinute(),
            millisOfSecond = clock.millisOfSecond(),
        ) ?: return
        diagnostics.log(
            "schedule_boundary_scheduled",
            mapOf("delayMillis" to delayMillis),
        )
        boundaryScheduler.scheduleAfterMillis(delayMillis) {
            offer(WallpaperEvent.ScheduleBoundaryReached)
        }
    }

    private fun ensureBackgroundAndRender(source: String) {
        val photo = currentPhotoBackground()
        if (photo == null) {
            cancelBackgroundLoad(reason = "non_photo")
            loadedBackground = null
            loadedAssetId = null
            loadedBlurBucket = -1
            renderIfPossible(source = source)
            return
        }
        val blurBucket = BackgroundBitmapCache.blurBucket(photo.blurRadiusDp)
        if (loadedAssetId == photo.assetId &&
            loadedBlurBucket == blurBucket &&
            loadedBackground != null
        ) {
            loadedBackground = loadedBackground?.copy(dimAmount = photo.dimAmount)
            renderIfPossible(source = source)
            return
        }
        // Draw fallback immediately, then load asynchronously.
        loadedBackground = null
        loadedAssetId = null
        loadedBlurBucket = -1
        renderIfPossible(source = source)
        maybeStartBackgroundLoad(photo, force = true)
    }

    private fun maybeStartBackgroundLoad(photo: BackgroundSpec.Photo, force: Boolean) {
        val loader = imageLoader ?: return
        if (surfaceWidth <= 0 || surfaceHeight <= 0 || destroyed || !surfaceAvailable) return
        val blurBucket = BackgroundBitmapCache.blurBucket(photo.blurRadiusDp)
        val token = BackgroundLoadToken(
            surfaceGeneration = surfaceGeneration,
            collectionId = playbackState.cursor.collectionId ?: -1L,
            assetId = photo.assetId,
            targetWidth = surfaceWidth,
            targetHeight = surfaceHeight,
            blurRadiusBucket = blurBucket,
        )
        if (!force && pendingBackgroundToken == token) return
        cancelBackgroundLoad(reason = "new_request")
        pendingBackgroundToken = token
        backgroundLoadJob = scope.launch {
            val result = loader.load(
                assetId = photo.assetId,
                targetWidth = surfaceWidth,
                targetHeight = surfaceHeight,
                blurRadiusDp = photo.blurRadiusDp,
                density = density,
            )
            offer(WallpaperEvent.BackgroundLoadCompleted(token, result))
        }
    }

    private fun cancelBackgroundLoad(reason: String) {
        if (backgroundLoadJob != null) {
            diagnostics.log("background_load_cancelled", mapOf("reason" to reason))
        }
        backgroundLoadJob?.cancel()
        backgroundLoadJob = null
        pendingBackgroundToken = null
    }

    private fun currentPhotoBackground(): BackgroundSpec.Photo? {
        val spec = WallpaperRenderSpecFactory.fromState(
            collections = collections,
            state = playbackState,
            showEmptyHint = false,
        )
        return spec.background as? BackgroundSpec.Photo
    }

    private fun renderIfPossible(source: String) {
        if (destroyed || !surfaceAvailable || rendering) return
        if (surfaceWidth <= 0 || surfaceHeight <= 0) return
        val currentHolder = holder ?: return
        val generation = surfaceGeneration
        rendering = true
        try {
            val spec = WallpaperRenderSpecFactory.fromState(
                collections = collections,
                state = playbackState,
                showEmptyHint = showEmptyHint && collections.isEmpty(),
            )
            val photoFrame = when (spec.background) {
                is BackgroundSpec.Photo -> loadedBackground
                else -> null
            }
            renderer.render(
                holder = currentHolder,
                surfaceWidth = surfaceWidth,
                surfaceHeight = surfaceHeight,
                renderSpec = spec,
                surfaceGeneration = generation,
                preparedPhoto = photoFrame,
            )
        } finally {
            rendering = false
        }
    }

    /** Test hooks */
    internal fun currentState(): PlaybackState = playbackState
    internal fun isSurfaceAvailable(): Boolean = surfaceAvailable
    internal fun isDestroyed(): Boolean = destroyed
    internal fun loadedAssetIdForTest(): String? = loadedAssetId
}
