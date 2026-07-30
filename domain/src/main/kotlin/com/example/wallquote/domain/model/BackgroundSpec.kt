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
        val angle: Float = 0f,
    ) : BackgroundSpec()

    @Serializable
    @SerialName("photo")
    data class Photo(
        val uri: String,
        val dimAmount: Float = 0f,
        val blurRadius: Float = 0f,
    ) : BackgroundSpec()
}
