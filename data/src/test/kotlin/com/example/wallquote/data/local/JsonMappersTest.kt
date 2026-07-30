package com.example.wallquote.data.local

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonMappersTest {

    @Test
    fun backgroundRoundTrip() {
        val spec = BackgroundSpec.Gradient("#111111", "#222222", angleDegrees = 45f)
        val (type, json) = spec.toStorage()
        val parsed = parseBackground(type, json)
        assertEquals(spec, parsed)
    }

    @Test
    fun photoAssetRoundTrip() {
        val spec = BackgroundSpec.Photo(
            assetId = "bg_abc123",
            dimAmount = 0.25f,
            blurRadiusDp = 8f,
        )
        val (type, json) = spec.toStorage()
        assertEquals("photo", type)
        assertTrue(json.contains("assetId"))
        assertEquals(spec, parseBackground(type, json))
    }

    @Test
    fun legacyPhotoUriFallsBackToSolid() {
        val legacy = """{"type":"photo","uri":"content://media/external/images/1","dimAmount":0.0,"blurRadius":0.0}"""
        val parsed = parseBackground("photo", legacy)
        assertTrue(parsed is BackgroundSpec.Solid)
    }

    @Test
    fun legacyGradientAngleAlias() {
        val legacy = """{"type":"gradient","startColorHex":"#111111","endColorHex":"#222222","angle":90.0}"""
        val parsed = parseBackground("gradient", legacy)
        assertEquals(BackgroundSpec.Gradient("#111111", "#222222", 90f), parsed)
    }

    @Test
    fun textStyleRoundTrip() {
        val style = TextStyleConfig(colorHex = "#ABCDEF", isBold = true, textSizeSp = 40f)
        val json = style.toJson()
        assertEquals(style, parseTextStyle(json))
    }

    @Test
    fun corruptJsonFallsBackToDefaults() {
        val solid = parseBackground("solid", "{not json")
        assertTrue(solid is BackgroundSpec.Solid)
        val style = parseTextStyle("{broken")
        assertEquals(TextStyleConfig(), style)
    }
}
