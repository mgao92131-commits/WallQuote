package com.example.wallquote.wallpaper.background

data class BackgroundImageKey(
    val assetId: String,
    val targetWidth: Int,
    val targetHeight: Int,
    val blurRadiusBucket: Int,
)

data class BackgroundLoadToken(
    val surfaceGeneration: Long,
    val collectionId: Long,
    val assetId: String,
    val targetWidth: Int,
    val targetHeight: Int,
    val blurRadiusBucket: Int,
)

enum class BackgroundImageFailure {
    Missing,
    DecodeFailed,
    OutOfMemory,
    Cancelled,
    Unknown,
}

sealed interface BackgroundImageResult {
    data class Success(
        val key: BackgroundImageKey,
        val bitmap: android.graphics.Bitmap,
    ) : BackgroundImageResult

    data class Failed(
        val reason: BackgroundImageFailure,
    ) : BackgroundImageResult
}

interface BackgroundImageLoader {
    suspend fun load(
        assetId: String,
        targetWidth: Int,
        targetHeight: Int,
        blurRadiusDp: Float,
        density: Float,
    ): BackgroundImageResult

    fun trimMemory()
}
