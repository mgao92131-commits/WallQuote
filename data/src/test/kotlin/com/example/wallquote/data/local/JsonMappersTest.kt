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
            assetId = "bg_0123456789abcdef0123456789abcdef",
            dimAmount = 0.25f,
            blurRadiusDp = 8f,
        )
        val (type, json) = spec.toStorage()
        assertEquals("photo", type)
        assertTrue(json.contains("assetId"))
        assertTrue(json.contains("blurRadiusDp"))
        assertTrue(json.contains("angleDegrees").not())
        assertEquals(spec, parseBackground(type, json))
    }

    @Test
    fun invalidPhotoAssetFallsBackToSolid() {
        val bad = """{"type":"photo","assetId":"content://media/1","dimAmount":0.0,"blurRadiusDp":0.0}"""
        val parsed = parseBackground("photo", bad)
        assertTrue(parsed is BackgroundSpec.Solid)
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
