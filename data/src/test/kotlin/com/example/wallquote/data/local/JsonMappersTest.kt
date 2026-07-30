package com.example.wallquote.data.local

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JsonMappersTest {

    @Test
    fun backgroundRoundTrip() {
        val spec = BackgroundSpec.Gradient("#111111", "#222222", angle = 45f)
        val (type, json) = spec.toStorage()
        val parsed = parseBackground(type, json)
        assertEquals(spec, parsed)
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
