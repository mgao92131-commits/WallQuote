package com.example.wallquote.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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
        val range = DailyTimeRange(60, 120)
        assertFalse(range.contains(59))
        assertTrue(range.contains(60))
        assertTrue(range.contains(119))
        assertFalse(range.contains(120))
    }

    @Test
    fun contains_overnightRange() {
        val range = DailyTimeRange(22 * 60, 6 * 60)
        assertTrue(range.isOvernight())
        assertTrue(range.contains(23 * 60))
        assertTrue(range.contains(3 * 60))
        assertFalse(range.contains(12 * 60))
    }

    @Test
    fun rejectsInvalidMinute() {
        assertThrows(IllegalArgumentException::class.java) {
            DailyTimeRange(-1, 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            DailyTimeRange(0, 1440)
        }
    }
}
