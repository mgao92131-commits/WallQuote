package com.example.wallquote.domain.wallpaper

import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.PlaybackCursor
import com.example.wallquote.domain.model.PlaybackState

/**
 * Restores a playback cursor after collection data changes without advancing.
 */
object PlaybackReconciler {

    fun reconcile(
        collections: List<CollectionConfig>,
        minuteOfDay: Int,
        previous: PlaybackState,
    ): PlaybackState {
        val active = ActiveCollectionSelector.select(collections, minuteOfDay)
        val playlist = PlaybackPlaylist.flatten(active)
        val activeIds = active.map { it.id }

        if (playlist.isEmpty()) {
            return PlaybackState(
                cursor = PlaybackCursor(null, null),
                activeCollectionIds = emptyList(),
                lastAdvanceAtMillis = previous.lastAdvanceAtMillis,
            )
        }

        val previousCursor = previous.cursor
        val previousCollectionId = previousCursor.collectionId
        val previousLineId = previousCursor.quoteLineId

        // 1) Current quote still valid.
        if (previousCollectionId != null && previousLineId != null) {
            val exact = PlaybackPlaylist.indexOf(playlist, previousCursor)
            if (exact >= 0) {
                return PlaybackState(
                    cursor = previousCursor,
                    activeCollectionIds = activeIds,
                    lastAdvanceAtMillis = previous.lastAdvanceAtMillis,
                )
            }

            // 2) Quote gone, collection still active → first line of that collection.
            val collectionStillActive = active.any { it.id == previousCollectionId }
            if (collectionStillActive) {
                val firstInCollection = playlist.first { it.collectionId == previousCollectionId }
                return PlaybackState(
                    cursor = PlaybackCursor(firstInCollection.collectionId, firstInCollection.line.id),
                    activeCollectionIds = activeIds,
                    lastAdvanceAtMillis = previous.lastAdvanceAtMillis,
                )
            }
        }

        // 3) Collection invalid/deleted → first active quote.
        val first = playlist.first()
        return PlaybackState(
            cursor = PlaybackCursor(first.collectionId, first.line.id),
            activeCollectionIds = activeIds,
            lastAdvanceAtMillis = previous.lastAdvanceAtMillis,
        )
    }
}
