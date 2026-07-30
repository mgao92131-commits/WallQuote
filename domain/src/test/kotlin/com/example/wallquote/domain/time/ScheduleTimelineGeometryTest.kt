package com.example.wallquote.domain.time

import com.example.wallquote.domain.model.DailyTimeRange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleTimelineGeometryTest {

    @Test
    fun allDay_producesFullSegment() {
        val result = ScheduleTimelineGeometry.compute(DailyTimeRange(480, 480), minuteOfDay = 0)
        assertTrue(result.isAllDay)
        assertFalse(result.wrapsMidnight)
        assertEquals(1, result.segments.size)
        assertEquals(0f, result.segments[0].startFraction, 0.0001f)
        assertEquals(1f, result.segments[0].endFraction, 0.0001f)
        assertTrue(result.isActiveNow)
    }

    @Test
    fun sameDayRange_producesSingleSegment() {
        val schedule = DailyTimeRange(60, 120)
        val result = ScheduleTimelineGeometry.compute(schedule, minuteOfDay = 90)
        assertFalse(result.isAllDay)
        assertFalse(result.wrapsMidnight)
        assertEquals(1, result.segments.size)
        assertEquals(60f / 1440f, result.segments[0].startFraction, 0.0001f)
        assertEquals(120f / 1440f, result.segments[0].endFraction, 0.0001f)
        assertTrue(result.isActiveNow)
    }

    @Test
    fun sameDayRange_notActiveOutsideWindow() {
        val schedule = DailyTimeRange(60, 120)
        val result = ScheduleTimelineGeometry.compute(schedule, minuteOfDay = 500)
        assertFalse(result.isActiveNow)
    }

    @Test
    fun overnightRange_wrapsIntoTwoSegments() {
        val schedule = DailyTimeRange(22 * 60, 6 * 60)
        val result = ScheduleTimelineGeometry.compute(schedule, minuteOfDay = 23 * 60)
        assertTrue(result.wrapsMidnight)
        assertEquals(2, result.segments.size)
        assertEquals(22 * 60f / 1440f, result.segments[0].startFraction, 0.0001f)
        assertEquals(1f, result.segments[0].endFraction, 0.0001f)
        assertEquals(0f, result.segments[1].startFraction, 0.0001f)
        assertEquals(6 * 60f / 1440f, result.segments[1].endFraction, 0.0001f)
        assertTrue(result.isActiveNow)
    }

    @Test
    fun overnightRange_activeAfterMidnightBeforeEnd() {
        val schedule = DailyTimeRange(22 * 60, 6 * 60)
        val result = ScheduleTimelineGeometry.compute(schedule, minuteOfDay = 3 * 60)
        assertTrue(result.isActiveNow)
    }

    @Test
    fun nowFraction_reflectsMinuteOfDay() {
        val schedule = DailyTimeRange(0, 0)
        val result = ScheduleTimelineGeometry.compute(schedule, minuteOfDay = 720)
        assertEquals(0.5f, result.nowFraction, 0.0001f)
    }
}
