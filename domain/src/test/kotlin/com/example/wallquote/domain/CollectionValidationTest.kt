package com.example.wallquote.domain

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.CollectionConfig
import com.example.wallquote.domain.model.DailyTimeRange
import com.example.wallquote.domain.model.QuoteLine
import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CollectionValidationTest {

    private fun config(lines: List<String>) = CollectionConfig(
        name = "n",
        schedule = DailyTimeRange(0, 0),
        background = BackgroundSpec.Solid("#000"),
        lines = lines.mapIndexed { i, t -> QuoteLine(0, t, i) },
        textStyle = TextStyleConfig(),
    )

    @Test
    fun rejectsBlankName() {
        assertNotNull(CollectionValidation.validateForSave(config(listOf("a")).copy(name = "  ")))
    }

    @Test
    fun rejectsAllBlankLines() {
        assertNotNull(CollectionValidation.validateForSave(config(listOf(" ", ""))))
    }

    @Test
    fun acceptsValidConfig() {
        assertNull(CollectionValidation.validateForSave(config(listOf("hello"))))
    }
}
