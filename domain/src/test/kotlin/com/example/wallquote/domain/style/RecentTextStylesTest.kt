package com.example.wallquote.domain.style

import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class RecentTextStylesTest {

    @Test
    fun push_insertsAtFront() {
        val older = TextStyleConfig(colorHex = "#111111")
        val newer = TextStyleConfig(colorHex = "#222222")

        val result = RecentTextStyles.push(newer, listOf(older))

        assertEquals("#222222", result.first().colorHex)
        assertEquals("#111111", result[1].colorHex)
    }

    @Test
    fun push_dedupesAndMovesToFront() {
        val a = TextStyleConfig(colorHex = "#AAAAAA", textSizeSp = 20f)
        val b = TextStyleConfig(colorHex = "#BBBBBB", textSizeSp = 24f)
        val aAgain = TextStyleConfig(colorHex = "#aaaaaa", textSizeSp = 20f)

        val result = RecentTextStyles.push(aAgain, listOf(b, a))

        assertEquals(2, result.size)
        assertEquals("#AAAAAA", result.first().colorHex)
        assertEquals("#BBBBBB", result[1].colorHex)
    }

    @Test
    fun push_capsAtMaxSize() {
        val existing = (1..6).map { TextStyleConfig(colorHex = "#00000$it", textSizeSp = 12f + it) }
        val newest = TextStyleConfig(colorHex = "#FFFFFF", textSizeSp = 40f)

        val result = RecentTextStyles.push(newest, existing)

        assertEquals(RecentTextStyles.MAX_SIZE, result.size)
        assertEquals("#FFFFFF", result.first().colorHex)
        assertEquals(false, result.any { it.colorHex.equals("#000006", ignoreCase = true) })
    }
}
