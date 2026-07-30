package com.example.wallquote.wallpaper

import android.os.SystemClock
import java.util.Calendar
import java.util.TimeZone

class AndroidClock : WallpaperClock {
    override fun elapsedRealtimeMillis(): Long = SystemClock.elapsedRealtime()

    override fun currentWallTimeMillis(): Long = System.currentTimeMillis()

    override fun minuteOfDay(): Int {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        return calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    }

    override fun secondOfMinute(): Int {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        return calendar.get(Calendar.SECOND)
    }

    override fun millisOfSecond(): Int {
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        return calendar.get(Calendar.MILLISECOND)
    }
}
