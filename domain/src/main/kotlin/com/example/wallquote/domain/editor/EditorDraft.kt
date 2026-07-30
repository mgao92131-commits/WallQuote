package com.example.wallquote.domain.editor

import com.example.wallquote.domain.model.BackgroundSpec
import com.example.wallquote.domain.model.QuoteTransform
import com.example.wallquote.domain.model.TextStyleConfig

/**
 * Comparable editor snapshot for dirty-state detection (ignores ephemeral UI keys).
 */
data class EditorDraft(
    val name: String,
    val startMinute: Int,
    val endMinute: Int,
    val background: BackgroundSpec,
    val orderedLines: List<LineDraft>,
    val textStyle: TextStyleConfig,
    val transform: QuoteTransform,
) {
    data class LineDraft(
        val persistentLineId: Long,
        val text: String,
    )
}
