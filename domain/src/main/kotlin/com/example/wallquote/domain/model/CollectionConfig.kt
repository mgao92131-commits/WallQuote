package com.example.wallquote.domain.model

data class CollectionConfig(
    val id: Long = 0,
    val name: String,
    val schedule: DailyTimeRange,
    val background: BackgroundSpec,
    val lines: List<QuoteLine>,
    val textStyle: TextStyleConfig,
    val transform: QuoteTransform = QuoteTransform(),
    val sortOrder: Int = 0,
)
