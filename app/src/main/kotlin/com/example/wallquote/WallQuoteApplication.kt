package com.example.wallquote

import android.app.Application
import android.content.ComponentCallbacks2
import com.example.wallquote.domain.usecase.CleanupOrphanBackgroundAssetsUseCase
import com.example.wallquote.wallpaper.background.BackgroundMemoryController
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WallQuoteApplication : Application() {

    @Inject
    lateinit var cleanupOrphanBackgroundAssetsUseCase: CleanupOrphanBackgroundAssetsUseCase

    @Inject
    lateinit var backgroundMemoryController: BackgroundMemoryController

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching { cleanupOrphanBackgroundAssetsUseCase() }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL ||
                level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                backgroundMemoryController.evictAll()
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW ||
                level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                backgroundMemoryController.trimToHalf()
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        backgroundMemoryController.evictAll()
    }
}
