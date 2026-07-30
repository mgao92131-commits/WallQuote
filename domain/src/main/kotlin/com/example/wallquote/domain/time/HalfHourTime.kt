package com.example.wallquote.domain.time

const val HALF_HOUR_SLOTS = 48

fun halfHourIndexFromMinute(minuteOfDay: Int): Int {
    val clamped = minuteOfDay.coerceIn(0, minuteFromHalfHourIndex(HALF_HOUR_SLOTS - 1))
    return (clamped / 30).coerceIn(0, HALF_HOUR_SLOTS - 1)
}

fun minuteFromHalfHourIndex(index: Int): Int =
    index.coerceIn(0, HALF_HOUR_SLOTS - 1) * 30
