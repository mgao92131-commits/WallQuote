package com.example.wallquote.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PhotoScaleMode {
    @SerialName("center_crop")
    CenterCrop,
}
