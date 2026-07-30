package com.example.wallquote.domain.wallpaper

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveCollectionSelectorTest {

    private fun collection(
        id: Long,
        start: Int,
        end: Int,
        sortOrder: Int = 0,
        lines: List<QuoteLine>,
    ) = CollectionConfig(
        id = id,
        name = "c$id",
        schedule = DailyTimeRange(start, end),
        background = BackgroundSpec.Solid("#000000"),
        lines = lines,
        textStyle = TextStyleConfig(),
        transform = QuoteTransform(),
        sortOrder = sortOrder,
    )

    @Test
    fun filtersEmptyQuotesAndInactiveSchedules() {
        val list = listOf(
            collection(
                id = 1,
                start = 0,
                end = 0,
                lines = listOf(QuoteLine(1, "  ", 0), QuoteLine(2, "ok", 1)),
            ),
            collection(
                id = 2,
                start = 8 * 60,
                end = 9 * 60,
                lines = listOf(QuoteLine(3, "morning", 0)),
            ),
        )
        val atNoon = ActiveCollectionSelector.select(list, 12 * 60)
        assertEquals(1, atNoon.size)
        assertEquals(listOf(2L), atNoon[0].lines.map { it.id })
    }

    @Test
    fun sortsBySortOrderThenId() {
        val list = listOf(
            collection(2, 0, 0, sortOrder = 1, lines = listOf(QuoteLine(1, "b", 0))),
            collection(1, 0, 0, sortOrder = 1, lines = listOf(QuoteLine(2, "a", 0))),
            collection(3, 0, 0, sortOrder = 0, lines = listOf(QuoteLine(3, "c", 0))),
        )
        val selected = ActiveCollectionSelector.select(list, 0)
        assertEquals(listOf(3L, 1L, 2L), selected.map { it.id })
    }

    @Test
    fun overnightAndHalfOpenBoundary() {
        val overnight = collection(
            id = 1,
            start = 22 * 60,
            end = 6 * 60,
            lines = listOf(QuoteLine(1, "n", 0)),
        )
        assertTrue(ActiveCollectionSelector.select(listOf(overnight), 23 * 60).isNotEmpty())
        assertTrue(ActiveCollectionSelector.select(listOf(overnight), 5 * 60).isNotEmpty())
        assertTrue(ActiveCollectionSelector.select(listOf(overnight), 6 * 60).isEmpty())
    }
}
