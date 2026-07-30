package com.example.wallquote.domain.model

data class CollectionConfig(
    val id: Long = 0,
    val name: String,
    val schedule: DailyTimeRange,
    val background: BackgroundSpec,
    val texts: List<String>,
    val textStyle: TextStyleConfig,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotation: Float = 0f,
    val sortOrder: Int = 0,
)
