package com.example.wallquote.domain.time

import kotlin.math.abs
import kotlin.math.min

data class TimeArc(
    val startAngleDegrees: Float,
    /** Sweep in degrees clockwise; positive. Full day uses 360. */
    val sweepDegrees: Float,
    val wrapsMidnight: Boolean,
)

/** Which handle of a start/end time range a drag gesture should move. */
enum class TimeDragHandle { Start, End }

/**
 * Circular 24h picker geometry. 00:00 is at the top; time increases clockwise.
 */
object CircularTimeGeometry {
    const val SLOT_COUNT = 48

    fun slotToAngle(slot: Int): Float {
        val s = ((slot % SLOT_COUNT) + SLOT_COUNT) % SLOT_COUNT
        return s * (360f / SLOT_COUNT)
    }

    fun angleToNearestSlot(angleDegrees: Float): Int {
        var angle = angleDegrees % 360f
        if (angle < 0f) angle += 360f
        val slot = ((angle + (360f / SLOT_COUNT) / 2f) / (360f / SLOT_COUNT)).toInt() % SLOT_COUNT
        return slot
    }

    fun buildActiveArc(startSlot: Int, endSlot: Int): TimeArc {
        val start = ((startSlot % SLOT_COUNT) + SLOT_COUNT) % SLOT_COUNT
        val end = ((endSlot % SLOT_COUNT) + SLOT_COUNT) % SLOT_COUNT
        return if (start == end) {
            TimeArc(startAngleDegrees = slotToAngle(start), sweepDegrees = 360f, wrapsMidnight = false)
        } else if (start < end) {
            TimeArc(
                startAngleDegrees = slotToAngle(start),
                sweepDegrees = (end - start) * (360f / SLOT_COUNT),
                wrapsMidnight = false,
            )
        } else {
            TimeArc(
                startAngleDegrees = slotToAngle(start),
                sweepDegrees = (SLOT_COUNT - start + end) * (360f / SLOT_COUNT),
                wrapsMidnight = true,
            )
        }
    }

    /**
     * Resolves which handle a touch near [touchSlot] should drag (P4-016). When the touch is
     * strictly closer to one handle, that handle wins. When the start and end handles overlap
     * (e.g. the "all day" state where `startSlot == endSlot`) or are otherwise equidistant from
     * the touch, distance alone can never distinguish them and a fixed tie-break would make one
     * handle permanently undraggable from that point; instead this alternates away from
     * [lastSelected] so both handles stay reachable.
     */
    fun resolveDragHandle(
        touchSlot: Int,
        startSlot: Int,
        endSlot: Int,
        lastSelected: TimeDragHandle,
    ): TimeDragHandle {
        val startDistance = slotDistance(touchSlot, startSlot)
        val endDistance = slotDistance(touchSlot, endSlot)
        return when {
            startDistance < endDistance -> TimeDragHandle.Start
            endDistance < startDistance -> TimeDragHandle.End
            lastSelected == TimeDragHandle.Start -> TimeDragHandle.End
            else -> TimeDragHandle.Start
        }
    }

    private fun slotDistance(a: Int, b: Int): Int {
        val diff = abs(a - b) % SLOT_COUNT
        return min(diff, SLOT_COUNT - diff)
    }
}
