package com.example.wallquote.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class BackgroundSpec {
    @Serializable
    @SerialName("solid")
    data class Solid(val colorHex: String) : BackgroundSpec()

    @Serializable
    @SerialName("gradient")
    data class Gradient(
        val startColorHex: String,
        val endColorHex: String,
        val angleDegrees: Float = 0f,
    ) : BackgroundSpec()

    /**
     * Photo background backed by an app-private asset id (never an external content URI).
     */
    @Serializable
    @SerialName("photo")
    data class Photo(
        val assetId: String,
        val dimAmount: Float = 0f,
        val blurRadiusDp: Float = 0f,
        val scaleMode: PhotoScaleMode = PhotoScaleMode.CenterCrop,
    ) : BackgroundSpec()
}
