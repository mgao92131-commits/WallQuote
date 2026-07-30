package com.example.wallquote.domain.wallpaper

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.PlaybackCursor
import com.example.wallquote.domain.model.PlaybackState
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaybackControllerTest {

    private fun collection(
        id: Long,
        lines: List<QuoteLine>,
        sortOrder: Int = id.toInt(),
    ) = CollectionConfig(
        id = id,
        name = "c$id",
        schedule = DailyTimeRange(0, 0),
        background = BackgroundSpec.Solid("#000000"),
        lines = lines,
        textStyle = TextStyleConfig(),
        transform = QuoteTransform(),
        sortOrder = sortOrder,
    )

    @Test
    fun advanceCyclesAcrossCollections() {
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1)), sortOrder = 0),
            collection(2, listOf(QuoteLine(21, "B1", 0), QuoteLine(22, "B2", 1)), sortOrder = 1),
        )
        var state = PlaybackController.sync(collections, 0, PlaybackState())
        assertEquals(PlaybackCursor(1, 11), state.cursor)

        state = PlaybackController.advance(collections, 0, state, 1)
        assertEquals(PlaybackCursor(1, 12), state.cursor)
        state = PlaybackController.advance(collections, 0, state, 2)
        assertEquals(PlaybackCursor(2, 21), state.cursor)
        state = PlaybackController.advance(collections, 0, state, 3)
        assertEquals(PlaybackCursor(2, 22), state.cursor)
        state = PlaybackController.advance(collections, 0, state, 4)
        assertEquals(PlaybackCursor(1, 11), state.cursor)
    }

    @Test
    fun singleQuoteStaysValidOnAdvance() {
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "only", 0))),
        )
        var state = PlaybackController.sync(collections, 0, PlaybackState())
        state = PlaybackController.advance(collections, 0, state, 10)
        assertEquals(PlaybackCursor(1, 11), state.cursor)
    }

    @Test
    fun emptyGoesToEmptyState() {
        val state = PlaybackController.advance(emptyList(), 0, PlaybackState(), 1)
        assertNull(state.cursor.collectionId)
        assertNull(state.cursor.quoteLineId)
    }
}
