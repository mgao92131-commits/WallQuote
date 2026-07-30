package com.example.wallquote.domain.model

data class PlaybackState(
    val cursor: PlaybackCursor = PlaybackCursor(null, null),
    val activeCollectionIds: List<Long> = emptyList(),
    val lastAdvanceAtMillis: Long? = null,
) {
    val isEmpty: Boolean
        get() = cursor.collectionId == null || cursor.quoteLineId == null
}
