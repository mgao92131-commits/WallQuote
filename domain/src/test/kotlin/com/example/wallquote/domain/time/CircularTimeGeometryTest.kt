package com.example.wallquote.domain.time

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CircularTimeGeometryTest {

    @Test
    fun slotToAngle_midnightIsZero() {
        assertEquals(0f, CircularTimeGeometry.slotToAngle(0), 0.0001f)
    }

    @Test
    fun slotToAngle_noonIsHalfway() {
        assertEquals(180f, CircularTimeGeometry.slotToAngle(24), 0.0001f)
    }

    @Test
    fun slotToAngle_wrapsNegativeAndOverflow() {
        assertEquals(CircularTimeGeometry.slotToAngle(0), CircularTimeGeometry.slotToAngle(48), 0.0001f)
        assertEquals(CircularTimeGeometry.slotToAngle(47), CircularTimeGeometry.slotToAngle(-1), 0.0001f)
    }

    @Test
    fun angleToNearestSlot_roundTripsExactAngles() {
        for (slot in 0 until CircularTimeGeometry.SLOT_COUNT) {
            val angle = CircularTimeGeometry.slotToAngle(slot)
            assertEquals(slot, CircularTimeGeometry.angleToNearestSlot(angle))
        }
    }

    @Test
    fun angleToNearestSlot_handlesNegativeAndOverflowAngles() {
        assertEquals(0, CircularTimeGeometry.angleToNearestSlot(-0.5f))
        assertEquals(0, CircularTimeGeometry.angleToNearestSlot(360f))
    }

    @Test
    fun buildActiveArc_sameSlotMeansAllDay() {
        val arc = CircularTimeGeometry.buildActiveArc(4, 4)
        assertEquals(360f, arc.sweepDegrees, 0.0001f)
        assertFalse(arc.wrapsMidnight)
    }

    @Test
    fun buildActiveArc_normalRangeDoesNotWrap() {
        val arc = CircularTimeGeometry.buildActiveArc(2, 10)
        assertFalse(arc.wrapsMidnight)
        assertEquals(CircularTimeGeometry.slotToAngle(2), arc.startAngleDegrees, 0.0001f)
        assertEquals((10 - 2) * (360f / CircularTimeGeometry.SLOT_COUNT), arc.sweepDegrees, 0.0001f)
    }

    @Test
    fun buildActiveArc_overnightRangeWraps() {
        val arc = CircularTimeGeometry.buildActiveArc(44, 4)
        assertTrue(arc.wrapsMidnight)
        val expectedSweep = (CircularTimeGeometry.SLOT_COUNT - 44 + 4) * (360f / CircularTimeGeometry.SLOT_COUNT)
        assertEquals(expectedSweep, arc.sweepDegrees, 0.0001f)
    }
}
