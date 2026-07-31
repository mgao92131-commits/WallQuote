package com.example.wallquote.wallpaper

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

/**
 * Real (Handler-driven) implementation of [TransitionDriver] for the 288ms advance fade.
 *
 * Runs frame callbacks on the [handler]'s looper (the wallpaper engine's main thread), which is
 * also where [WallpaperCoordinator] processes its event loop in production, so no extra
 * synchronization is required between transition frames and coordinator event handling.
 */
class WallpaperTransitionDriver(
    private val handler: Handler = Handler(Looper.getMainLooper()),
    private val nowMillis: () -> Long = { SystemClock.uptimeMillis() },
) : TransitionDriver {

    private var frameRunnable: Runnable? = null
    private var running = false

    override fun isRunning(): Boolean = running

    override fun start(
        from: () -> Unit,
        to: () -> Unit,
        onFrame: (textAlpha: Float) -> Unit,
        onComplete: () -> Unit,
    ) {
        cancel()
        running = true
        from()
        val startTime = nowMillis()
        var switched = false
        val runnable = object : Runnable {
            override fun run() {
                if (!running) return
                val elapsed = nowMillis() - startTime
                if (!switched && elapsed >= HALF_DURATION_MS) {
                    switched = true
                    // Force a clean pass through alpha=0 at the exact switch point before
                    // flipping the rendered spec to the target, so the target's first frame
                    // never renders at a stale intermediate alpha (P4-012).
                    onFrame(0f)
                    to()
                }
                if (elapsed >= TOTAL_DURATION_MS) {
                    running = false
                    frameRunnable = null
                    onFrame(1f)
                    onComplete()
                    return
                }
                val alpha = if (elapsed < HALF_DURATION_MS) {
                    1f - elapsed / HALF_DURATION_MS.toFloat()
                } else {
                    (elapsed - HALF_DURATION_MS) / HALF_DURATION_MS.toFloat()
                }
                onFrame(alpha.coerceIn(0f, 1f))
                handler.postDelayed(this, FRAME_INTERVAL_MS)
            }
        }
        frameRunnable = runnable
        handler.post(runnable)
    }

    override fun cancel() {
        frameRunnable?.let { handler.removeCallbacks(it) }
        frameRunnable = null
        running = false
    }

    companion object {
        const val TOTAL_DURATION_MS = 288L
        const val HALF_DURATION_MS = 144L
        private const val FRAME_INTERVAL_MS = 16L
    }
}
