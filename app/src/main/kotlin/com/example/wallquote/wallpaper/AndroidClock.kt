package com.example.wallquote.wallpaper

import java.util.Calendar
import java.util.TimeZone

class AndroidClock(
    private val timeZone: TimeZone = TimeZone.getDefault(),
) : WallpaperClock {
    override fun nowMillis(): Long = System.currentTimeMillis()

    override fun minuteOfDay(): Int {
        val calendar = Calendar.getInstance(timeZone)
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }
}
