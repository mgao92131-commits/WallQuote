package com.example.wallquote.wallpaper

import android.view.SurfaceHolder
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.PlaybackState
import com.example.wallquote.domain.model.WallpaperRenderSpec
import com.example.wallquote.domain.wallpaper.PlaybackController
import com.example.wallquote.domain.wallpaper.ScheduleBoundaryCalculator
import com.example.wallquote.domain.wallpaper.WallpaperRenderSpecFactory
import com.example.wallquote.wallpaper.background.BackgroundImageLoader
import com.example.wallquote.wallpaper.background.BackgroundImageResult
import com.example.wallquote.wallpaper.background.BackgroundLoadToken
import com.example.wallquote.domain.background.ProcessedImageSizeCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

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
    private val transitionDriver: TransitionDriver = WallpaperTransitionDriver(),
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
    private var loadedBlurPx: Int = -1
    private var loadedProcessedWidth: Int = -1
    private var loadedProcessedHeight: Int = -1

    // --- 288ms advance fade transition state (see WallpaperTransitionDriver) ---
    private var transitionPrepareJob: Job? = null
    private var transitionFromSpec: WallpaperRenderSpec? = null
    private var transitionToSpec: WallpaperRenderSpec? = null
    private var transitionToPhotoFrame: PreparedPhotoFrame? = null
    private var transitionShowTarget: Boolean = false
    private var transitionTextAlpha: Float = 1f
    private val isTransitioning: Boolean
        get() = transitionFromSpec != null

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
        cancelTransition(reason = "engine_destroyed", adoptTarget = false)
        cancelBackgroundLoad(reason = "engine_destroyed")
        boundaryScheduler.cancel()
        surfaceAvailable = false
        holder = null
        loadedBackground = null
        loadedAssetId = null
        loadedBlurPx = -1
        loadedProcessedWidth = -1
        loadedProcessedHeight = -1
        diagnostics.log("engine_destroyed")
        eventChannel.close()
    }

    fun trimMemory() {
        imageLoader?.trimToHalf()
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
            cancelTransition(reason = "surface_size_changed", adoptTarget = false)
            loadedBackground = null
            loadedAssetId = null
            loadedBlurPx = -1
            loadedProcessedWidth = -1
            loadedProcessedHeight = -1
        }
        syncOnly(source = "surface_changed")
        ensureBackgroundAndRender(source = "surface_changed")
    }

    private fun onSurfaceDestroyed() {
        diagnostics.log("surface_destroyed", mapOf("gen" to surfaceGeneration))
        cancelTransition(reason = "surface_destroyed", adoptTarget = false)
        cancelBackgroundLoad(reason = "surface_destroyed")
        surfaceAvailable = false
        holder = null
        surfaceGeneration += 1
        loadedBackground = null
        loadedAssetId = null
        loadedBlurPx = -1
        loadedProcessedWidth = -1
        loadedProcessedHeight = -1
    }

    private fun onVisibilityChanged(nowVisible: Boolean) {
        diagnostics.log("visibility_changed", mapOf("visible" to nowVisible))
        if (!nowVisible) {
            visible = false
            hiddenAtElapsedMillis = clock.elapsedRealtimeMillis()
            advancedForCurrentReveal = false
            boundaryScheduler.cancel()
            // Stop animating while hidden; the cursor already points at the (adopted) target.
            cancelTransition(reason = "hide", adoptTarget = true)
            return
        }

        val hiddenFor = hiddenAtElapsedMillis?.let { clock.elapsedRealtimeMillis() - it } ?: 0L
        hiddenAtElapsedMillis = null
        visible = true

        if (hiddenFor >= hideAdvanceThresholdMillis) {
            if (!advancedForCurrentReveal) {
                advancedForCurrentReveal = true
                advanceWithTransition(source = "visibility_long_hide")
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
        // Collections changed underneath the in-flight transition; abandon it and resync/render
        // immediately from the fresh data instead of continuing to animate toward a stale target.
        cancelTransition(reason = "collections_changed", adoptTarget = true)
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
        // Boundary crossings only sync (recompute active collections); they never advance the
        // quote cursor, so any in-flight advance transition is cancelled rather than continued.
        cancelTransition(reason = "schedule_boundary", adoptTarget = true)
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
                loadedBlurPx = token.effectiveBlurRadiusPx
                loadedProcessedWidth = token.processedWidth
                loadedProcessedHeight = token.processedHeight
                renderIfPossible(source = "background_ready")
            }
            is BackgroundImageResult.Failed -> {
                diagnostics.log(
                    "background_decode_failed",
                    mapOf("assetId" to token.assetId, "reason" to result.reason.name),
                )
                loadedBackground = null
                loadedAssetId = null
                loadedBlurPx = -1
                loadedProcessedWidth = -1
                loadedProcessedHeight = -1
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

    /**
     * Advances the cursor and animates the visible change over 288ms via [transitionDriver].
     * If a transition is already in-flight, it is cancelled and its (already-adopted) target
     * cursor becomes the new "from" state, i.e. the animation restarts from where it currently is.
     */
    private fun advanceWithTransition(source: String) {
        cancelTransition(reason = "advance_restart", adoptTarget = true)
        val previousState = playbackState
        val fromSpec = WallpaperRenderSpecFactory.fromState(collections, previousState, showEmptyHint = false)
        advanceOnce(source)
        if (playbackState.cursor == previousState.cursor) {
            // Nothing to animate (e.g. single-quote playlist, or playlist became empty).
            renderIfPossible(source = source)
            return
        }
        val toSpec = WallpaperRenderSpecFactory.fromState(collections, playbackState, showEmptyHint = false)
        beginTransition(source, fromSpec, toSpec)
    }

    private fun beginTransition(
        source: String,
        fromSpec: WallpaperRenderSpec,
        toSpec: WallpaperRenderSpec,
    ) {
        transitionDriver.cancel()
        transitionPrepareJob?.cancel()
        transitionFromSpec = fromSpec
        transitionToSpec = toSpec
        transitionToPhotoFrame = null
        transitionShowTarget = false
        transitionTextAlpha = 1f
        diagnostics.log("transition_started", mapOf("source" to source))

        val targetPhoto = toSpec.background as? BackgroundSpec.Photo
        val loader = imageLoader
        if (targetPhoto == null || loader == null || surfaceWidth <= 0 || surfaceHeight <= 0) {
            startTransitionAnimation(source)
            return
        }
        // Photo target: wait briefly for a cache hit / decode, else proceed with a fallback frame
        // and let the normal background-load path fill it in once the transition completes.
        transitionPrepareJob = scope.launch {
            val result = withTimeoutOrNull(TRANSITION_PHOTO_WAIT_MILLIS) {
                loader.load(
                    assetId = targetPhoto.assetId,
                    targetWidth = surfaceWidth,
                    targetHeight = surfaceHeight,
                    blurRadiusDp = targetPhoto.blurRadiusDp,
                    density = density,
                )
            }
            if (transitionToSpec !== toSpec) return@launch // superseded by a newer transition
            if (result is BackgroundImageResult.Success) {
                transitionToPhotoFrame = PreparedPhotoFrame(result.bitmap, targetPhoto.dimAmount)
            } else {
                diagnostics.log("transition_photo_wait_timeout", mapOf("assetId" to targetPhoto.assetId))
            }
            startTransitionAnimation(source)
        }
    }

    private fun startTransitionAnimation(source: String) {
        if (destroyed) return
        transitionDriver.start(
            from = {
                renderIfPossible(source = "$source:transition_from")
            },
            to = {
                transitionShowTarget = true
                adoptTransitionTarget()
                renderIfPossible(source = "$source:transition_switch")
            },
            onFrame = { alpha ->
                transitionTextAlpha = alpha
                renderIfPossible(source = "$source:transition_frame")
            },
            onComplete = {
                transitionFromSpec = null
                transitionToSpec = null
                transitionToPhotoFrame = null
                transitionShowTarget = false
                transitionTextAlpha = 1f
                diagnostics.log("transition_completed")
                ensureBackgroundAndRender(source = "$source:transition_complete")
            },
        )
    }

    /** Cancels any in-flight transition. If [adoptTarget], the target cursor's background bookkeeping
     * (loadedBackground/loadedAssetId/...) is adopted immediately so the next normal render shows it. */
    private fun cancelTransition(reason: String, adoptTarget: Boolean) {
        if (!isTransitioning) return
        transitionDriver.cancel()
        transitionPrepareJob?.cancel()
        transitionPrepareJob = null
        diagnostics.log("transition_cancelled", mapOf("reason" to reason))
        if (adoptTarget) {
            adoptTransitionTarget()
        }
        transitionFromSpec = null
        transitionToSpec = null
        transitionToPhotoFrame = null
        transitionShowTarget = false
        transitionTextAlpha = 1f
    }

    private fun adoptTransitionTarget() {
        val toSpec = transitionToSpec ?: return
        val photo = toSpec.background as? BackgroundSpec.Photo
        val frame = transitionToPhotoFrame
        if (photo != null && frame != null) {
            val plan = photoLoadPlan(photo)
            loadedBackground = frame
            loadedAssetId = photo.assetId
            loadedBlurPx = plan.blurPx
            loadedProcessedWidth = plan.width
            loadedProcessedHeight = plan.height
        } else {
            loadedBackground = null
            loadedAssetId = null
            loadedBlurPx = -1
            loadedProcessedWidth = -1
            loadedProcessedHeight = -1
        }
    }

    private data class PhotoLoadPlan(val width: Int, val height: Int, val blurPx: Int)

    private fun photoLoadPlan(photo: BackgroundSpec.Photo): PhotoLoadPlan {
        val planned = ProcessedImageSizeCalculator.calculate(surfaceWidth, surfaceHeight, photo.blurRadiusDp)
        val blurPx = ProcessedImageSizeCalculator.effectiveBlurRadiusPx(photo.blurRadiusDp, density)
        return PhotoLoadPlan(planned.width, planned.height, blurPx)
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
        if (isTransitioning) {
            // The transition owns loadedBackground/from-to bookkeeping until it completes or is
            // cancelled; just repaint the current transition frame instead of touching it.
            renderIfPossible(source = source)
            return
        }
        val photo = currentPhotoBackground()
        if (photo == null) {
            cancelBackgroundLoad(reason = "non_photo")
            loadedBackground = null
            loadedAssetId = null
            loadedBlurPx = -1
            loadedProcessedWidth = -1
            loadedProcessedHeight = -1
            renderIfPossible(source = source)
            return
        }
        val planned = ProcessedImageSizeCalculator.calculate(
            surfaceWidth,
            surfaceHeight,
            photo.blurRadiusDp,
        )
        val blurPx = ProcessedImageSizeCalculator.effectiveBlurRadiusPx(photo.blurRadiusDp, density)
        if (loadedAssetId == photo.assetId &&
            loadedBlurPx == blurPx &&
            loadedProcessedWidth == planned.width &&
            loadedProcessedHeight == planned.height &&
            loadedBackground != null
        ) {
            // Dim-only change: redraw without re-decoding.
            loadedBackground = loadedBackground?.copy(dimAmount = photo.dimAmount)
            renderIfPossible(source = source)
            return
        }
        // Draw fallback immediately, then load asynchronously.
        loadedBackground = null
        loadedAssetId = null
        loadedBlurPx = -1
        loadedProcessedWidth = -1
        loadedProcessedHeight = -1
        renderIfPossible(source = source)
        maybeStartBackgroundLoad(photo)
    }

    private fun maybeStartBackgroundLoad(photo: BackgroundSpec.Photo) {
        val loader = imageLoader ?: return
        if (surfaceWidth <= 0 || surfaceHeight <= 0 || destroyed || !surfaceAvailable) return
        val planned = ProcessedImageSizeCalculator.calculate(
            surfaceWidth,
            surfaceHeight,
            photo.blurRadiusDp,
        )
        val blurPx = ProcessedImageSizeCalculator.effectiveBlurRadiusPx(photo.blurRadiusDp, density)
        val token = BackgroundLoadToken(
            surfaceGeneration = surfaceGeneration,
            collectionId = playbackState.cursor.collectionId ?: -1L,
            assetId = photo.assetId,
            processedWidth = planned.width,
            processedHeight = planned.height,
            effectiveBlurRadiusPx = blurPx,
        )
        if (pendingBackgroundToken == token) return
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
            val transFrom = transitionFromSpec
            val transTo = transitionToSpec
            val spec: WallpaperRenderSpec
            val photoFrame: PreparedPhotoFrame?
            if (transFrom != null && transTo != null) {
                val base = if (transitionShowTarget) transTo else transFrom
                spec = base.copy(transitionTextAlpha = transitionTextAlpha)
                photoFrame = if (base.background is BackgroundSpec.Photo) {
                    if (transitionShowTarget) transitionToPhotoFrame else loadedBackground
                } else {
                    null
                }
            } else {
                spec = WallpaperRenderSpecFactory.fromState(
                    collections = collections,
                    state = playbackState,
                    showEmptyHint = showEmptyHint && collections.isEmpty(),
                )
                photoFrame = when (spec.background) {
                    is BackgroundSpec.Photo -> loadedBackground
                    else -> null
                }
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
    internal fun isTransitioningForTest(): Boolean = isTransitioning

    private companion object {
        const val TRANSITION_PHOTO_WAIT_MILLIS = 300L
    }
}
