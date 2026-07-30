package com.example.wallquote.domain.wallpaper

import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.PlaybackCursor
import com.example.wallquote.domain.model.QuoteLine

data class FlatQuote(
    val collectionId: Long,
    val line: QuoteLine,
)

object PlaybackPlaylist {

    fun flatten(activeCollections: List<CollectionConfig>): List<FlatQuote> =
        activeCollections.flatMap { collection ->
            ActiveCollectionSelector.playableLines(collection.lines).map { line ->
                FlatQuote(collectionId = collection.id, line = line)
            }
        }

    fun indexOf(playlist: List<FlatQuote>, cursor: PlaybackCursor): Int {
        val collectionId = cursor.collectionId ?: return -1
        val quoteLineId = cursor.quoteLineId ?: return -1
        return playlist.indexOfFirst {
            it.collectionId == collectionId && it.line.id == quoteLineId
        }
    }

    fun cursorAt(playlist: List<FlatQuote>, index: Int): PlaybackCursor {
        if (playlist.isEmpty() || index !in playlist.indices) {
            return PlaybackCursor(null, null)
        }
        val item = playlist[index]
        return PlaybackCursor(
            collectionId = item.collectionId,
            quoteLineId = item.line.id,
        )
    }
}
