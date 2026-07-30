package com.example.wallquote.data.local

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.TextStyleConfig
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonConfig {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}

fun BackgroundSpec.toStorage(): Pair<String, String> {
    val type = when (this) {
        is BackgroundSpec.Solid -> "solid"
        is BackgroundSpec.Gradient -> "gradient"
        is BackgroundSpec.Photo -> "photo"
    }
    return type to JsonConfig.json.encodeToString(this)
}

fun parseBackground(type: String, data: String): BackgroundSpec {
    return runCatching {
        JsonConfig.json.decodeFromString<BackgroundSpec>(data)
    }.getOrElse {
        when (type) {
            "gradient" -> BackgroundSpec.Gradient("#000000", "#FFFFFF")
            "photo" -> BackgroundSpec.Photo(uri = "")
            else -> BackgroundSpec.Solid("#2E3440")
        }
    }
}

fun TextStyleConfig.toJson(): String = JsonConfig.json.encodeToString(this)

fun parseTextStyle(data: String): TextStyleConfig =
    runCatching { JsonConfig.json.decodeFromString<TextStyleConfig>(data) }
        .getOrDefault(TextStyleConfig())
