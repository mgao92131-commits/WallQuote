package com.example.wallquote.domain.wallpaper

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleBoundaryCalculatorTest {

    private fun collection(id: Long, start: Int, end: Int) = CollectionConfig(
        id = id,
        name = "c$id",
        schedule = DailyTimeRange(start, end),
        background = BackgroundSpec.Solid("#000"),
        lines = listOf(QuoteLine(1, "t", 0)),
        textStyle = TextStyleConfig(),
        transform = QuoteTransform(),
        sortOrder = 0,
    )

    @Test
    fun nextSameDayStart() {
        val collections = listOf(collection(1, 8 * 60, 12 * 60))
        assertEquals(60, ScheduleBoundaryCalculator.minutesUntilNextBoundary(collections, 7 * 60))
    }

    @Test
    fun nextEndBoundary() {
        val collections = listOf(collection(1, 8 * 60, 12 * 60))
        assertEquals(1, ScheduleBoundaryCalculator.minutesUntilNextBoundary(collections, 12 * 60 - 1))
    }

    @Test
    fun wrapsOvernight() {
        val collections = listOf(collection(1, 22 * 60, 6 * 60))
        assertEquals(7 * 60, ScheduleBoundaryCalculator.minutesUntilNextBoundary(collections, 23 * 60))
    }

    @Test
    fun allDayProducesNoBoundary() {
        val collections = listOf(collection(1, 0, 0))
        assertNull(ScheduleBoundaryCalculator.minutesUntilNextBoundary(collections, 100))
    }

    @Test
    fun multipleBoundariesPicksSoonest() {
        val collections = listOf(
            collection(1, 8 * 60, 12 * 60),
            collection(2, 9 * 60, 10 * 60),
        )
        assertEquals(30, ScheduleBoundaryCalculator.minutesUntilNextBoundary(collections, 8 * 60 + 30))
    }

    @Test
    fun millisPrecisionAccountsForSeconds() {
        val collections = listOf(collection(1, 8 * 60, 12 * 60))
        // At 07:59:50 → 10 seconds until 08:00:00
        val delay = ScheduleBoundaryCalculator.millisUntilNextBoundary(
            collections = collections,
            minuteOfDay = 7 * 60 + 59,
            secondOfMinute = 50,
            millisOfSecond = 0,
        )
        assertEquals(10_000L, delay)
    }

    @Test
    fun millisPrecisionNearExactBoundary() {
        val collections = listOf(collection(1, 8 * 60, 12 * 60))
        val delay = ScheduleBoundaryCalculator.millisUntilNextBoundary(
            collections = collections,
            minuteOfDay = 8 * 60 - 1,
            secondOfMinute = 59,
            millisOfSecond = 500,
        )
        assertEquals(500L, delay)
        assertTrue(delay!! < 60_000L)
    }
}
