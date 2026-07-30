package com.example.wallquote.data.local

import com.example.wallquote.domain.background.BackgroundValidation
import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.TextStyleConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonConfig {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "type"
    }
}

fun BackgroundSpec.toStorage(): Pair<String, String> {
    val normalized = BackgroundValidation.normalize(this)
    val type = when (normalized) {
        is BackgroundSpec.Solid -> "solid"
        is BackgroundSpec.Gradient -> "gradient"
        is BackgroundSpec.Photo -> "photo"
    }
    return type to JsonConfig.json.encodeToString(normalized)
}

fun parseBackground(type: String, data: String): BackgroundSpec {
    val parsed = runCatching {
        JsonConfig.json.decodeFromString<BackgroundSpec>(data)
    }.getOrElse {
        return fallbackBackground(type)
    }
    val normalized = BackgroundValidation.normalize(parsed)
    return when (normalized) {
        is BackgroundSpec.Photo -> {
            if (!BackgroundValidation.isUsablePhotoAssetId(normalized.assetId)) {
                // Legacy external URIs or blank ids are unusable after Phase 3.
                BackgroundSpec.Solid("#2E3440")
            } else {
                normalized
            }
        }
        else -> normalized
    }
}

private fun fallbackBackground(type: String): BackgroundSpec =
    when (type) {
        "gradient" -> BackgroundSpec.Gradient("#000000", "#FFFFFF", angleDegrees = 0f)
        "photo" -> BackgroundSpec.Solid("#2E3440")
        else -> BackgroundSpec.Solid("#2E3440")
    }

fun TextStyleConfig.toJson(): String = JsonConfig.json.encodeToString(this)

fun parseTextStyle(data: String): TextStyleConfig =
    runCatching { JsonConfig.json.decodeFromString<TextStyleConfig>(data) }
        .getOrDefault(TextStyleConfig())
