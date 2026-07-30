package com.example.wallquote.wallpaper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import com.example.wallquote.domain.repository.CollectionRepository
import com.example.wallquote.wallpaper.background.BackgroundImageLoader
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

    @Inject
    lateinit var backgroundImageLoader: BackgroundImageLoader

    override fun onCreateEngine(): Engine =
        WallQuoteEngine(collectionRepository, backgroundImageLoader)

    inner class WallQuoteEngine(
        private val repository: CollectionRepository,
        private val imageLoader: BackgroundImageLoader,
    ) : Engine() {

        private val engineId = AndroidWallpaperDiagnostics.newEngineId()
        private val diagnostics = AndroidWallpaperDiagnostics(engineId)
        private val job = SupervisorJob()
        private val scope = CoroutineScope(Dispatchers.Main.immediate + job)
        private val clock = AndroidClock()
        private lateinit var coordinator: WallpaperCoordinator
        private var timeReceiverRegistered = false

        private val timeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    Intent.ACTION_DATE_CHANGED,
                    -> coordinator.offer(WallpaperEvent.TimeOrZoneChanged)
                }
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            (imageLoader as? com.example.wallquote.wallpaper.background.DefaultBackgroundImageLoader)
                ?.diagnostics = diagnostics
            val renderer = CanvasWallpaperRenderer(
                density = resources.displayMetrics.density,
                fontScale = resources.configuration.fontScale,
                diagnostics = diagnostics,
            )
            val scheduler = CoroutineBoundaryScheduler(scope)
            coordinator = WallpaperCoordinator(
                scope = scope,
                clock = clock,
                renderer = renderer,
                boundaryScheduler = scheduler,
                diagnostics = diagnostics,
                imageLoader = imageLoader,
                density = resources.displayMetrics.density,
            )
            coordinator.start()
            registerTimeReceiver()
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
            unregisterTimeReceiver()
            if (::coordinator.isInitialized) {
                coordinator.close()
            }
            job.cancel()
            scope.cancel()
            super.onDestroy()
        }

        private fun registerTimeReceiver() {
            if (timeReceiverRegistered) return
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_DATE_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                applicationContext.registerReceiver(timeReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                applicationContext.registerReceiver(timeReceiver, filter)
            }
            timeReceiverRegistered = true
        }

        private fun unregisterTimeReceiver() {
            if (!timeReceiverRegistered) return
            runCatching { applicationContext.unregisterReceiver(timeReceiver) }
            timeReceiverRegistered = false
        }
    }
}
