package com.example.wallquote.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent

object LiveWallpaperLauncher {

    sealed interface Result {
        data object LaunchedChangeLiveWallpaper : Result
        data object LaunchedChooser : Result
        data class Failed(val message: String) : Result
    }

    fun launch(context: Context): Result {
        val component = ComponentName(context, WallQuoteWallpaperService::class.java)
        return try {
            val changeIntent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(
                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    component,
                )
            }
            if (changeIntent.resolveActivity(context.packageManager) != null) {
                start(context, changeIntent)
                Result.LaunchedChangeLiveWallpaper
            } else {
                val chooser = Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER)
                if (chooser.resolveActivity(context.packageManager) != null) {
                    start(context, chooser)
                    Result.LaunchedChooser
                } else {
                    Result.Failed("当前设备不支持设置动态壁纸")
                }
            }
        } catch (error: Exception) {
            Result.Failed(error.message ?: "无法打开壁纸设置")
        }
    }

    private fun start(context: Context, intent: Intent) {
        if (context !is Activity) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
