package com.example.wallquote.wallpaper

import android.util.Log
import java.util.concurrent.atomic.AtomicLong

class AndroidWallpaperDiagnostics(
    private val engineId: String,
) : WallpaperDiagnostics {

    private val tag = "WallQuoteWP"

    override fun log(event: String, details: Map<String, Any?>) {
        val payload = buildString {
            append("engine=").append(engineId)
            append(' ').append(event)
            details.forEach { (key, value) ->
                // Never log quote text or JSON payloads.
                append(' ').append(key).append('=').append(value)
            }
        }
        Log.i(tag, payload)
    }

    companion object {
        private val nextId = AtomicLong(1)
        fun newEngineId(): String = "eng-${nextId.getAndIncrement()}"
    }
}
