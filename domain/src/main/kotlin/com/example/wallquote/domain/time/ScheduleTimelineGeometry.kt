package com.example.wallquote.domain.time

import com.example.wallquote.domain.model.DailyTimeRange

/**
 * Normalized [0f, 1f] fraction span of a 24h timeline (00:00 = 0f, 24:00 = 1f).
 */
data class TimelineSegment(
    val startFraction: Float,
    val endFraction: Float,
)

data class ScheduleTimelineResult(
    val segments: List<TimelineSegment>,
    val isAllDay: Boolean,
    val wrapsMidnight: Boolean,
    val isActiveNow: Boolean,
    val nowFraction: Float,
)

/**
 * Pure-Kotlin geometry for the Home card's 24h schedule timeline visualization.
 * Mirrors [DailyTimeRange] semantics: all-day (start == end), same-day range, and
 * overnight ranges that wrap past midnight (rendered as two segments).
 */
object ScheduleTimelineGeometry {
    private const val MINUTES_PER_DAY = 1440f

    fun compute(schedule: DailyTimeRange, minuteOfDay: Int): ScheduleTimelineResult {
        val nowFraction = minuteOfDay.coerceIn(0, 1439) / MINUTES_PER_DAY
        val isActiveNow = schedule.contains(minuteOfDay)
        if (schedule.isAllDay()) {
            return ScheduleTimelineResult(
                segments = listOf(TimelineSegment(0f, 1f)),
                isAllDay = true,
                wrapsMidnight = false,
                isActiveNow = isActiveNow,
                nowFraction = nowFraction,
            )
        }
        val startFraction = schedule.startMinuteOfDay / MINUTES_PER_DAY
        val endFraction = schedule.endMinuteOfDay / MINUTES_PER_DAY
        val segments = if (schedule.isOvernight()) {
            listOf(
                TimelineSegment(startFraction, 1f),
                TimelineSegment(0f, endFraction),
            )
        } else {
            listOf(TimelineSegment(startFraction, endFraction))
        }
        return ScheduleTimelineResult(
            segments = segments,
            isAllDay = false,
            wrapsMidnight = schedule.isOvernight(),
            isActiveNow = isActiveNow,
            nowFraction = nowFraction,
        )
    }
}
