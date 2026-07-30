package com.example.wallquote.wallpaper

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CoroutineBoundaryScheduler(
    private val scope: CoroutineScope,
) : BoundaryScheduler {

    private var job: Job? = null

    override fun scheduleAfterMillis(delayMillis: Long, onFire: () -> Unit) {
        cancel()
        job = scope.launch {
            delay(delayMillis.coerceAtLeast(0L))
            onFire()
        }
    }

    override fun cancel() {
        job?.cancel()
        job = null
    }
}
