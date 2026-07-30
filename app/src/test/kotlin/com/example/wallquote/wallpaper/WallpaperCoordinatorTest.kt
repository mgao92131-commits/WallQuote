package com.example.wallquote.wallpaper

import android.view.SurfaceHolder
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.PlaybackCursor
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig
import com.example.wallquote.domain.model.WallpaperRenderSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class WallpaperCoordinatorTest {

    private val fakeHolder: SurfaceHolder = mock(SurfaceHolder::class.java)

    private class FakeClock(
        var elapsed: Long = 0L,
        var wall: Long = 0L,
        var minute: Int = 0,
        var second: Int = 0,
        var millis: Int = 0,
    ) : WallpaperClock {
        override fun elapsedRealtimeMillis(): Long = elapsed
        override fun currentWallTimeMillis(): Long = wall
        override fun minuteOfDay(): Int = minute
        override fun secondOfMinute(): Int = second
        override fun millisOfSecond(): Int = millis
    }

    private class FakeRenderer : WallpaperRenderTarget {
        var renderCount = 0
        var concurrent = 0
        var maxConcurrent = 0
        var lastSpec: WallpaperRenderSpec? = null

        override fun render(
            holder: SurfaceHolder?,
            surfaceWidth: Int,
            surfaceHeight: Int,
            renderSpec: WallpaperRenderSpec,
            surfaceGeneration: Long,
            preparedPhoto: PreparedPhotoFrame?,
        ): RenderOutcome {
            concurrent++
            maxConcurrent = maxOf(maxConcurrent, concurrent)
            renderCount++
            lastSpec = renderSpec
            concurrent--
            return RenderOutcome.Success
        }
    }

    private class FakeScheduler : BoundaryScheduler {
        var scheduledDelay: Long? = null
        private var pending: (() -> Unit)? = null

        override fun scheduleAfterMillis(delayMillis: Long, onFire: () -> Unit) {
            scheduledDelay = delayMillis
            pending = onFire
        }

        override fun cancel() {
            pending = null
            scheduledDelay = null
        }

        fun fire() {
            val cb = pending
            pending = null
            cb?.invoke()
        }
    }

    private class FakeDiagnostics : WallpaperDiagnostics {
        val events = mutableListOf<String>()
        override fun log(event: String, details: Map<String, Any?>) {
            events += event
        }
    }

    /** Records start/cancel calls; frames only fire when a test explicitly drives them. */
    private class FakeTransitionDriver : TransitionDriver {
        var startCount = 0
        var cancelCount = 0
        private var running = false
        private var pendingTo: (() -> Unit)? = null
        private var pendingOnFrame: ((Float) -> Unit)? = null
        private var pendingOnComplete: (() -> Unit)? = null

        override fun isRunning(): Boolean = running

        override fun start(
            from: () -> Unit,
            to: () -> Unit,
            onFrame: (Float) -> Unit,
            onComplete: () -> Unit,
        ) {
            startCount++
            running = true
            pendingTo = to
            pendingOnFrame = onFrame
            pendingOnComplete = onComplete
            from()
        }

        override fun cancel() {
            if (running) cancelCount++
            running = false
            pendingTo = null
            pendingOnFrame = null
            pendingOnComplete = null
        }

        /** Simulates the driver reaching the end of the animation. */
        fun completeNow() {
            val to = pendingTo
            val onFrame = pendingOnFrame
            val onComplete = pendingOnComplete
            running = false
            pendingTo = null
            pendingOnFrame = null
            pendingOnComplete = null
            to?.invoke()
            onFrame?.invoke(1f)
            onComplete?.invoke()
        }
    }

    private fun collection(
        id: Long,
        lines: List<QuoteLine>,
    ) = CollectionConfig(
        id = id,
        name = "c$id",
        schedule = DailyTimeRange(0, 0),
        background = BackgroundSpec.Solid("#2E3440"),
        lines = lines,
        textStyle = TextStyleConfig(),
        transform = QuoteTransform(),
        sortOrder = id.toInt(),
    )

    private fun TestScope.createCoordinator(
        clock: FakeClock,
        renderer: FakeRenderer,
        scheduler: FakeScheduler = FakeScheduler(),
        transitionDriver: FakeTransitionDriver = FakeTransitionDriver(),
    ): WallpaperCoordinator {
        val coordinator = WallpaperCoordinator(
            scope = backgroundScope,
            clock = clock,
            renderer = renderer,
            boundaryScheduler = scheduler,
            diagnostics = FakeDiagnostics(),
            transitionDriver = transitionDriver,
        )
        coordinator.start()
        return coordinator
    }

    private fun TestScope.finish(coordinator: WallpaperCoordinator) {
        coordinator.close()
        advanceUntilIdle()
    }

    @Test
    fun surfaceCreatedSyncsAndRendersWithoutAdvance() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock()
        val renderer = FakeRenderer()
        val coordinator = createCoordinator(clock, renderer)
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 1080, 1920))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()

        assertEquals(PlaybackCursor(1, 11), coordinator.currentState().cursor)
        assertTrue(renderer.renderCount >= 1)
        assertEquals("A1", renderer.lastSpec?.text)
        finish(coordinator)
    }

    @Test
    fun shortHideDoesNotAdvance() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock(elapsed = 0)
        val renderer = FakeRenderer()
        val coordinator = createCoordinator(clock, renderer)
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 100, 100))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()

        coordinator.offer(WallpaperEvent.VisibilityChanged(false))
        clock.elapsed = 29_000
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()

        assertEquals(PlaybackCursor(1, 11), coordinator.currentState().cursor)
        finish(coordinator)
    }

    @Test
    fun longHideAdvancesOnce() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock(elapsed = 0)
        val renderer = FakeRenderer()
        val coordinator = createCoordinator(clock, renderer)
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 100, 100))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()

        coordinator.offer(WallpaperEvent.VisibilityChanged(false))
        clock.elapsed = 30_000
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()

        assertEquals(PlaybackCursor(1, 12), coordinator.currentState().cursor)
        finish(coordinator)
    }

    @Test
    fun screenOnDoesNotAdvanceEvenWithVisibility() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock(elapsed = 0)
        val renderer = FakeRenderer()
        val coordinator = createCoordinator(clock, renderer)
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 100, 100))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()

        coordinator.offer(WallpaperEvent.VisibilityChanged(false))
        clock.elapsed = 31_000
        coordinator.offer(WallpaperEvent.ScreenOn)
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()

        assertEquals(PlaybackCursor(1, 12), coordinator.currentState().cursor)
        finish(coordinator)
    }

    @Test
    fun dataChangeReconcilesWithoutAdvance() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock()
        val renderer = FakeRenderer()
        val coordinator = createCoordinator(clock, renderer)
        val original = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(original))
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 100, 100))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()

        val updated = listOf(
            collection(1, listOf(QuoteLine(11, "A1-edited", 0), QuoteLine(12, "A2", 1))),
            collection(2, listOf(QuoteLine(21, "B1", 0))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(updated))
        advanceUntilIdle()

        assertEquals(PlaybackCursor(1, 11), coordinator.currentState().cursor)
        assertEquals("A1-edited", renderer.lastSpec?.text)
        finish(coordinator)
    }

    @Test
    fun surfaceUnavailableSkipsRender() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock()
        val renderer = FakeRenderer()
        val coordinator = createCoordinator(clock, renderer)
        coordinator.offer(
            WallpaperEvent.CollectionsChanged(
                listOf(collection(1, listOf(QuoteLine(11, "A1", 0)))),
            ),
        )
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 100, 100))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()
        val before = renderer.renderCount

        coordinator.offer(WallpaperEvent.SurfaceDestroyed)
        coordinator.offer(
            WallpaperEvent.CollectionsChanged(
                listOf(collection(1, listOf(QuoteLine(11, "A1x", 0)))),
            ),
        )
        advanceUntilIdle()

        assertEquals(before, renderer.renderCount)
        assertFalse(coordinator.isSurfaceAvailable())
        finish(coordinator)
    }

    @Test
    fun destroyStopsFurtherRender() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock()
        val renderer = FakeRenderer()
        val coordinator = createCoordinator(clock, renderer)
        coordinator.offer(
            WallpaperEvent.CollectionsChanged(
                listOf(collection(1, listOf(QuoteLine(11, "A1", 0)))),
            ),
        )
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 100, 100))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()
        val before = renderer.renderCount

        coordinator.close()
        advanceUntilIdle()
        coordinator.offer(
            WallpaperEvent.CollectionsChanged(
                listOf(collection(1, listOf(QuoteLine(11, "A1y", 0)))),
            ),
        )
        advanceUntilIdle()

        assertTrue(coordinator.isDestroyed())
        assertEquals(before, renderer.renderCount)
    }

    @Test
    fun scheduleBoundarySyncsWithoutAdvance() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock(minute = 8 * 60 - 1)
        val renderer = FakeRenderer()
        val scheduler = FakeScheduler()
        val coordinator = createCoordinator(clock, renderer, scheduler)
        val collections = listOf(
            CollectionConfig(
                id = 1,
                name = "morning",
                schedule = DailyTimeRange(8 * 60, 12 * 60),
                background = BackgroundSpec.Solid("#000"),
                lines = listOf(QuoteLine(11, "A1", 0)),
                textStyle = TextStyleConfig(),
                transform = QuoteTransform(),
                sortOrder = 0,
            ),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 100, 100))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()
        assertEquals(PlaybackCursor(null, null), coordinator.currentState().cursor)

        clock.minute = 8 * 60
        coordinator.offer(WallpaperEvent.ScheduleBoundaryReached)
        advanceUntilIdle()
        assertEquals(PlaybackCursor(1, 11), coordinator.currentState().cursor)
        finish(coordinator)
    }

    private fun TestScope.revealAfterLongHide(
        coordinator: WallpaperCoordinator,
        clock: FakeClock,
    ) {
        coordinator.offer(WallpaperEvent.VisibilityChanged(false))
        clock.elapsed += 30_000
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()
    }

    @Test
    fun syncPathsNeverStartTransition() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock()
        val renderer = FakeRenderer()
        val transitionDriver = FakeTransitionDriver()
        val coordinator = createCoordinator(clock, renderer, transitionDriver = transitionDriver)
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 1080, 1920))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        coordinator.offer(WallpaperEvent.ScreenOn)
        coordinator.offer(WallpaperEvent.TimeOrZoneChanged)
        coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
        coordinator.offer(WallpaperEvent.ScheduleBoundaryReached)
        advanceUntilIdle()

        assertEquals(0, transitionDriver.startCount)
        assertFalse(coordinator.isTransitioningForTest())
        finish(coordinator)
    }

    @Test
    fun longHideAdvanceStartsTransitionAndCompletesToTargetText() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock()
        val renderer = FakeRenderer()
        val transitionDriver = FakeTransitionDriver()
        val coordinator = createCoordinator(clock, renderer, transitionDriver = transitionDriver)
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 1080, 1920))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()

        revealAfterLongHide(coordinator, clock)

        assertEquals(1, transitionDriver.startCount)
        assertTrue(coordinator.isTransitioningForTest())
        assertEquals(PlaybackCursor(1, 12), coordinator.currentState().cursor)

        transitionDriver.completeNow()
        advanceUntilIdle()

        assertFalse(coordinator.isTransitioningForTest())
        assertEquals("A2", renderer.lastSpec?.text)
        finish(coordinator)
    }

    @Test
    fun hideCancelsInFlightTransition() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock()
        val renderer = FakeRenderer()
        val transitionDriver = FakeTransitionDriver()
        val coordinator = createCoordinator(clock, renderer, transitionDriver = transitionDriver)
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 1080, 1920))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()
        revealAfterLongHide(coordinator, clock)
        assertTrue(coordinator.isTransitioningForTest())

        coordinator.offer(WallpaperEvent.VisibilityChanged(false))
        advanceUntilIdle()

        assertEquals(1, transitionDriver.cancelCount)
        assertFalse(coordinator.isTransitioningForTest())
        finish(coordinator)
    }

    @Test
    fun surfaceDestroyedCancelsInFlightTransition() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock()
        val renderer = FakeRenderer()
        val transitionDriver = FakeTransitionDriver()
        val coordinator = createCoordinator(clock, renderer, transitionDriver = transitionDriver)
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 1080, 1920))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()
        revealAfterLongHide(coordinator, clock)
        assertTrue(coordinator.isTransitioningForTest())

        coordinator.offer(WallpaperEvent.SurfaceDestroyed)
        advanceUntilIdle()

        assertEquals(1, transitionDriver.cancelCount)
        assertFalse(coordinator.isTransitioningForTest())
        finish(coordinator)
    }

    @Test
    fun collectionsChangedCancelsTransitionAndResyncsImmediately() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock()
        val renderer = FakeRenderer()
        val transitionDriver = FakeTransitionDriver()
        val coordinator = createCoordinator(clock, renderer, transitionDriver = transitionDriver)
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 1080, 1920))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()
        revealAfterLongHide(coordinator, clock)
        assertTrue(coordinator.isTransitioningForTest())

        val updated = listOf(
            collection(1, listOf(QuoteLine(11, "A1-edited", 0), QuoteLine(12, "A2-edited", 1))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(updated))
        advanceUntilIdle()

        assertEquals(1, transitionDriver.cancelCount)
        assertFalse(coordinator.isTransitioningForTest())
        assertEquals("A2-edited", renderer.lastSpec?.text)
        finish(coordinator)
    }

    @Test
    fun scheduleBoundaryCancelsTransitionAndSyncsWithoutAdvance() = runTest(UnconfinedTestDispatcher()) {
        val clock = FakeClock(minute = 8 * 60 - 1)
        val renderer = FakeRenderer()
        val transitionDriver = FakeTransitionDriver()
        val coordinator = createCoordinator(clock, renderer, transitionDriver = transitionDriver)
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1))),
        )
        coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
        coordinator.offer(WallpaperEvent.SurfaceCreated(fakeHolder))
        coordinator.offer(WallpaperEvent.SurfaceChanged(fakeHolder, 1080, 1920))
        coordinator.offer(WallpaperEvent.VisibilityChanged(true))
        advanceUntilIdle()
        revealAfterLongHide(coordinator, clock)
        assertTrue(coordinator.isTransitioningForTest())
        val cursorBeforeBoundary = coordinator.currentState().cursor

        clock.minute = 8 * 60
        coordinator.offer(WallpaperEvent.ScheduleBoundaryReached)
        advanceUntilIdle()

        assertEquals(1, transitionDriver.cancelCount)
        assertFalse(coordinator.isTransitioningForTest())
        assertEquals(cursorBeforeBoundary, coordinator.currentState().cursor)
        finish(coordinator)
    }
}
