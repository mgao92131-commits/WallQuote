package com.example.wallquote.domain.wallpaper

import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.PlaybackCursor
import com.example.wallquote.domain.model.PlaybackState

/**
 * Advances through the flattened playlist of active collections / quotes.
 */
object PlaybackController {

    fun sync(
        collections: List<CollectionConfig>,
        minuteOfDay: Int,
        previous: PlaybackState,
    ): PlaybackState = PlaybackReconciler.reconcile(collections, minuteOfDay, previous)

    fun advance(
        collections: List<CollectionConfig>,
        minuteOfDay: Int,
        previous: PlaybackState,
        nowMillis: Long,
    ): PlaybackState {
        val synced = sync(collections, minuteOfDay, previous)
        val playlist = PlaybackPlaylist.flatten(
            ActiveCollectionSelector.select(collections, minuteOfDay),
        )
        if (playlist.isEmpty()) {
            return synced.copy(
                cursor = PlaybackCursor(null, null),
                lastAdvanceAtMillis = nowMillis,
            )
        }
        val currentIndex = PlaybackPlaylist.indexOf(playlist, synced.cursor)
        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % playlist.size
        return synced.copy(
            cursor = PlaybackPlaylist.cursorAt(playlist, nextIndex),
            lastAdvanceAtMillis = nowMillis,
        )
    }
}
