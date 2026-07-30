package com.example.wallquote.wallpaper

import android.view.SurfaceHolder
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.PlaybackState
import com.example.wallquote.domain.wallpaper.PlaybackController
import com.example.wallquote.domain.wallpaper.ScheduleBoundaryCalculator
import com.example.wallquote.domain.wallpaper.WallpaperRenderSpecFactory
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
    private var hiddenAtMillis: Long? = null
    /** Guards against double-advance if visibility and other reveal events race. */
    private var advancedForCurrentReveal: Boolean = false
    private var destroyed: Boolean = false
    private var rendering: Boolean = false

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

    private fun handleEvent(event: WallpaperEvent) {
        when (event) {
            is WallpaperEvent.SurfaceCreated -> onSurfaceCreated(event.holder)
            is WallpaperEvent.SurfaceChanged -> onSurfaceChanged(event.holder, event.width, event.height)
            WallpaperEvent.SurfaceDestroyed -> onSurfaceDestroyed()
            is WallpaperEvent.VisibilityChanged -> onVisibilityChanged(event.visible)
            is WallpaperEvent.CollectionsChanged -> onCollectionsChanged(event.collections)
            WallpaperEvent.ScreenOn -> onScreenOn()
            WallpaperEvent.ScheduleBoundaryReached -> onScheduleBoundaryReached()
            WallpaperEvent.Destroy -> onDestroy()
        }
    }

    private fun onSurfaceCreated(newHolder: SurfaceHolder) {
        holder = newHolder
        surfaceAvailable = true
        surfaceGeneration += 1
        diagnostics.log("surface_created", mapOf("gen" to surfaceGeneration))
        syncOnly(source = "surface_created")
        renderIfPossible(source = "surface_created")
    }

    private fun onSurfaceChanged(newHolder: SurfaceHolder, width: Int, height: Int) {
        holder = newHolder
        surfaceWidth = width
        surfaceHeight = height
        surfaceAvailable = true
        diagnostics.log(
            "surface_changed",
            mapOf("gen" to surfaceGeneration, "w" to width, "h" to height),
        )
        syncOnly(source = "surface_changed")
        renderIfPossible(source = "surface_changed")
    }

    private fun onSurfaceDestroyed() {
        diagnostics.log("surface_destroyed", mapOf("gen" to surfaceGeneration))
        surfaceAvailable = false
        holder = null
        surfaceGeneration += 1
    }

    private fun onVisibilityChanged(nowVisible: Boolean) {
        diagnostics.log("visibility_changed", mapOf("visible" to nowVisible))
        if (!nowVisible) {
            visible = false
            hiddenAtMillis = clock.nowMillis()
            advancedForCurrentReveal = false
            boundaryScheduler.cancel()
            return
        }

        val hiddenFor = hiddenAtMillis?.let { clock.nowMillis() - it } ?: 0L
        hiddenAtMillis = null
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
        renderIfPossible(source = "visibility_changed")
    }

    private fun onScreenOn() {
        diagnostics.log("screen_on")
        // Sync only — never advance (D-015: no SCREEN_ON broadcast advance).
        syncOnly(source = "screen_on")
        if (visible) {
            renderIfPossible(source = "screen_on")
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
            renderIfPossible(source = "collections_changed")
        }
    }

    private fun onScheduleBoundaryReached() {
        diagnostics.log("schedule_boundary_reached")
        syncOnly(source = "schedule_boundary")
        scheduleNextBoundary()
        if (visible) {
            renderIfPossible(source = "schedule_boundary")
        }
    }

    private fun onDestroy() {
        if (destroyed) return
        destroyed = true
        boundaryScheduler.cancel()
        surfaceAvailable = false
        holder = null
        diagnostics.log("engine_destroyed")
        eventChannel.close()
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
            nowMillis = clock.nowMillis(),
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
        val minutes = ScheduleBoundaryCalculator.minutesUntilNextBoundary(
            collections = collections,
            minuteOfDay = clock.minuteOfDay(),
        ) ?: return
        val delayMillis = minutes * 60_000L
        diagnostics.log(
            "schedule_boundary_scheduled",
            mapOf("minutes" to minutes),
        )
        boundaryScheduler.scheduleAfterMillis(delayMillis) {
            offer(WallpaperEvent.ScheduleBoundaryReached)
        }
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
            renderer.render(
                holder = currentHolder,
                surfaceWidth = surfaceWidth,
                surfaceHeight = surfaceHeight,
                renderSpec = spec,
                surfaceGeneration = generation,
            )
        } finally {
            rendering = false
        }
    }

    /** Test hooks */
    internal fun currentState(): PlaybackState = playbackState
    internal fun isSurfaceAvailable(): Boolean = surfaceAvailable
    internal fun isDestroyed(): Boolean = destroyed
}
