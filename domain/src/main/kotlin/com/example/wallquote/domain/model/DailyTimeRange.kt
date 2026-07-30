package com.example.wallquote.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DailyTimeRange(
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
) {
    init {
        require(startMinuteOfDay in 0..1439) { "startMinuteOfDay out of range" }
        require(endMinuteOfDay in 0..1439) { "endMinuteOfDay out of range" }
    }

    fun isAllDay(): Boolean = startMinuteOfDay == endMinuteOfDay

    fun isOvernight(): Boolean = startMinuteOfDay > endMinuteOfDay

    fun contains(minuteOfDay: Int): Boolean {
        if (isAllDay()) return true
        return if (isOvernight()) {
            minuteOfDay >= startMinuteOfDay || minuteOfDay < endMinuteOfDay
        } else {
            minuteOfDay in startMinuteOfDay until endMinuteOfDay
        }
    }
}
