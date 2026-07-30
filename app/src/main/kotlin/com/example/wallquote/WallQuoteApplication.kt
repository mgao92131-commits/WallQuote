package com.example.wallquote

import android.app.Application
import android.content.ComponentCallbacks2
import com.example.wallquote.domain.usecase.CleanupOrphanBackgroundAssetsUseCase
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

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            runCatching { cleanupOrphanBackgroundAssetsUseCase() }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Wallpaper engines trim their own bitmap caches via ComponentCallbacks when attached.
        if (level >= ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW) {
            // Formal assets are never deleted here.
        }
    }
}
