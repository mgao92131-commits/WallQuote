package com.example.wallquote.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
sealed class BackgroundSpec {
    @Serializable
    @SerialName("solid")
    data class Solid(val colorHex: String) : BackgroundSpec()

    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @SerialName("gradient")
    data class Gradient(
        val startColorHex: String,
        val endColorHex: String,
        @JsonNames("angle")
        val angleDegrees: Float = 0f,
    ) : BackgroundSpec()

    /**
     * Photo background backed by an app-private asset id (never an external content URI).
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Serializable
    @SerialName("photo")
    data class Photo(
        @JsonNames("uri")
        val assetId: String,
        val dimAmount: Float = 0f,
        @JsonNames("blurRadius")
        val blurRadiusDp: Float = 0f,
        val scaleMode: PhotoScaleMode = PhotoScaleMode.CenterCrop,
    ) : BackgroundSpec()
}
