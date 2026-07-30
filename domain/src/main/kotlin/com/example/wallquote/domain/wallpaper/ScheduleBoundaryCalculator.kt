package com.example.wallquote.domain.wallpaper

import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange

/**
 * Computes delay until the next schedule boundary with second-level precision.
 */
object ScheduleBoundaryCalculator {

    private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
    private const val MINUTE_MILLIS = 60L * 1000L

    /**
     * @param epochMillis current wall-clock instant
     * @param minuteOfDay minute-of-day in the same zone used for schedules (0..1439)
     * @param secondOfMinute seconds within the current minute (0..59)
     * @param millisOfSecond milliseconds within the current second (0..999)
     */
    fun millisUntilNextBoundary(
        collections: List<CollectionConfig>,
        minuteOfDay: Int,
        secondOfMinute: Int = 0,
        millisOfSecond: Int = 0,
    ): Long? {
        val boundaries = collections
            .asSequence()
            .map { it.schedule }
            .filterNot { it.isAllDay() }
            .flatMap { range -> boundaryMinutes(range) }
            .distinct()
            .sorted()
            .toList()

        if (boundaries.isEmpty()) return null

        val elapsedInDay =
            minuteOfDay * MINUTE_MILLIS +
                secondOfMinute.coerceIn(0, 59) * 1000L +
                millisOfSecond.coerceIn(0, 999)

        val nextSameDay = boundaries.firstOrNull { it * MINUTE_MILLIS > elapsedInDay }
        val targetElapsed = if (nextSameDay != null) {
            nextSameDay * MINUTE_MILLIS
        } else {
            DAY_MILLIS + boundaries.first() * MINUTE_MILLIS
        }
        val delay = targetElapsed - elapsedInDay
        return delay.coerceAtLeast(1L)
    }

    /** Kept for tests / coarse scheduling. */
    fun minutesUntilNextBoundary(
        collections: List<CollectionConfig>,
        minuteOfDay: Int,
    ): Int? {
        val millis = millisUntilNextBoundary(collections, minuteOfDay, 0, 0) ?: return null
        return ((millis + MINUTE_MILLIS - 1) / MINUTE_MILLIS).toInt().coerceAtLeast(1)
    }

    private fun boundaryMinutes(range: DailyTimeRange): Sequence<Int> =
        sequenceOf(range.startMinuteOfDay, range.endMinuteOfDay)
}
