package com.example.wallquote.wallpaper

import android.os.Handler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong

/**
 * Verifies [WallpaperTransitionDriver] drives its callbacks in the order required by P4-012:
 * `from()` immediately, then at the halfway point `onFrame(0f)` strictly before `to()`, so the
 * target spec's first render never inherits a stale non-zero alpha from the outgoing side.
 */
class WallpaperTransitionDriverTest {

    /** Queues posted/delayed runnables instead of a real looper; [runNext] executes exactly one. */
    private class ImmediateHandler {
        private val queue = ArrayDeque<Runnable>()

        fun post(runnable: Runnable) {
            queue.addLast(runnable)
        }

        fun postDelayed(runnable: Runnable, @Suppress("UNUSED_PARAMETER") delayMillis: Long) {
            queue.addLast(runnable)
        }

        fun removeCallbacks(runnable: Runnable) {
            queue.remove(runnable)
        }

        fun hasPending(): Boolean = queue.isNotEmpty()

        /** Runs exactly the next queued runnable (which may itself enqueue another). */
        fun runNext() {
            val next = queue.removeFirstOrNull() ?: return
            next.run()
        }
    }

    private fun mockHandler(immediate: ImmediateHandler): Handler {
        val handler = mock(Handler::class.java)
        `when`(handler.post(any())).thenAnswer { invocation ->
            immediate.post(invocation.getArgument(0))
            true
        }
        `when`(handler.postDelayed(any(), anyLong())).thenAnswer { invocation ->
            immediate.postDelayed(invocation.getArgument(0), invocation.getArgument(1))
            true
        }
        doAnswer { invocation ->
            immediate.removeCallbacks(invocation.getArgument(0))
            null
        }.`when`(handler).removeCallbacks(any())
        return handler
    }

    @Test
    fun onFrameZero_isCalledBeforeTo_atSwitchPoint() {
        val immediate = ImmediateHandler()
        val handler = mockHandler(immediate)
        var now = 0L
        val driver = WallpaperTransitionDriver(handler = handler, nowMillis = { now })

        val callOrder = mutableListOf<String>()

        driver.start(
            from = { callOrder += "from" },
            to = { callOrder += "to" },
            onFrame = { alpha -> callOrder += "onFrame($alpha)" },
            onComplete = { callOrder += "onComplete" },
        )
        assertEquals(listOf("from"), callOrder)
        assertTrue("expected the first frame tick to be queued", immediate.hasPending())

        // First tick, still before the halfway point: just a fade-out frame, no switch yet.
        now = 0L
        immediate.runNext()
        assertEquals(listOf("from", "onFrame(1.0)"), callOrder)

        // Reach the exact halfway point: onFrame(0f) must fire before to().
        now = WallpaperTransitionDriver.HALF_DURATION_MS
        immediate.runNext()
        val toIndex = callOrder.indexOf("to")
        val zeroFrameIndex = callOrder.indexOf("onFrame(0.0)")
        assertTrue("expected 'to' to have been invoked at the halfway point: $callOrder", toIndex >= 0)
        assertTrue(
            "expected onFrame(0f) to appear before 'to', got: $callOrder",
            zeroFrameIndex in 0 until toIndex,
        )

        // Reach the end: onComplete fires and no further frames are queued.
        now = WallpaperTransitionDriver.TOTAL_DURATION_MS
        immediate.runNext()
        assertEquals("onComplete", callOrder.last())
        assertTrue("driver should not queue further ticks after completing", !immediate.hasPending())
    }
}
