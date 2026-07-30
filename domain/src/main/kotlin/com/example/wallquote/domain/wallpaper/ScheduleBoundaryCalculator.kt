package com.example.wallquote.domain.wallpaper

import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange

/**
 * Computes the next schedule boundary minute relative to [minuteOfDay] (0..1439).
 * Returns minutes until that boundary (1..1440), or null if no timed schedules exist.
 */
object ScheduleBoundaryCalculator {

    fun minutesUntilNextBoundary(
        collections: List<CollectionConfig>,
        minuteOfDay: Int,
    ): Int? {
        val boundaries = collections
            .asSequence()
            .map { it.schedule }
            .filterNot { it.isAllDay() }
            .flatMap { range -> boundaryMinutes(range) }
            .distinct()
            .sorted()
            .toList()

        if (boundaries.isEmpty()) return null

        val nextSameDay = boundaries.firstOrNull { it > minuteOfDay }
        return if (nextSameDay != null) {
            nextSameDay - minuteOfDay
        } else {
            // Wrap to next day first boundary.
            (1440 - minuteOfDay) + boundaries.first()
        }.takeIf { it > 0 } ?: 1440
    }

    private fun boundaryMinutes(range: DailyTimeRange): Sequence<Int> =
        sequenceOf(range.startMinuteOfDay, range.endMinuteOfDay)
}
