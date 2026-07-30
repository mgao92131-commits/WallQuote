package com.example.wallquote.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyTimeRangeTest {

    @Test
    fun isAllDay_whenStartEqualsEnd() {
        val range = DailyTimeRange(480, 480)
        assertTrue(range.isAllDay())
        assertTrue(range.contains(0))
        assertTrue(range.contains(1439))
    }

    @Test
    fun contains_sameDayRange() {
        val range = DailyTimeRange(60, 120) // 01:00 - 02:00
        assertFalse(range.contains(59))
        assertTrue(range.contains(60))
        assertTrue(range.contains(119))
        assertFalse(range.contains(120))
    }

    @Test
    fun contains_overnightRange() {
        val range = DailyTimeRange(22 * 60, 6 * 60) // 22:00 - 06:00
        assertTrue(range.isOvernight())
        assertTrue(range.contains(23 * 60))
        assertTrue(range.contains(3 * 60))
        assertFalse(range.contains(12 * 60))
    }
}
