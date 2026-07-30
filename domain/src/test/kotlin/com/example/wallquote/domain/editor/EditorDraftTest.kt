package com.example.wallquote.domain.editor

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.TextStyleConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorDraftTest {

    @Test
    fun detectsTextChangeWithStableLineIds() {
        val baseline = EditorDraft(
            name = "n",
            startMinute = 0,
            endMinute = 0,
            background = BackgroundSpec.Solid("#000"),
            orderedLines = listOf(EditorDraft.LineDraft(5, "a")),
            textStyle = TextStyleConfig(),
            transform = com.example.wallquote.domain.model.QuoteTransform(),
        )
        val dirty = baseline.copy(orderedLines = listOf(EditorDraft.LineDraft(5, "b")))
        assertFalse(baseline == dirty)
        val clean = baseline.copy(orderedLines = listOf(EditorDraft.LineDraft(5, "a")))
        assertTrue(baseline == clean)
    }
}
