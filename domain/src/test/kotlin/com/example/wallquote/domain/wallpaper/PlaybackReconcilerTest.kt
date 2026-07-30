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
import org.junit.Test

class PlaybackReconcilerTest {

    private fun collection(
        id: Long,
        lines: List<QuoteLine>,
        start: Int = 0,
        end: Int = 0,
    ) = CollectionConfig(
        id = id,
        name = "c$id",
        schedule = DailyTimeRange(start, end),
        background = BackgroundSpec.Solid("#000000"),
        lines = lines,
        textStyle = TextStyleConfig(),
        transform = QuoteTransform(),
        sortOrder = id.toInt(),
    )

    @Test
    fun keepsCurrentQuoteWhenStillValid() {
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(12, "A2", 1))),
            collection(2, listOf(QuoteLine(21, "B1", 0))),
        )
        val previous = PlaybackState(cursor = PlaybackCursor(1, 12))
        val next = PlaybackReconciler.reconcile(collections, 0, previous)
        assertEquals(PlaybackCursor(1, 12), next.cursor)
    }

    @Test
    fun fallsBackToFirstLineWhenQuoteDeleted() {
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0), QuoteLine(13, "A3", 1))),
        )
        val previous = PlaybackState(cursor = PlaybackCursor(1, 12))
        val next = PlaybackReconciler.reconcile(collections, 0, previous)
        assertEquals(PlaybackCursor(1, 11), next.cursor)
    }

    @Test
    fun fallsBackToFirstActiveWhenCollectionRemoved() {
        val collections = listOf(
            collection(2, listOf(QuoteLine(21, "B1", 0))),
        )
        val previous = PlaybackState(cursor = PlaybackCursor(1, 11))
        val next = PlaybackReconciler.reconcile(collections, 0, previous)
        assertEquals(PlaybackCursor(2, 21), next.cursor)
    }

    @Test
    fun newCollectionDoesNotInterruptCurrent() {
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0))),
            collection(2, listOf(QuoteLine(21, "B1", 0))),
        )
        val previous = PlaybackState(cursor = PlaybackCursor(1, 11))
        val next = PlaybackReconciler.reconcile(collections, 0, previous)
        assertEquals(PlaybackCursor(1, 11), next.cursor)
        assertEquals(listOf(1L, 2L), next.activeCollectionIds)
    }

    @Test
    fun inactiveScheduleClearsToNextActive() {
        val collections = listOf(
            collection(1, listOf(QuoteLine(11, "A1", 0)), start = 8 * 60, end = 9 * 60),
            collection(2, listOf(QuoteLine(21, "B1", 0)), start = 0, end = 0),
        )
        val previous = PlaybackState(cursor = PlaybackCursor(1, 11))
        val next = PlaybackReconciler.reconcile(collections, 12 * 60, previous)
        assertEquals(PlaybackCursor(2, 21), next.cursor)
    }
}
