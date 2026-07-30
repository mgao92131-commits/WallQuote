package com.example.wallquote.domain.time

data class TimeArc(
    val startAngleDegrees: Float,
    /** Sweep in degrees clockwise; positive. Full day uses 360. */
    val sweepDegrees: Float,
    val wrapsMidnight: Boolean,
)

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
}
