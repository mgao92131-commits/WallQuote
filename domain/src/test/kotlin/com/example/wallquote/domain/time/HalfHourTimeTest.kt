package com.example.wallquote.domain.time

import org.junit.Assert.assertEquals
import org.junit.Test

class HalfHourTimeTest {

    @Test
    fun roundTripHalfHourSlots() {
        assertEquals(0, minuteFromHalfHourIndex(0))
        assertEquals(30, minuteFromHalfHourIndex(1))
        assertEquals(23 * 60 + 30, minuteFromHalfHourIndex(47))
        assertEquals(0, halfHourIndexFromMinute(0))
        assertEquals(1, halfHourIndexFromMinute(30))
        assertEquals(47, halfHourIndexFromMinute(23 * 60 + 30))
        assertEquals(47, halfHourIndexFromMinute(23 * 60 + 45))
    }
}
