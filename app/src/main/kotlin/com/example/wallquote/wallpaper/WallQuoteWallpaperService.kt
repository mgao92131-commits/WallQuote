package com.example.wallquote.wallpaper

import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.wallquote.domain.repository.CollectionRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WallQuoteWallpaperService : WallpaperService() {

    @Inject
    lateinit var collectionRepository: CollectionRepository

    override fun onCreateEngine(): Engine = WallQuoteEngine(collectionRepository)

    inner class WallQuoteEngine(
        private val repository: CollectionRepository,
    ) : Engine() {

        private val engineId = AndroidWallpaperDiagnostics.newEngineId()
        private val diagnostics = AndroidWallpaperDiagnostics(engineId)
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.Main.immediate + job)
        private val clock = AndroidClock()
        private lateinit var coordinator: WallpaperCoordinator

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            val renderer = CanvasWallpaperRenderer(
                density = resources.displayMetrics.density,
                diagnostics = diagnostics,
            )
            val scheduler = CoroutineBoundaryScheduler(scope)
            coordinator = WallpaperCoordinator(
                scope = scope,
                clock = clock,
                renderer = renderer,
                boundaryScheduler = scheduler,
                diagnostics = diagnostics,
            )
            coordinator.start()
            scope.launch {
                repository.observeOrderedCollections().collectLatest { collections ->
                    coordinator.offer(WallpaperEvent.CollectionsChanged(collections))
                }
            }
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
            coordinator.offer(WallpaperEvent.SurfaceCreated(holder))
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            coordinator.offer(WallpaperEvent.SurfaceChanged(holder, width, height))
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            coordinator.offer(WallpaperEvent.SurfaceDestroyed)
            super.onSurfaceDestroyed(holder)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            coordinator.offer(WallpaperEvent.VisibilityChanged(visible))
        }

        override fun onDestroy() {
            coordinator.offer(WallpaperEvent.Destroy)
            job.cancel()
            scope.cancel()
            super.onDestroy()
        }
    }
}
