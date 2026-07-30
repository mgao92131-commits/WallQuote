package com.example.wallquote.ui.util

fun formatMinuteOfDay(minute: Int): String {
    val h = minute / 60
    val m = minute % 60
    return "%02d:%02d".format(h, m)
}

fun snapToHalfHour(minute: Int): Int = ((minute + 15) / 30 * 30).coerceIn(0, 1439)
