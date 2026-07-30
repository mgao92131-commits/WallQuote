package com.example.wallquote.domain.model

data class QuoteRenderInput(
    val background: BackgroundSpec,
    val lines: List<QuoteLine>,
    val previewLineIndex: Int,
    val textStyle: TextStyleConfig,
    val transform: QuoteTransform,
) {
    val previewText: String
        get() {
            if (lines.isEmpty()) return ""
            val index = previewLineIndex.coerceIn(0, lines.lastIndex)
            return lines[index].text
        }

    val previewLineId: Long?
        get() {
            if (lines.isEmpty()) return null
            val index = previewLineIndex.coerceIn(0, lines.lastIndex)
            val id = lines[index].id
            return id.takeIf { it != 0L }
        }

    companion object {
        fun fromCollection(
            config: CollectionConfig,
            previewLineIndex: Int = 0,
        ): QuoteRenderInput =
            QuoteRenderInput(
                background = config.background,
                lines = config.lines.sortedBy { it.displayOrder },
                previewLineIndex = previewLineIndex,
                textStyle = config.textStyle,
                transform = config.transform,
            )
    }
}
